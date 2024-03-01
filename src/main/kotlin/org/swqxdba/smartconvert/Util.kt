package org.swqxdba.smartconvert


import java.beans.BeanInfo

import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.lang.RuntimeException
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

internal object InternalUtil {
    fun getPropertyDescriptorMap(clazz: Class<*>): Map<String, PropertyDescriptor> {
        val beanInfo = Introspector.getBeanInfo(clazz) ?: return emptyMap()

        val propertyDescriptors = beanInfo.propertyDescriptors
        val map: MutableMap<String, PropertyDescriptor> = mutableMapOf()
        for (element in propertyDescriptors) {
            map[element.name] = element
        }
        return map
    }

    fun getPropertyDescriptor(clazz: Class<*>, name: String): PropertyDescriptor? {
        return getPropertyDescriptorMap(clazz)[name]
    }


    fun getPropertyDescriptors(clazz: Class<*>?): Array<PropertyDescriptor> {
        val beanInfo: BeanInfo = Introspector.getBeanInfo(clazz)
        return beanInfo.propertyDescriptors.filter { it.name!="class" }.toTypedArray()
    }

    fun getElementClass(type:Type):Class<*>?{
        if (type is ParameterizedType) {
            val rawType = type.actualTypeArguments[0]
            if(rawType !is Class<*>){
                return null
            }
            return rawType
        }
        if(type is Class<*>){
            if(type.isArray){
                return type.componentType
            }
        }
        return null
    }

    fun getOuterClass(type:Type):Class<*>?{
        if (type is ParameterizedType) {
            val rawType = type.rawType
            if(rawType !is Class<*>){
                return null
            }
            return rawType
        }
        if(type is Class<*>){
            return type
        }
        return null
    }

    fun getPrimitiveClass(clazz: Class<*>): Class<*>?{
        if(clazz.isPrimitive){
            return clazz
        }
        val field: Field? = clazz.getField("TYPE")
        if(field==null){
            return null
        }

        if (field.type == Class::class.java) {
            field.isAccessible = true
            if (Modifier.isStatic(field.modifiers)) {
                val typeClass = field.get(null)
                if(typeClass is Class<*>){
                    return typeClass;
                }
            }
        }

        return null
    }

    /**
     * 获取常见集合的带初始容量的构造方法 必须保证传入的不是抽象类或者接口
     */
    fun findCollectionConstructor(type:Class<*>): (Int)->Any {
        if(type == ArrayList::class.java){
            return {  java.util.ArrayList<Any>(it) }
        }else  if(type == LinkedList::class.java){
            return {  java.util.LinkedList<Any>() }
        }else  if(type == HashMap::class.java){
            return {  java.util.HashMap<Any,Any>(it) }
        }
        else  if(type == HashSet::class.java){
            return {  java.util.HashSet<Any>(it) }
        }
        val noArgsConstructor = type.getConstructor()
        return {noArgsConstructor.newInstance()}
    }
}

class RandomCodeRule {
    val len: Long? = null
    val incrRule: String? = null
}

