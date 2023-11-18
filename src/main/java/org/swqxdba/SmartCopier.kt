package org.swqxdba

import cn.hutool.core.bean.BeanUtil
import com.sun.org.slf4j.internal.LoggerFactory
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap



class SmartCopier {

    companion object {

        data class CopierMeta(val source: Class<*>, val target: Class<*>, val config: CopyConfig? = null)

        class GenerateResult(val ctClass: CtClass, val genContext: GenContext)

        class GenContext {

            var fieldCounter: Int = 0

            var fields: MutableList<FieldInfo> = mutableListOf()

            //添加属性转换器字段名
            var propertyValueConverterFieldName: String? = null

            val methodStrings = mutableMapOf<CopierMethod, String>()
            fun nextFieldName(): String {
                fieldCounter++
                return "generatedField$fieldCounter"
            }

            fun addField(fieldInto: FieldInfo) {
                fields.add(fieldInto)
            }

            fun setGeneratedMethodBody(method: CopierMethod, body: String) {
                methodStrings[method] = body
            }

        }

        class FieldInfo(val fieldName: String, val fieldClass: Class<*>, val fieldValue: Any?)

        enum class CopierMethod {
            copy, copyNonNullProperties, merge
        }

        val logger = LoggerFactory.getLogger(SmartCopier::class.java)

        var staticConfig: CopyConfig? = null

        val copierCache: ConcurrentHashMap<CopierMeta, Pair<Copier, GenerateResult>> = ConcurrentHashMap()

        var debug = false

        /**
         * 设置debug模式下 生成的class文件存放在哪个文件夹下
         */
        @JvmStatic
        var debugGeneratedClassFileDir: String? = null

        @JvmStatic
        fun setGlobalConfig(config: CopyConfig) {
            staticConfig = config;
        }

        /**
         * 设置是否为debug模式，debug模式下 生成的方法源码会在GenerateResult.GenContext.methodStrings中
         */
        @JvmStatic
        fun setDebugMode(debugMode: Boolean) {
            debug = debugMode;
        }


        @JvmStatic
        @JvmOverloads
        fun copy(source: Any, target: Any, config: CopyConfig? = null) {
            getCopier(source.javaClass, target.javaClass, config ?: staticConfig).copy(source, target)
        }

        @JvmStatic
        @JvmOverloads
        fun copyNonNullProperties(source: Any, target: Any, config: CopyConfig? = null) {
            getCopier(source.javaClass, target.javaClass, config ?: staticConfig).copyNonNullProperties(source, target)
        }

        @JvmStatic
        @JvmOverloads
        fun merge(source: Any, target: Any, config: CopyConfig? = null) {
            getCopier(source.javaClass, target.javaClass, config ?: staticConfig).merge(source, target)
        }

        @JvmStatic
        @JvmOverloads
        fun getCopier(source: Class<*>, target: Class<*>, config: CopyConfig? = null): Copier {
            val key = CopierMeta(source, target, config)
            return copierCache.computeIfAbsent(key) {
                val ctResult = getCtClass(source, target, config)
                Pair(getCopierInternal(ctResult), ctResult)
            }.first
        }

        /**
         * 用于调试或者测试使用，获取生成的结果。
         */
        @JvmStatic
        @JvmOverloads
        fun getGenerateResult(source: Class<*>, target: Class<*>, config: CopyConfig? = null): GenerateResult {
            val key = CopierMeta(source, target, config)
            return copierCache.computeIfAbsent(key) {
                val ctResult = getCtClass(source, target, config)
                Pair(getCopierInternal(ctResult), ctResult)
            }.second
        }

        private fun getCopierInternal(generateResult: GenerateResult): Copier {

            if (debug) {
                generateResult.ctClass.writeFile(debugGeneratedClassFileDir)
            }


            val generatedClass = generateResult.ctClass.toClass()

            val instance = generatedClass.newInstance() as Copier

            //创建实例后,给各个所需字段赋值
            for (fieldInfo in generateResult.genContext.fields) {
                val field = generatedClass.getDeclaredField(fieldInfo.fieldName)
                field.isAccessible = true
                field.set(instance, fieldInfo.fieldValue)
            }
            return instance
        }

        @JvmStatic
        private fun getCtClass(source: Class<*>, target: Class<*>, config: CopyConfig? = null): GenerateResult {
            val classPool = ClassPool.getDefault()
            val ctClass =
                classPool.makeClass(
                    "org.swqxdba.smartcopier.generated.BeanCopier${System.identityHashCode(source)}${
                        System.identityHashCode(
                            target
                        )
                    }\$${
                        if (config == null) "Default" else System.identityHashCode(
                            config
                        )
                    }"
                )
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

            val context = GenContext()


            val copyMethodResult = getCopyMethod(source, target, context, CopierMethod.copy, config)


            val copyNonNullPropertiesResult =
                getCopyMethod(source, target, context, CopierMethod.copyNonNullProperties, config)

            val mergeResult = getCopyMethod(source, target, context, CopierMethod.merge, config)


            for (field in context.fields) {
                val fieldClass = classPool[field.fieldClass.getName()]
                val targetCtField = CtField(fieldClass, field.fieldName, ctClass)
                ctClass.addField(targetCtField)
            }

            run {
                val method = CtMethod.make(copyMethodResult, ctClass)
                ctClass.addMethod(method)
            }

            run {
                val method = CtMethod.make(copyNonNullPropertiesResult, ctClass)
                ctClass.addMethod(method)
            }

            run {
                val method = CtMethod.make(mergeResult, ctClass)
                ctClass.addMethod(method)
            }

            return GenerateResult(ctClass, context)
        }


