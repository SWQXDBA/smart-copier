package io.github.swqxdba.smartcopier


import net.sf.cglib.core.CodeEmitter
import net.sf.cglib.core.Signature
import java.lang.reflect.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

internal object InternalUtil {
    fun getPropertyDescriptorMap(clazz: Class<*>): Map<String, PropertyDescriptor> {
        val propertyDescriptors = getPropertyDescriptors(clazz)
        val map: MutableMap<String, PropertyDescriptor> = mutableMapOf()
        for (element in propertyDescriptors) {
            map[element.name] = element
        }
        return map
    }

    fun getPropertyDescriptor(clazz: Class<*>, name: String): PropertyDescriptor? {
        return getPropertyDescriptorMap(clazz)[name]
    }

    fun getPropertyDescriptors(clazz: Class<*>): Array<PropertyDescriptor> {
        val propertyDescriptors = mutableListOf<PropertyDescriptor>()

        val methods = clazz.methods
        val getters = mutableMapOf<String, Method>()
        val setters = mutableMapOf<String, Method>()

        for (method in methods) {
            if (method.modifiers == Modifier.PRIVATE || method.modifiers == Modifier.PROTECTED || method.modifiers == Modifier.STATIC) {
                continue
            }
            val methodName = method.name
            if (methodName.startsWith("get") && methodName.length > 3 && method.parameterCount == 0) {
                val propertyName = methodName.substring(3).replaceFirstChar { it.lowercase(Locale.getDefault()) }
                getters[propertyName] = method
            } else if (methodName.startsWith("is") && methodName.length > 2 && method.parameterCount == 0) {
                val propertyName = methodName.substring(2).replaceFirstChar { it.lowercase(Locale.getDefault()) }
                getters[propertyName] = method
            } else if (methodName.startsWith("set") && methodName.length > 3 && method.parameterCount == 1) {
                val propertyName = methodName.substring(3).replaceFirstChar { it.lowercase(Locale.getDefault()) }
                setters[propertyName] = method
            }
        }

        for ((propertyName, getter) in getters) {
            if (getter.name == "declaringClass" || getter.name == "class") {
                continue
            }
            val propertyDescriptor = PropertyDescriptor()
            propertyDescriptor.name = propertyName
            propertyDescriptor.readMethod = getter
            propertyDescriptor.writeMethod = setters[propertyName]
            propertyDescriptors.add(propertyDescriptor)
        }

        return propertyDescriptors.toTypedArray()
    }

    fun getElementClass(type: Type): Class<*>? {
        if (type is ParameterizedType) {
            val rawType = type.actualTypeArguments[0]
            if (rawType !is Class<*>) {
                return null
            }
            return rawType
        }
        if (type is Class<*>) {
            if (type.isArray) {
                return type.componentType
            }
        }
        return null
    }

    fun getOuterClass(type: Type): Class<*>? {
        if (type is ParameterizedType) {
            val rawType = type.rawType
            if (rawType !is Class<*>) {
                return null
            }
            return rawType
        }
        if (type is Class<*>) {
            return type
        }
        return null
    }

    fun getPrimitiveClass(clazz: Class<*>): Class<*>? {
        if (clazz.isPrimitive) {
            return clazz
        }
        val field: Field = try {
            clazz.getField("TYPE")
        }catch (e:NoSuchFieldException){
            return null
        }


        if (field.type == Class::class.java) {
            field.isAccessible = true
            if (Modifier.isStatic(field.modifiers)) {
                val typeClass = field.get(null)
                if (typeClass is Class<*>) {
                    return typeClass;
                }
            }
        }

        return null
    }

    /**
     * 获取常见集合的带初始容量的构造方法 必须保证传入的不是抽象类或者接口
     */
    fun findCollectionConstructor(type: Class<*>): (Int) -> Any {
        if (type == ArrayList::class.java) {
            return { java.util.ArrayList<Any>(it) }
        } else if (type == LinkedList::class.java) {
            return { java.util.LinkedList<Any>() }
        } else if (type == HashMap::class.java) {
            return { java.util.HashMap<Any, Any>(it) }
        } else if (type == HashSet::class.java) {
            return { java.util.HashSet<Any>(it) }
        }
        val noArgsConstructor = type.getConstructor()
        return { noArgsConstructor.newInstance() }
    }

