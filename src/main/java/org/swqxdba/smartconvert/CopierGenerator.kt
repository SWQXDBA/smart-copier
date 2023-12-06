package org.swqxdba.smartconvert


import cn.hutool.core.bean.BeanUtil
import net.sf.cglib.core.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.swqxdba.SmartUtil
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Paths


class CopierGenerator(val sourceClass: Class<*>, val targetClass: Class<*>, val config: CopyConfig? = null) {

    val generateContext = GenerateContext()

    companion object {
        val defineClassMethod = ClassLoader::class.java.getDeclaredMethod(
            "defineClass",
            String::class.java,
            ByteArray::class.java,
            Int::class.java,
            Int::class.java
        )
    }

    private fun getClassLoader(): ClassLoader {
        return javaClass.getClassLoader() ?: Thread.currentThread().getContextClassLoader()
        ?: throw RuntimeException("classLoader not found")
    }

    private fun defineClass(name: String, bytes: ByteArray): Class<*> {
        val classLoader = getClassLoader()
        defineClassMethod.isAccessible = true
        return defineClassMethod(classLoader, name, bytes, 0, bytes.size) as Class<*>
    }

    val methodMapper: MutableMap<Method, Method>


    val COPY_DESCRIPTOR =
        Signature("copy", Type.VOID_TYPE, arrayOf(Constants.TYPE_OBJECT, Constants.TYPE_OBJECT))

    val COPY_NONNULL_DESCRIPTOR =
        Signature("copyNonNullProperties", Type.VOID_TYPE, arrayOf(Constants.TYPE_OBJECT, Constants.TYPE_OBJECT))
    val MERGE_DESCRIPTOR = Signature("merge", Type.VOID_TYPE, arrayOf(Constants.TYPE_OBJECT, Constants.TYPE_OBJECT))

    //调整属性映射
    init {
        val targetProperties = BeanUtil.getPropertyDescriptors(targetClass)

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
        methodMapper = mapper
    }

    fun generateClassName(sourceClass: Class<*>, targetClass: Class<*>, copyConfig: CopyConfig?): String {

        val sourceClassName = sourceClass.simpleName
        val targetClassName = targetClass.simpleName
        val className =
            "SmartCopierImpl_${sourceClassName}_to_${targetClassName}_${copyConfig?.let { copyConfig.hashCode() } ?: "default"}"
        return className
    }

    fun generateClass(): Class<*>? {
        val cv = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        val generateClassName = generateClassName(sourceClass, targetClass, config)

        val ce = ClassEmitter(cv)
        ce.begin_class(
            Constants.V1_8,
            Constants.ACC_PUBLIC,
            generateClassName,
            null,
            arrayOf(TypeUtils.getType("org.swqxdba.smartconvert.Copier")),
            Constants.SOURCE_FILE
        )

        EmitUtils.null_constructor(ce)

        val propertyValueConverter = config?.propertyValueConverter

        if (propertyValueConverter != null) {
            generateContext.setValueConverter(propertyValueConverter)
        }

        generateMethod(ce, COPY_DESCRIPTOR, CopyMethodType.COPY)
        generateMethod(ce, COPY_NONNULL_DESCRIPTOR, CopyMethodType.COPY_NONNULL)
        generateMethod(ce, MERGE_DESCRIPTOR, CopyMethodType.MERGE)

        ce.end_class()

        val toByteArray = cv.toByteArray()
        Files.write(Paths.get(generateClassName + ".class"), toByteArray)
        return defineClass(generateClassName, toByteArray)
    }