        private fun getCopyMethod(
            sourceClass: Class<*>,
            targetClass: Class<*>,
            context: GenContext,
            method: CopierMethod,
            config: CopyConfig?
        ): String {

            val sourceName = getParamName(sourceClass)
            val targetName = getParamName(targetClass)
            val targetProperties = BeanUtil.getPropertyDescriptors(targetClass)


            val bodyStringBuilder = StringBuilder();
            var mapper = mutableMapOf<Method, Method>()

            //目标属性的getter方法 用于生成merge方法时判断字段现有的值是否为null
            //Map的键值为<target.setter,target.getter>
            val targetGetterMethodMap = mutableMapOf<Method, Method>()
            //获取bean属性
            for (targetProperty in targetProperties) {
                val writeMethod = targetProperty.getWriteMethod() ?: continue

                val sourceProperty = SmartUtil.getPropertyDescriptor(sourceClass, targetProperty.name) ?: continue
                val readMethod = sourceProperty.getReadMethod() ?: continue
                mapper[readMethod] = writeMethod

                val targetGetterMethod = targetProperty.getReadMethod() ?: continue
                targetGetterMethodMap[writeMethod] = targetGetterMethod
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


            //遍历属性 生成代码
            for ((readMethod, writeMethod) in mapper) {
                //开启独立的代码块 避免变量名冲突
                bodyStringBuilder.append("{")
                //setter中的参数类型
                val setterParamType = writeMethod.parameters[0].type

                //用于标记后面需不需要闭合的}符号 因为如果是merge模式 可能需要生成的代码是:
                // if(target.getXXX()==null){
                //  //xxx其他代码
                //
                // }<-这里需要补一个括号
                var needCloseStr = false

                if (method == CopierMethod.merge) {
                    val targetGetterMethod = targetGetterMethodMap[writeMethod]
                    //getter不存在 无法检测目标属性的现有值 则不做检测
                    if (targetGetterMethod == null) {
                        logger.warn(
                            "can not find target getter method to generate merge method, " +
                                    "the setter method is :${writeMethod.name}"
                        )
                    } else if (!targetGetterMethod.returnType.isPrimitive) {//returnType为原始类型，就不赋值了
                        bodyStringBuilder.append(
                            """
                    if(target.${targetGetterMethod.name}()==null){
                """.trimIndent()
                        )
                        needCloseStr = true
                    }
                }


                //定义临时变量 用来存储源属性值
                bodyStringBuilder.append(
                    """
                   ${getParamName(setterParamType)} tempValue = src.${readMethod.name}();
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

                    //如果还没有转换器属性，则给copier实例添加这个属性
                    var propertyValueConverterFieldName = context.propertyValueConverterFieldName
                    if (propertyValueConverterFieldName == null) {
                        propertyValueConverterFieldName = context.nextFieldName()
                        context.propertyValueConverterFieldName = propertyValueConverterFieldName
                        context.addField(
                            FieldInfo(
                                propertyValueConverterFieldName,
                                PropertyValueConverter::class.java,
                                config.propertyValueConverter
                            )
                        )
                    }


                    bodyStringBuilder.append(
                        """
                  tempValue =(${getParamName(setterParamType)}) this.$propertyValueConverterFieldName.convert(tempValue);
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
                    //如果为null 则设置默认值
                    val fieldName = context.nextFieldName()

                    context.addField(FieldInfo(fieldName, setterParamType, provideValue))
                    bodyStringBuilder.append(
                        """
                    if(tempValue == null){
                       tempValue =  $fieldName;
                    }
                    
                """.trimIndent()
                    )
                }


                ///接下来根据tempValue给目标属性赋值///

                //如果允许用null替代 或者是primitive类型 则直接执行
                if (method == CopierMethod.copy || setterParamType.isPrimitive) {
                    bodyStringBuilder.append("target.${writeMethod.name}(tempValue);")
                } else if (method == CopierMethod.copyNonNullProperties) {
                    bodyStringBuilder.append(
                        """
                    if(tempValue!=null){
                     target.${writeMethod.name}(tempValue);
                    }
                   
                """.trimIndent()
                    )
                } else {

                    bodyStringBuilder.append(
                        """
                     target.${writeMethod.name}(tempValue);
                """.trimIndent()
                    );

                }

                if (needCloseStr) {
                    bodyStringBuilder.append("}")
                }

                bodyStringBuilder.append("}")
            }

            val methodString = """
            public void ${method}( java.lang.Object srcBean, java.lang.Object targetBean ){
                $sourceName src = ($sourceName)srcBean;
                $targetName target = ($targetName) targetBean;
                $bodyStringBuilder
            }
        """.trimIndent()
            if (debug) {
                context.setGeneratedMethodBody(method, methodString)
            }
            return methodString
        }

        private fun getParamName(clazz: Class<*>):String{
            var name = clazz.name
            if(!clazz.isArray){
                return name
            }
            name = name.replace(";","")
            var arrayDeep = 0
            for (c in name.toCharArray()) {
                if(c=='['){
                    arrayDeep++
                }
            }

            name = when(name.last()){
                'Z'->"boolean"
                'B'->"byte"
                'C'->"char"
                'D'->"double"
                'F'->"float"
                'I'->"int"
                'J'->"long"
                'S'->"short"
                else ->name.substring(name.indexOf("[L")+2) // class/interface
            }

            name+="[]".repeat(arrayDeep)

            return name
        }
    }


}