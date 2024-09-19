package io.github.swqxdba.smartcopier.converter

import io.github.swqxdba.smartcopier.CopyConfig
import io.github.swqxdba.smartcopier.InternalUtil
import io.github.swqxdba.smartcopier.container.IterWrapper
import java.lang.Exception
import java.lang.reflect.Type

/**
 * 用于处理集合/数组的转换
 */
class ContainerTypeConverterProvider(private val config: CopyConfig) : TypeConverterProvider {
    override fun tryGetConverter(from: Type, to: Type): TypeConverter? {
        val setterContainerWrapper: IterWrapper
        val getterContainerWrapper: IterWrapper
        var elementTransfer: ((Any?) -> Any?)? = null

        if (!IterWrapper.canWrap(to) || !IterWrapper.canWrap(from)) {
            return null
        }
        val toElementType = InternalUtil.getElementType(to) ?: return null
        val fromElementType = InternalUtil.getElementType(from) ?: return null
        val setterOuterClass = InternalUtil.getOuterClass(to) ?: return null
        val getterOuterClass = InternalUtil.getOuterClass(from) ?: return null


        //集合元素的类型不相同 且不兼容
        if (!InternalUtil.canAssignableFrom(fromElementType, toElementType)) {
            val primitiveClass1 = InternalUtil.getPrimitiveClass(toElementType)
            val primitiveClass2 = InternalUtil.getPrimitiveClass(fromElementType)
            //不可以转化成兼容的基本类型
            if (primitiveClass1 != primitiveClass2 || primitiveClass1 == null) {
                //尝试用
                val typeConverter =
                    config.findTypeConverter(fromElementType, toElementType)
                        ?: return null
                elementTransfer = { typeConverter.doConvert(it) }
            }

        }

        try {
            setterContainerWrapper = IterWrapper(setterOuterClass)
            getterContainerWrapper = IterWrapper(getterOuterClass)
        } catch (e: Exception) {
            return null
        }
        return ContainerTypeConverter(setterContainerWrapper, getterContainerWrapper, elementTransfer)
    }
}