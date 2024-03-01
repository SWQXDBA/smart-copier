package org.swqxdba.smartconvert


import java.beans.BeanInfo
import java.beans.Introspector


import java.lang.RuntimeException
import java.lang.reflect.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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
}

class RandomCodeRule {
    val len: Long? = null
    val incrRule: String? = null
}

