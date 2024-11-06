package io.github.swqxdba.smartcopier

import io.github.swqxdba.smartcopier.InternalUtil.javaObjectClass
import io.github.swqxdba.smartcopier.InternalUtil.smartCast
import io.github.swqxdba.smartcopier.converter.PropertyValueConverter
import net.sf.cglib.core.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import io.github.swqxdba.smartcopier.propertyreader.PropertyValueReader
import java.io.File
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Paths


internal class CopierGenerator(
    val sourceClass: Class<*>,
    val targetClass: Class<*>,
    config0: CopyConfig? = null,
    val smartCopier: SmartCopier
) {

    var config: CopyConfig? = config0

    val generateContext = FieldContext()


    open class MyClassLoader : ClassLoader() {
        fun define(name: String, bytes: ByteArray): Class<*> {
            return defineClass(name, bytes, 0, bytes.size)
        }

        companion object : MyClassLoader()
    }

    private fun defineClass(name: String, bytes: ByteArray): Class<*> {
        val myClassLoader: MyClassLoader = MyClassLoader.Companion
        return myClassLoader.define(name, bytes)
    }

    val methodMapper: MutableMap<Method, Method>


    val COPY_DESCRIPTOR =
        Signature("copy", Type.VOID_TYPE, arrayOf(Constants.TYPE_OBJECT, Constants.TYPE_OBJECT))

    val COPY_NONNULL_DESCRIPTOR =
        Signature("copyNonNullProperties", Type.VOID_TYPE, arrayOf(Constants.TYPE_OBJECT, Constants.TYPE_OBJECT))
    val MERGE_DESCRIPTOR = Signature("merge", Type.VOID_TYPE, arrayOf(Constants.TYPE_OBJECT, Constants.TYPE_OBJECT))

    //调整属性映射
    init {
        if (config == null) {
            config = smartCopier.defaultConfig
        }
        val targetProperties = InternalUtil.getPropertyDescriptors(targetClass)

        var mapper = mutableMapOf<Method, Method>()

        //目标属性的getter方法 用于生成merge方法时判断字段现有的值是否为null
        //Map的键值为<target.setter,target.getter>
        val targetGetterMethodMap = mutableMapOf<Method, Method>()
        //获取bean属性
        for (targetProperty in targetProperties) {
            val writeMethod = targetProperty.writeMethod ?: continue

            val sourceProperty = InternalUtil.getPropertyDescriptor(sourceClass, targetProperty.name) ?: continue
            val readMethod = sourceProperty.readMethod ?: continue
            mapper[readMethod] = writeMethod

            val targetGetterMethod = targetProperty.readMethod ?: continue
            targetGetterMethodMap[writeMethod] = targetGetterMethod
        }
        //根据设置调整字段映射
        config?.propertyMapperRuleCustomizer?.let {
            mapper = it.mapperRule(sourceClass, targetClass, mapper).toMutableMap()
        }

        //校验映射关系 validate mapper
        for (mutableEntry in mapper) {
            val getter = mutableEntry.key
            if (!getter.declaringClass.isAssignableFrom(sourceClass)) {
                throw RuntimeException("the method of ${getter.name} not assignableFrom Class ${sourceClass.name}")
            }
            val setter = mutableEntry.value
            if (!setter.declaringClass.isAssignableFrom(targetClass)) {
                throw RuntimeException("the method of ${setter.name} not assignableFrom Class ${targetClass.name}")
            }
        }
        methodMapper = mapper
    }

    fun generateClassName(sourceClass: Class<*>, targetClass: Class<*>, copyConfig: CopyConfig?): String {

        val sourceClassName = sourceClass.name
        val targetClassName = targetClass.name
        val className =
            ("SmartCopierImpl_${sourceClassName}_to_${targetClassName}_${
                copyConfig?.let {
                    System.identityHashCode(
                        copyConfig
                    )
                } ?: "default"
            }_" +
                    "${System.identityHashCode(sourceClass)}_${System.identityHashCode(targetClass)}_${System.identityHashCode(smartCopier)}").replace(
                ".",
                "_"
            )
        return className
    }


    fun generateCopier(): Copier {
        val generateClass = generateClass()
        val copier = generateClass.newInstance() as Copier
        //给字段设置值
        for (fieldWrapper in generateContext.fields) {
            fieldWrapper.value?.let { value ->
                val field = generateClass.getDeclaredField(fieldWrapper.name)
                field.isAccessible = true
                field[copier] = value
            }
        }
        return copier
    }

    private fun generateClass(): Class<*> {
        val cv = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        val generateClassName = generateClassName(sourceClass, targetClass, config)

        val ce = ClassEmitter(cv)
        ce.begin_class(
            Constants.V1_8,
            Constants.ACC_PUBLIC,
            generateClassName,
            null,
            arrayOf(Type.getType(Copier::class.java)),
            Constants.SOURCE_FILE
        )


        EmitUtils.null_constructor(ce)


        generateMethod(ce, COPY_DESCRIPTOR, CopyMethodType.COPY)
        generateMethod(ce, COPY_NONNULL_DESCRIPTOR, CopyMethodType.COPY_NONNULL)
        generateMethod(ce, MERGE_DESCRIPTOR, CopyMethodType.MERGE)

        ce.end_class()

        val toByteArray = cv.toByteArray()
        if (smartCopier.debugMode) {
            smartCopier.debugOutPutDir?.let { dir ->
                File(dir).mkdirs()
                Files.write(Paths.get(dir, generateClassName + ".class"), toByteArray)
            }
            smartCopier.debugOutputStream?.write(toByteArray)
        }
        return defineClass(generateClassName, toByteArray)
    }

    private fun generateMethod(ce: ClassEmitter, signature: Signature, copyMethodType: CopyMethodType) {


        val sourceType: Type = Type.getType(sourceClass)
        val targetType: Type = Type.getType(targetClass)


        val codeEmitter = ce.begin_method(Opcodes.ACC_PUBLIC, signature, null)

        val methodEndLabel = codeEmitter.make_label()

        //这个判断其实可以不用 因为copy的应该是java bean而不是基础类型 只是为了防止错误
        if (!TypeUtils.isPrimitive(sourceType)) {
            codeEmitter.load_arg(1)
            codeEmitter.ifnull(methodEndLabel)

        }
        if (!TypeUtils.isPrimitive(sourceType)) {
            codeEmitter.load_arg(0)
            codeEmitter.ifnull(methodEndLabel)
        }
        val localVariableStartLabel = codeEmitter.make_label()
        codeEmitter.visitLabel(localVariableStartLabel)

        //从局部变量表中读取source和target变量,把传入的两个object参数转换为对应的类型,压入操作数栈
        //因为要先getter再setter，所以先转换target，再转换source 保证source在栈顶

        //转换source 和 target的类型, 存入局部变量表
        codeEmitter.load_arg(1)
        codeEmitter.checkcast(targetType)
        val targetLocal = codeEmitter.make_local(targetType)
        codeEmitter.storeLocal(targetLocal)




        codeEmitter.load_arg(0)
        codeEmitter.checkcast(sourceType)
        val sourceLocal = codeEmitter.make_local(sourceType)
        codeEmitter.storeLocal(sourceLocal)

        //此时操作数栈是空的

        for ((reader, writer) in methodMapper) {
            val targetProperties = InternalUtil.getPropertyDescriptors(targetClass)
            val targetProperty = targetProperties.find { it.writeMethod == writer } ?: continue
            val targetGetterMethod = targetProperty.readMethod
            val targetGetterMethodInfo = ReflectUtils.getMethodInfo(targetGetterMethod)

            val readMethodInfo = ReflectUtils.getMethodInfo(reader)
            val writeMethodInfo = ReflectUtils.getMethodInfo(writer)
            val getterReturnType: Type = readMethodInfo.signature.returnType
            val setterArgType: Type = writeMethodInfo.signature.argumentTypes[0]

            //处理默认值 计算出默认值后生成一个字段，等到这个Copier生成后调用反射把默认值放到字段中。
            //注意 每一种方法中的默认值可以不同!!!
            val defaultFieldName = "${copyMethodType.name}_default_value_of_" + targetProperty.name

            val defaultValueProvider = config?.defaultValueProvider
            var defaultValue: Any? = null
            defaultValueProvider?.let { provider ->
                defaultValue = provider.provide(reader, writer, sourceClass, targetClass, copyMethodType)
                if (defaultValue != null) {
                    ce.declare_field(Opcodes.ACC_PRIVATE, defaultFieldName, setterArgType, null)
                    generateContext.addField(
                        defaultFieldName,
                        defaultValue
                    )
                }

            }

            val converterName = "${targetProperty.name}Converter"
            //处理转换器converter
            var converterField = config?.findPropertyValueConverter(
                reader,
                writer,
                sourceClass,
                targetClass,
                copyMethodType
            )?.let {
                generateContext.addValueConverter(converterName, it, ce)
            }
            //当没有属性转换器 且类型不兼容时 尝试查找类型转换器
            if (converterField == null && !InternalUtil.canAssignableFrom(
                    reader.genericReturnType,
                    writer.genericParameterTypes[0]
                )
            ) {
                converterField = config?.findTypeConverter(
                    reader.genericReturnType,
                    writer.genericParameterTypes[0],
                )?.let { typeConverter ->
                    generateContext.addValueConverter(converterName, object : PropertyValueConverter {
                        override fun convert(oldValue: Any?): Any? {
                            return typeConverter.doConvert(oldValue)
                        }

                    }, ce)
                }
            }


            val useConverter = converterField != null
            //如果类型不兼容 且不适用converter 则忽略该属性

            if (!InternalUtil.canAssignableFrom(reader.genericReturnType, writer.genericParameterTypes[0])) {
                //不允许自动拆包装 且不使用转换器
                if (!useConverter) {
                    if (!useConverter) {
                        continue
                    }

                } else if (javaObjectClass(writer.parameterTypes[0]) != javaObjectClass(reader.returnType)) {
                    //允许拆包装 但拆包装不兼容 且不使用转换器
                    if (!useConverter) {
                        continue
                    }
                }
            }


            //自定义reader方法
            val customReader = config?.propertyValueReaderProvider?.tryGetReader(reader)
            val customReaderFieldName = "custom_reader_${reader.name}"
            if (customReader != null) {
                ce.declare_field(
                    Opcodes.ACC_PRIVATE,
                    customReaderFieldName,
                    Type.getType(PropertyValueReader::class.java),
                    null
                )
                generateContext.addField(customReaderFieldName, customReader)
            }


            //不消耗栈元素 读取source的属性值
            val invokeReader = {
                if (customReader != null) {
                    codeEmitter.load_this()
                    codeEmitter.getfield(customReaderFieldName)
                    codeEmitter.loadLocal(sourceLocal)
                    codeEmitter.invoke_interface(
                        Type.getType(PropertyValueReader::class.java),
                        Signature("readValue", Type.getType(Any::class.java), arrayOf(Type.getType(Any::class.java)))
                    )
                } else {
                    codeEmitter.loadLocal(sourceLocal)
                    codeEmitter.invoke(readMethodInfo)
                }
            }
            //要求栈为[target,currentValue(栈顶)]
            val invokeWriter = {
                //确保栈顶元素和writer的参数类型兼容
                if (useConverter || defaultValue != null || customReader != null) {
                    smartCast(Any::class.java, writer.parameterTypes[0], codeEmitter)
                } else {
                    smartCast(reader.returnType, writer.parameterTypes[0], codeEmitter)
                }

                codeEmitter.invoke(writeMethodInfo)
                //对于链式调用的setter方法会返回this 要把它扔掉
                if (writer.returnType !== Void.TYPE) {
                    codeEmitter.pop()
                }
            }
            //不消耗栈元素 生成一个转换后的值
            val invokeReadAndConvert = {
                codeEmitter.load_this()
                codeEmitter.getfield(converterField!!.name)
                invokeReader()
                //此时栈顶可能是primitive类型 此时无法作为converter的参数 要判断一下
                //当使用customReader时 栈顶元素必然是非primitive类型 不用处理
                //当不使用customReader时 栈顶元素可能是primitive类型 要强制转换一下
                if (customReader == null) {
                    smartCast(reader.returnType, Any::class.java, codeEmitter)
                }

                codeEmitter.invoke_interface(
                    Type.getType(PropertyValueConverter::class.java),
                    Signature("convert", Type.getType(Any::class.java), arrayOf(Type.getType(Any::class.java)))
                )

            }


            val doSwap = {
                //复制source 和 target 然后调用方法
                codeEmitter.loadLocal(targetLocal)//target
                invokeReader()
                invokeWriter()
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
                    codeEmitter.loadLocal(targetLocal)

                    //可能调用converter进行转换 结束后栈顶是目标值
                    if (useConverter) {
                        invokeReadAndConvert()
                    } else {
                        invokeReader()
                    }
                    //此时栈顶就是目标元素了
                    if (needUseDefaultValue()) {
                        tryGetDefaultValue()
                    }
                    //执行setter
                    invokeWriter()
                }

            } else if (signature == COPY_NONNULL_DESCRIPTOR) {
                //这三个条件同时满足时 可以确保栈顶是primitive类型 那么直接swap就行 否则要考虑一系列处理
                if (useConverter || customReader != null || !TypeUtils.isPrimitive(getterReturnType)) {
                    codeEmitter.loadLocal(targetLocal)//先压入栈 用于调用setter

                    if (useConverter) {
                        invokeReadAndConvert()
                    } else {
                        invokeReader()
                    }
                    if (needUseDefaultValue()) {
                        tryGetDefaultValue()
                    }
                    //复制栈顶元素用于执行if指令
                    codeEmitter.dupForType(setterArgType)
                    val skip = codeEmitter.make_label()
                    val end = codeEmitter.make_label()

                    //如果为null则清理栈 否则执行setter 然后跳转到end
                    codeEmitter.ifnull(skip)
                    invokeWriter()
                    codeEmitter.visitJumpInsn(Opcodes.GOTO, end)
                    codeEmitter.visitLabel(skip)
                    //此时不执行setter 要把栈中的target和currentValue扔掉
                    codeEmitter.popForType(setterArgType)//扔掉currentValue
                    codeEmitter.pop()//扔掉target
                    codeEmitter.visitLabel(end)
                } else {
                    //primitive时不考虑default value
                    doSwap()
                }


            } else if (signature == MERGE_DESCRIPTOR) {
                if (TypeUtils.isPrimitive(setterArgType)) {
                    continue
                } else {
                    codeEmitter.loadLocal(targetLocal)//先压入栈 用于调用setter

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
                    codeEmitter.loadLocal(targetLocal)
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


        codeEmitter.visitLabel(methodEndLabel)

        //这里必须放到这里再写 因为里面的解析对Label的使用有问题 如果在visitLabel前调用 会导致出错
        //理论上它必须先检测label是否有被访问过 即Label.FLAG_RESOLVED标志。但是这里面直接读取offset了 所以必须放在后面

        //函数参数名 这里1和2是试出来的。。。 因为要经过remap
        codeEmitter.visitLocalVariable(
            "target", Type.getType(Object::class.java).descriptor,
            null,
            localVariableStartLabel, methodEndLabel, 2
        )
        codeEmitter.visitLocalVariable(
            "src", Type.getType(Object::class.java).descriptor,
            null,
            localVariableStartLabel, methodEndLabel, 1
        )


        codeEmitter.visitLocalVariable(
            "copy_target", targetLocal.type.descriptor,
            null,
            localVariableStartLabel, methodEndLabel, targetLocal.index
        )
        codeEmitter.visitLocalVariable(
            "copy_source", sourceLocal.type.descriptor,
            null,
            localVariableStartLabel, methodEndLabel, sourceLocal.index
        )

        codeEmitter.return_value()
        codeEmitter.end_method()
    }

}

