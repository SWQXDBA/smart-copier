package io.github.swqxdba.smartcopier.converter

import io.github.swqxdba.smartcopier.CopyConfig
import io.github.swqxdba.smartcopier.container.IterWrapper

class ContainerTypeConverter(
    private val config: CopyConfig,
    var setterContainerWrapper: IterWrapper,
    var getterContainerWrapper: IterWrapper,
    var elementTransfer: ((Any?) -> Any?)? = null
) : TypeConverter {


    private fun isJavaImmutableType(type: Class<*>): Boolean {
        return type == String::class.java ||
                type == Byte::class.java ||
                type == Short::class.java ||
                type == Int::class.java ||
                type == Long::class.java ||
                type == Float::class.java ||
                type == Double::class.java ||
                type == Char::class.java ||
                type == Boolean::class.java ||
                type == java.util.Date::class.java ||
                type == java.time.LocalDate::class.java ||
                type == java.sql.Timestamp::class.java ||
                type == java.sql.Date::class.java ||
                type == java.sql.Time::class.java
    }


    override fun doConvert(from: Any?): Any? {
        if (from == null) {
            return null
        }
        val size = getterContainerWrapper.sizeGetter(from)
        val instance = setterContainerWrapper.instanceCreator(size)
        var elements = getterContainerWrapper.elementsResolver(from)
        elementTransfer?.let { transfer ->
            elements = elements.map { item ->
                transfer(item)
            }
        }
        setterContainerWrapper.fillExecutor(instance, elements)

        return instance
    }

}