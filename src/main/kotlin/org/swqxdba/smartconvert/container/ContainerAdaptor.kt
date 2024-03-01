package org.swqxdba.smartconvert.container

import org.swqxdba.smartconvert.CopyMethodType
import org.swqxdba.smartconvert.InternalUtil
import org.swqxdba.smartconvert.PropertyValueConverter
import org.swqxdba.smartconvert.SmartCopier
import java.lang.Exception
import java.lang.reflect.Method

class ContainerAdaptor : PropertyValueConverter {

    lateinit var setterContainerWrapper: IterWrapper

    lateinit var getterContainerWrapper: IterWrapper

    var elementTransfer: ((Any) -> Any)? = null

    var sourceGetter:Method?=null
    var targetSetter:Method?=null


    override fun shouldIntercept(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        copyMethodType: CopyMethodType
    ): Boolean {
        this.sourceGetter = sourceGetter
        this.targetSetter = targetSetter
        val setterType = targetSetter.genericParameterTypes[0]
        val getterType = sourceGetter.genericReturnType
        if (!IterWrapper.canWrap(setterType) || !IterWrapper.canWrap(getterType)) {
            return false
        }
        val setterElementClass = InternalUtil.getElementClass(setterType) ?: return false
        val getterElementClass = InternalUtil.getElementClass(getterType) ?: return false
        val setterOuterClass = InternalUtil.getOuterClass(setterType) ?: return false
        val getterOuterClass = InternalUtil.getOuterClass(getterType) ?: return false

        //集合元素的类型不相同 且不兼容
        if (setterElementClass != getterElementClass && !setterElementClass.isAssignableFrom(getterElementClass) ) {
            val primitiveClass1 = InternalUtil.getPrimitiveClass(setterElementClass)
            val primitiveClass2 = InternalUtil.getPrimitiveClass(getterElementClass)
            //不可以转化成兼容的基本类型
            if (primitiveClass1 != primitiveClass2||primitiveClass1==null) {
                //尝试用beanConverter
                val beanConverter =
                    SmartCopier.beanConvertProvider?.tryGetConverter(getterElementClass, setterElementClass)
                        ?: return false
                elementTransfer = { beanConverter.doConvert(it) }
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

    override fun convert(oldValue: Any?): Any? {
        if (oldValue == null) {
            return null
        }
        val size = getterContainerWrapper.sizeGetter(oldValue)
        val instance = setterContainerWrapper.instanceCreator(size)
        var elements = getterContainerWrapper.elementsResolver(oldValue)
        elementTransfer?.let { transfer ->
            elements = elements.map { item ->
                transfer(item)
            }
        }
        setterContainerWrapper.fillExecutor(instance, elements)

        return instance
    }
}