    fun smartCast(fromType:Class<*>, toType:Class<*>, codeEmitter:CodeEmitter){
        if(fromType==toType){
            return
        }
        //两种均为基本类型 或者均为非基本类型 都直接转换
        if(fromType.isPrimitive && toType.isPrimitive){
            codeEmitter.checkcast(org.objectweb.asm.Type.getType(toType))
            return
        }
        if(!fromType.isPrimitive && !toType.isPrimitive){
            codeEmitter.checkcast(org.objectweb.asm.Type.getType(toType))
            return
        }

        if(toType.isPrimitive){
            if(fromType!=javaObjectClass(toType)){
                codeEmitter.checkcast(org.objectweb.asm.Type.getType(javaObjectClass(toType)))
            }

            when(toType){
                Int::class.java->{
                    codeEmitter.invoke_virtual(
                        org.objectweb.asm.Type.getType(java.lang.Integer::class.java),
                        Signature("intValue", org.objectweb.asm.Type.getType(Int::class.java), arrayOf())
                    )
                }
                Long::class.java->{
                    codeEmitter.invoke_virtual(
                        org.objectweb.asm.Type.getType(java.lang.Long::class.java),
                        Signature("longValue", org.objectweb.asm.Type.getType(Long::class.java), arrayOf())
                    )
                }
                Float::class.java->{
                    codeEmitter.invoke_virtual(
                        org.objectweb.asm.Type.getType(java.lang.Float::class.java),
                        Signature("floatValue", org.objectweb.asm.Type.getType(Float::class.java), arrayOf())
                    )
                }
                Double::class.java->{
                    codeEmitter.invoke_virtual(
                        org.objectweb.asm.Type.getType(java.lang.Double::class.java),
                        Signature("doubleValue", org.objectweb.asm.Type.getType(Double::class.java), arrayOf())
                    )
                }
                Byte::class.java->{
                    codeEmitter.invoke_virtual(
                        org.objectweb.asm.Type.getType(java.lang.Byte::class.java),
                        Signature("byteValue", org.objectweb.asm.Type.getType(Byte::class.java),arrayOf())
                    )
                }
                Short::class.java->{
                    codeEmitter.invoke_virtual(
                        org.objectweb.asm.Type.getType(java.lang.Short::class.java),
                        Signature("shortValue", org.objectweb.asm.Type.getType(Short::class.java), arrayOf())
                    )
                }
            }

        }else{
            when(fromType){
                Int::class.javaPrimitiveType -> {
                    codeEmitter.invoke_static(
                        org.objectweb.asm.Type.getType(Int::class.javaObjectType),
                        Signature("valueOf", org.objectweb.asm.Type.getType(Int::class.javaObjectType), arrayOf(org.objectweb.asm.Type.getType(Int::class.javaPrimitiveType)))
                    )
                    // Assuming we are boxing the primitive int to Integer
                }
                Long::class.javaPrimitiveType -> {
                    codeEmitter.invoke_static(
                        org.objectweb.asm.Type.getType(Long::class.javaObjectType),
                        Signature("valueOf", org.objectweb.asm.Type.getType(Long::class.javaObjectType), arrayOf(org.objectweb.asm.Type.getType(Long::class.javaPrimitiveType)))
                    )
                }
                Float::class.javaPrimitiveType -> {
                    codeEmitter.invoke_static(
                        org.objectweb.asm.Type.getType(Float::class.javaObjectType),
                        Signature("valueOf", org.objectweb.asm.Type.getType(Float::class.javaObjectType), arrayOf(org.objectweb.asm.Type.getType(Float::class.javaPrimitiveType)))
                    )
                }
                Double::class.javaPrimitiveType -> {
                    codeEmitter.invoke_static(
                        org.objectweb.asm.Type.getType(Double::class.javaObjectType),
                        Signature("valueOf", org.objectweb.asm.Type.getType(Double::class.javaObjectType), arrayOf(org.objectweb.asm.Type.getType(Double::class.javaPrimitiveType)))
                    )
                }
                Byte::class.javaPrimitiveType -> {
                    codeEmitter.invoke_static(
                        org.objectweb.asm.Type.getType(Byte::class.javaObjectType),
                        Signature("valueOf", org.objectweb.asm.Type.getType(Byte::class.javaObjectType), arrayOf(org.objectweb.asm.Type.getType(Byte::class.javaPrimitiveType)))
                    )
                }
                Short::class.javaPrimitiveType -> {
                    codeEmitter.invoke_static(
                        org.objectweb.asm.Type.getType(Short::class.javaObjectType),
                        Signature("valueOf", org.objectweb.asm.Type.getType(Short::class.javaObjectType), arrayOf(org.objectweb.asm.Type.getType(Short::class.javaPrimitiveType)))
                    )
                }
                Boolean::class.javaPrimitiveType -> {
                    codeEmitter.invoke_static(
                        org.objectweb.asm.Type.getType(Boolean::class.javaObjectType),
                        Signature("valueOf", org.objectweb.asm.Type.getType(Boolean::class.javaObjectType), arrayOf(org.objectweb.asm.Type.getType(Boolean::class.javaPrimitiveType)))
                    )
                }
            }

        }
    }
    fun javaObjectClass(type:Class<*>):Class<*>{
        return when (type.name) {
            "boolean" -> java.lang.Boolean::class.java
            "char"    -> Character::class.java
            "byte"    -> java.lang.Byte::class.java
            "short"   -> java.lang.Short::class.java
            "int"     -> Integer::class.java
            "float"   -> java.lang.Float::class.java
            "long"    -> java.lang.Long::class.java
            "double"  -> java.lang.Double::class.java
            "void"    -> Void::class.java
            else -> type
        }
    }
}

class RandomCodeRule {
    val len: Long? = null
    val incrRule: String? = null
}

