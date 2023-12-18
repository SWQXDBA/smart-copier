package org.swqxdba.smartconvert


import java.beans.BeanInfo

import java.beans.Introspector
import java.beans.PropertyDescriptor

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
}

class RandomCodeRule {
    val len: Long? = null
    val incrRule: String? = null
}

