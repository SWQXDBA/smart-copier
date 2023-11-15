package org.swqxdba

import cn.hutool.core.bean.BeanUtil
import com.sun.org.slf4j.internal.LoggerFactory
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import java.lang.RuntimeException
import java.lang.reflect.Method

object SmartCopier {

    val logger = LoggerFactory.getLogger(this.javaClass)

    @Throws(Exception::class)
    fun getCopier(source: Class<*>, target: Class<*>, config: CopyConfig? = null): Copier {

        var generateResult = getCtClass(source, target, config)
        generateResult.ctClass.writeFile()
        val generatedClass = generateResult.ctClass.toClass()

        val instance = generatedClass.newInstance() as Copier

        //创建实例后,给各个所需字段赋值
        for (fieldInfo in generateResult.methodResult.fields) {
            val field = generatedClass.getDeclaredField(fieldInfo.fieldName)
            field.isAccessible = true
            field.set(instance, fieldInfo.fieldValue)
        }
        return instance
    }

    class GenerateResult(val ctClass: CtClass, val methodResult: CopyMethodResult)

    fun getCtClass(source: Class<*>, target: Class<*>, config: CopyConfig? = null): GenerateResult {
        val classPool = ClassPool.getDefault()
        val ctClass =
            classPool.makeClass("test.BeanCopier\$${if (config == null) "Default" else System.identityHashCode(config)}")
        ctClass.addInterface(classPool.makeInterface(Copier::class.qualifiedName))

        run {
            val sourceCtClass = classPool[source.getName()]
            val sourceCtField = CtField(sourceCtClass, "sourceClass", ctClass)
            ctClass.addField(sourceCtField)
        }
        run {
            val targetCtClass = classPool[target.getName()]
            val targetCtField = CtField(targetCtClass, "targetClass", ctClass)
            ctClass.addField(targetCtField)
        }


        val copyMethodResult = getCopyMethod(source, target, config)

        for (field in copyMethodResult.fields) {
            val fieldClass = classPool[field.fieldClass.getName()]
            val targetCtField = CtField(fieldClass, field.fieldName, ctClass)
            ctClass.addField(targetCtField)
        }

        val method = CtMethod.make(copyMethodResult.methodString, ctClass)
        ctClass.addMethod(method)

        //桥接方法
        val overrideMethod = CtMethod.make(
            """
          public void copy( Object src, Object target ){
             copyInternal((${source.name})src,(${target.name})target);
         }
        """.trimIndent(), ctClass
        )

        ctClass.addMethod(overrideMethod)
        return GenerateResult(ctClass, copyMethodResult)
    }

    class CopyMethodResult(
        var methodString: String? = null,
        var fields: MutableList<FieldInfo> = mutableListOf()
    )

    class FieldInfo(val fieldName: String, val fieldClass: Class<*>, val fieldValue: Any?)

    private fun getCopyMethod(sourceClass: Class<*>, targetClass: Class<*>, config: CopyConfig?): CopyMethodResult {
        val result = CopyMethodResult()
        val sourceName = sourceClass.getName()
        val targetName = targetClass.getName()
        val targetProperties = BeanUtil.getPropertyDescriptors(targetClass)


        var fieldCounter = 0
        val nextFieldName: () -> String = {
            fieldCounter++
            "generatedField$fieldCounter"
        }

        //添加属性转换器字段
        val propertyValueConverterFieldName = nextFieldName()
        result.fields.add(
            FieldInfo(
                propertyValueConverterFieldName,
                PropertyValueConverter::class.java,
                config?.propertyValueConverter
            )
        )

        val bodyStringBuilder = StringBuilder();
        var mapper = mutableMapOf<Method, Method>()
        //获取bean属性
        for (targetProperty in targetProperties) {
            val writeMethod = targetProperty.getWriteMethod() ?: continue
            val sourceProperty = BeanUtil.getPropertyDescriptor(sourceClass, targetProperty.name) ?: continue
            val readMethod = sourceProperty.getReadMethod() ?: continue
            mapper[readMethod] = writeMethod
        }
        //根据设置调整字段映射
        config?.propertyMapperRuleCustomizer?.let {
            mapper = it.mapperRule(sourceClass, targetClass, mapper).toMutableMap()
        }

        //校验映射关系 validate mapper
        for (mutableEntry in mapper) {
            val getter = mutableEntry.key
            if (getter.declaringClass != sourceClass) {
                throw RuntimeException("the method of ${getter.name} not belong Class ${sourceClass.name}")
            }
            val setter = mutableEntry.value
            if (setter.declaringClass != targetClass) {
                throw RuntimeException("the method of ${setter.name} not belong Class ${targetClass.name}")
            }
        }

        for ((readMethod, writeMethod) in mapper) {
            //开启独立的代码块 避免变量名冲突
            bodyStringBuilder.append("{")
            //setter中的参数类型
            val setterParamType = writeMethod.parameters[0].type

            //定义临时变量
            bodyStringBuilder.append(
                """
                   ${setterParamType.name} tempValue = src.${readMethod.name}();
                """.trimIndent()
            )

            //应用值转换器
            if (config?.propertyValueConverter?.shouldIntercept(
                    readMethod,
                    writeMethod,
                    sourceClass,
                    targetClass
                ) == true
            ) {
                bodyStringBuilder.append(
                    """
                  tempValue =(${setterParamType.name}) this.$propertyValueConverterFieldName.convert(tempValue);
              """
                )
            }

            val provideValue: Any? =
                config?.defaultValueProvider?.provide(readMethod, writeMethod, sourceClass, targetClass)

            if (provideValue != null && !setterParamType.isPrimitive) {
                //校验给的默认值是否能赋值给setter或者变量
                if (!setterParamType.isAssignableFrom(provideValue.javaClass)) {
                    throw java.lang.RuntimeException(
                        "Can not set type: ${provideValue.javaClass.name} to type: ${setterParamType.name}" +
                                "(it is the setter method's argument type), \n" +
                                "This exception means you can't invoke '${targetClass.name}.${writeMethod.name}(${provideValue.javaClass.name})'.\n" +
                                "Please check the return value of CopyConfig.defaultValueProvider.\n" +
                                "The setter method Name is '${writeMethod.name}',the Class is '${targetClass.name}'.\n" +
                                "\n"
                    )
                }
                //设置默认值
                val fieldName = nextFieldName()
                result.fields.add(FieldInfo(fieldName, setterParamType, provideValue))
                bodyStringBuilder.append(
                    """
                        
                    if(tempValue == null){
                       tempValue =  $fieldName;
                    }
                    
                """.trimIndent()
                )
            }
            //如果允许用null替代 或者是primitive类型 则直接执行
            if (config?.replaceWithNull == true || setterParamType.isPrimitive) {
                bodyStringBuilder.append("target.${writeMethod.name}(tempValue);")
            } else {
                bodyStringBuilder.append(
                    """
                    if(tempValue!=null){
                     target.${writeMethod.name}(tempValue);
                    }
                   
                """.trimIndent()
                )
            }


            bodyStringBuilder.append("}")
        }

        val methodString = """
            public void copyInternal( $sourceName src, $targetName target ){
                $bodyStringBuilder
            }
        """.trimIndent()
        result.methodString = methodString
        println(methodString)
        return result
    }
}