    fun generateMethod(ce: ClassEmitter, signature: Signature, copyMethodType: CopyMethodType) {


        val sourceType: Type = Type.getType(sourceClass)
        val targetType: Type = Type.getType(targetClass)


        val codeEmitter = ce.begin_method(Opcodes.ACC_PUBLIC, signature, null)


        //从局部变量表中读取source和target变量,把传入的两个object参数转换为对应的类型,压入操作数栈
        //因为要先getter再setter，所以先转换target，再转换source 保证source在栈顶

        //转换source 和 target的类型, 存入局部变量表
        codeEmitter.load_arg(1)
        codeEmitter.checkcast(targetType)
        val targetLocal = codeEmitter.make_local(targetType)
        codeEmitter.store_local(targetLocal)

        codeEmitter.load_arg(0)
        codeEmitter.checkcast(sourceType)
        val sourceLocal = codeEmitter.make_local(sourceType)
        codeEmitter.store_local(sourceLocal)

        //此时操作数栈是空的

        for ((reader, writer) in methodMapper) {
            val targetProperties = BeanUtil.getPropertyDescriptors(targetClass)
            val targetProperty = targetProperties.find { it.writeMethod == writer } ?: continue
            val targetGetterMethod = targetProperty.readMethod
            val targetGetterMethodInfo = ReflectUtils.getMethodInfo(targetGetterMethod)

            val readMethodInfo = ReflectUtils.getMethodInfo(reader)
            val writeMethodInfo = ReflectUtils.getMethodInfo(writer)
            val getterReturnType: Type = readMethodInfo.signature.returnType
            val setterArgType: Type = writeMethodInfo.signature.argumentTypes[0]

            //处理默认值 计算出默认值后生成一个字段，等到这个Copier生成后调用一个setter方法把默认值放到字段中。
            val defaultFieldName = "default_value_of_" + targetProperty.name
            val setDefaultFieldMethodName = "set_default_value_for_$defaultFieldName"

            val defaultValueProvider = config?.defaultValueProvider
            var defaultValue: Any? = null
            defaultValueProvider?.let { provider ->
                defaultValue = provider.provide(reader, writer, sourceClass, targetClass, copyMethodType)
                if (defaultValue != null) {
                    ce.declare_field(Opcodes.ACC_PRIVATE, defaultFieldName, setterArgType, null)
                    generateContext.addField(
                        defaultFieldName,
                        setDefaultFieldMethodName,
                        defaultValue
                    )
                }

            }

            //处理转换器converter
            var useConverter = false
            if (generateContext.convertField != null) {
                if (config!!.propertyValueConverter!!.shouldIntercept(
                        reader,
                        writer,
                        sourceClass,
                        targetClass,
                        copyMethodType
                    )
                ) {
                    useConverter = true
                }
            }


            val doSwap = {
                //复制source 和 target 然后调用方法
                codeEmitter.load_local(targetLocal)//target
                codeEmitter.load_local(sourceLocal)//source
                codeEmitter.invoke(readMethodInfo)
                codeEmitter.invoke(writeMethodInfo)
            }
            //不消耗栈元素 读取source的属性值
            val invokeReader = {
                codeEmitter.load_local(sourceLocal)
                codeEmitter.invoke(readMethodInfo)
            }
            //要求栈为[target,currentValue(栈顶)]
            val invokeWriter = {
                codeEmitter.invoke(writeMethodInfo)
            }
            //不消耗栈元素 生成一个转换后的值
            val invokeReadAndConvert = {
                codeEmitter.load_this()
                codeEmitter.getfield(generateContext.convertField!!.name)
                invokeReader()
                codeEmitter.invoke_interface(
                    Type.getType(PropertyValueConverter::class.java),
                    Signature("convert", Type.getType(Any::class.java), arrayOf(Type.getType(Any::class.java)))
                )
                //记得转换类型 因为convert返回的是Any
                codeEmitter.checkcast(setterArgType)
            }

            //是否使用默认值 如果不需要默认值提供者，或者默认值为null则直接赋值 如果是primitive则永远不用默认值 因为不会是null

            val needUseDefaultValue = {
                //defaultValue不为null说明defaultValueProvider也不为null
                defaultValue != null && !TypeUtils.isPrimitive(setterArgType)
            }

            //如果栈顶是null 则调用默认值提供者
            //要求调用时 栈为[target,currentValue(栈顶)]
            //调用结束后 栈顶可能为默认值 或者 currentValue
            val tryGetDefaultValue = {
                //复制一份目标元素 用于判断是否为null 后面调用setter还要用一次
                codeEmitter.dupForType(setterArgType)
                val skipLabel = codeEmitter.make_label()
                codeEmitter.ifnonnull(skipLabel)//不等于null 则跳到后面直接执行setter
                ///使用默认值///
                //扔掉原有的值(null)
                codeEmitter.popForType(setterArgType)

                //此时栈中只有一个target

                //读取默认值
                codeEmitter.load_this()
                codeEmitter.getfield(defaultFieldName)

                codeEmitter.visitLabel(skipLabel)

            }



            if (signature == COPY_DESCRIPTOR) {
                if (defaultValueProvider == null && !useConverter) {
                    doSwap()
                } else {
                    //先读入target 用于最后执行setter
                    codeEmitter.load_local(targetLocal)

                    //可能调用converter进行转换 结束后栈顶是目标值
                    if (useConverter) {
                        invokeReadAndConvert()
                    } else {
                        invokeReader()
                    }
                    //此时栈顶就是目标元素了
                    if (needUseDefaultValue()) {
                        tryGetDefaultValue()
                        //执行setter
                        codeEmitter.invoke(writeMethodInfo)
                    } else {
                        invokeWriter()
                    }
                }

            } else {

                if (signature == COPY_NONNULL_DESCRIPTOR) {
                    if (TypeUtils.isPrimitive(getterReturnType)) {
                        //primitive时不考虑default value
                        if (!useConverter) {
                            doSwap()
                        } else {
                            codeEmitter.load_local(targetLocal)//先压入栈 用于调用setter
                            invokeReadAndConvert()
                            //由于是primitive 所以不用考虑null convert应当返回非null值
                            codeEmitter.invoke(writeMethodInfo)
                        }
                    } else {
                        codeEmitter.load_local(targetLocal)//先压入栈 用于调用setter

                        if (useConverter) {
                            invokeReadAndConvert()
                        } else {
                            invokeReader()
                        }
                        if(needUseDefaultValue()){
                            tryGetDefaultValue()
                        }
                        //复制栈顶元素用于执行if指令
                        codeEmitter.dupForType(setterArgType)
                        val skip = codeEmitter.make_label()
                        val end = codeEmitter.make_label()

                        //如果为null则清理栈 否则执行setter 然后跳转到end
                        codeEmitter.ifnull(skip)
                        codeEmitter.invoke(writeMethodInfo)
                        codeEmitter.visitJumpInsn(Opcodes.GOTO, end)
                        codeEmitter.visitLabel(skip)
                        //此时不执行setter 要把栈中的target和currentValue扔掉
                        codeEmitter.popForType(setterArgType)//扔掉currentValue
                        codeEmitter.pop()//扔掉target
                        codeEmitter.visitLabel(end)
                    }

                } else if (signature == MERGE_DESCRIPTOR) {
                    if (TypeUtils.isPrimitive(setterArgType)) {
                        continue
                    } else {
                        codeEmitter.load_local(targetLocal)//先压入栈 用于调用setter

                        if (useConverter) {
                            invokeReadAndConvert()
                        } else {
                            invokeReader()
                        }
                        if (needUseDefaultValue()) {
                            tryGetDefaultValue()
                        }



                        ///此时栈元素为[target,currentValue]///

                        //读取target中的属性值 这个值执行完if后就没用了 不需要dup
                        codeEmitter.load_local(targetLocal)
                        codeEmitter.invoke(targetGetterMethodInfo)
                        val skip = codeEmitter.make_label()
                        val end = codeEmitter.make_label()

                        //如果为null则执行setter 否则清理栈 然后跳转到end
                        codeEmitter.ifnonnull(skip)
                        ///此时栈元素为[target,currentValue]///
                        invokeWriter()
                        codeEmitter.visitJumpInsn(Opcodes.GOTO, end)
                        codeEmitter.visitLabel(skip)
                        codeEmitter.popForType(setterArgType)//扔掉currentValue
                        codeEmitter.pop()//扔掉target
                        codeEmitter.visitLabel(end)

                    }
                }
            }

        }

        codeEmitter.return_value()
        codeEmitter.end_method()
    }

}

