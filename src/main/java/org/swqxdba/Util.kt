package org.swqxdba



import ch.qos.logback.core.joran.util.beans.BeanUtil
import com.sun.org.apache.xerces.internal.impl.xpath.regex.CaseInsensitiveMap
import java.beans.Introspector
import java.beans.PropertyDescriptor

object SmartUtil{
    fun getPropertyDescriptorMap(clazz: Class<*>): Map<String, PropertyDescriptor> {
        val beanInfo = Introspector.getBeanInfo(clazz)?:return emptyMap()

        val propertyDescriptors = beanInfo.propertyDescriptors
        val map: MutableMap<String, PropertyDescriptor> = mutableMapOf()
        val var5 = propertyDescriptors.size
        for (var6 in 0 until var5) {
            val propertyDescriptor = propertyDescriptors[var6]
            map[propertyDescriptor.name] = propertyDescriptor
        }
        return map
    }
    fun getPropertyDescriptor(clazz: Class<*>,name:String):PropertyDescriptor?{
        return getPropertyDescriptorMap(clazz)[name]
    }
}


