package org.swqxdba

import java.beans.Introspector
import java.beans.PropertyDescriptor

object SmartUtil {
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
}

class RandomCodeRule {
    val len: Long? = null
    val incrRule: String? = null
}

