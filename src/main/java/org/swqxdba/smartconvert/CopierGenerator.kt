package org.swqxdba.smartconvert


import cn.hutool.core.bean.BeanUtil
import net.sf.cglib.core.*
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.swqxdba.CopyConfig
import org.swqxdba.SmartUtil
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Paths

class Person {
    var name: String? = null
    var age: Long = 1
}

fun main() {
    val generateClass = CopierGenerator(Person::class.java, Person::class.java).generateClass()
    val newInstance = generateClass!!.newInstance() as Copier
    val person = Person()
    person.name = "123"
    person.age = 123151
    val person2 = Person()
    newInstance.copy(person, person2)
    println(person2.age)
}

class CopierGenerator(val sourceClass: Class<*>, val targetClass: Class<*>, val config: CopyConfig? = null) {

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

    val NOPY_NONNULL_DESCRIPTOR =
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

        generateMethod(ce, COPY_DESCRIPTOR)
        generateMethod(ce, NOPY_NONNULL_DESCRIPTOR)
        generateMethod(ce, MERGE_DESCRIPTOR)

        ce.end_class()

        val toByteArray = cv.toByteArray()
        Files.write(Paths.get(generateClassName + ".class"), toByteArray)
        return defineClass(generateClassName, toByteArray)
    }

    fun generateMethod(ce: ClassEmitter, signature: Signature) {


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

        for ((reader, writer) in methodMapper) {
            val readMethodInfo = ReflectUtils.getMethodInfo(reader)
            val writeMethodInfo = ReflectUtils.getMethodInfo(writer)
            val getterReturnType: Type = readMethodInfo.signature.returnType
            val setterArgType: Type = writeMethodInfo.signature.argumentTypes[0]

            val doSwap = {
                //复制source 和 target 然后调用方法
                codeEmitter.load_local(targetLocal)//target
                codeEmitter.load_local(sourceLocal)//source
                codeEmitter.invoke(readMethodInfo)
                codeEmitter.invoke(writeMethodInfo)
            }

            if (signature == COPY_DESCRIPTOR) {

                doSwap()
            } else {
                if (signature == NOPY_NONNULL_DESCRIPTOR) {
                    if (TypeUtils.isPrimitive(getterReturnType)) {
                        doSwap()
                        continue
                    }
                    val tempLocal = codeEmitter.make_local(getterReturnType)
                    codeEmitter.load_local(sourceLocal)//source
                    codeEmitter.invoke(readMethodInfo)//获取source中的属性值
                    codeEmitter.store_local(tempLocal)//把source中的属性值存入临时变量
                    //if(temp != null)
                    codeEmitter.load_local(tempLocal)
                    val skipLabel = codeEmitter.make_label()
                    codeEmitter.ifnull(skipLabel)
                    codeEmitter.load_local(targetLocal)//target
                    codeEmitter.load_local(tempLocal)
                    codeEmitter.invoke(writeMethodInfo)//调用setter方法
                    codeEmitter.visitLabel(skipLabel)
                } else if (signature == MERGE_DESCRIPTOR) {
                    if (TypeUtils.isPrimitive(setterArgType)) {
                        continue
                    }
                    val targetProperties = BeanUtil.getPropertyDescriptors(targetClass)
                    val targetGetterMethod = targetProperties.find { it.writeMethod == writer }?.readMethod
                    val targetGetterMethodInfo = ReflectUtils.getMethodInfo(targetGetterMethod)

                    //target的属性没有getter方法
                    if (targetGetterMethod == null ) {
                        doSwap()
                    }
                    codeEmitter.load_local(targetLocal)
                    codeEmitter.invoke(targetGetterMethodInfo)//获取target中的属性值
                    //if(target.getValue() != null)
                    val skipLabel = codeEmitter.make_label()
                    codeEmitter.ifnonnull(skipLabel)
                    doSwap()
                    codeEmitter.visitLabel(skipLabel)
                }
            }

        }

        codeEmitter.return_value()
        codeEmitter.end_method()
    }
}

