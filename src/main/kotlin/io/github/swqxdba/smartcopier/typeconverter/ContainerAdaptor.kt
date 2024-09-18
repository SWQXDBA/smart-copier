package io.github.swqxdba.smartcopier.typeconverter

import io.github.swqxdba.smartcopier.CopyConfig
import io.github.swqxdba.smartcopier.CopyMethodType
import io.github.swqxdba.smartcopier.InternalUtil
import io.github.swqxdba.smartcopier.SmartCopier
import io.github.swqxdba.smartcopier.container.IterWrapper
import java.lang.Exception
import java.lang.reflect.Method
import java.lang.reflect.Type

class ContainerAdaptor (private val config:CopyConfig):TypeConverter {

    lateinit var setterContainerWrapper: IterWrapper

    lateinit var getterContainerWrapper: IterWrapper

    var elementTransfer: ((Any?) -> Any?)? = null



    override fun shouldConvert(from: Type, to: Type): Boolean {
        if (!IterWrapper.canWrap(to) || !IterWrapper.canWrap(from)) {
            return false
        }
        val toElementType = InternalUtil.getElementType(to) ?: return false
        val fromElementType = InternalUtil.getElementType(from) ?: return false
        val setterOuterClass = InternalUtil.getOuterClass(to) ?: return false
        val getterOuterClass = InternalUtil.getOuterClass(from) ?: return false


        //集合元素的类型不相同 且不兼容
        if (!InternalUtil.canAssignableFrom(fromElementType,toElementType)) {
            val primitiveClass1 = InternalUtil.getPrimitiveClass(toElementType)
            val primitiveClass2 = InternalUtil.getPrimitiveClass(fromElementType)
            //不可以转化成兼容的基本类型
            if (primitiveClass1 != primitiveClass2||primitiveClass1==null) {
                //尝试用
                val typeConverter =
                    config.findTypeConverter(fromElementType, toElementType)
                        ?: return false
                elementTransfer = { typeConverter.doConvert(it) }
            }

        }

        try {
            setterContainerWrapper = IterWrapper(setterOuterClass)
            getterContainerWrapper = IterWrapper(getterOuterClass)
        } catch (e: Exception) {
            println("ignore container transfer...")
            return false
        }


        return true;
    }


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