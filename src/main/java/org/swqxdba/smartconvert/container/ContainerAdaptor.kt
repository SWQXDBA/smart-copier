package org.swqxdba.smartconvert.container

import org.swqxdba.smartconvert.CopyMethodType
import org.swqxdba.smartconvert.InternalUtil
import org.swqxdba.smartconvert.PropertyValueConverter
import java.lang.Exception
import java.lang.reflect.Method

class ContainerAdaptor : PropertyValueConverter {

    lateinit var setterContainerWrapper: IterWrapper

    lateinit var getterContainerWrapper: IterWrapper

    var elementTransfer: ((Any) -> Any)? = null

    override fun shouldIntercept(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        copyMethodType: CopyMethodType
    ): Boolean {
        val setterType = targetSetter.genericParameterTypes[0]
        val getterType = sourceGetter.genericReturnType
        if (!IterWrapper.canWrap(setterType) || !IterWrapper.canWrap(getterType)) {
            return false
        }
        val setterElementClass = InternalUtil.getElementClass(setterType) ?: return false
        val getterElementClass = InternalUtil.getElementClass(getterType) ?: return false
        val setterOuterClass = InternalUtil.getOuterClass(setterType) ?: return false
        val getterOuterClass = InternalUtil.getOuterClass(getterType) ?: return false
        //集合元素的类型都相同 且集合类型兼容
        if (setterElementClass == getterElementClass && setterOuterClass.isAssignableFrom(getterOuterClass)) {
            return false
        }

        try {
            setterContainerWrapper = IterWrapper(setterOuterClass)
            getterContainerWrapper = IterWrapper(getterOuterClass)
        } catch (e: Exception) {
            println("ignore container transfer...")
            return false
        }

        //不兼容的类型
        if (!setterElementClass.isAssignableFrom(getterElementClass)) {
            //可以转化成兼容的基本类型
            if (InternalUtil.getPrimitiveClass(setterElementClass) == InternalUtil.getPrimitiveClass(getterElementClass)) {
                return true
            }
            return false;
        }


        return true;

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