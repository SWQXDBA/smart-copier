package org.swqxdba.smartconvert.converters

import org.swqxdba.smartconvert.CopyMethodType
import org.swqxdba.smartconvert.InternalUtil
import org.swqxdba.smartconvert.PropertyValueConverter
import org.swqxdba.smartconvert.container.ContainerAdaptor
import org.swqxdba.smartconvert.container.IterWrapper
import java.lang.reflect.Method

class DefaultConverterGenerator {
    companion object{
        fun tryGetConverter(
            sourceGetter: Method,
            targetSetter: Method,
            sourceClass: Class<*>,
            targetClass: Class<*>,
            copyMethodType: CopyMethodType
        ): PropertyValueConverter? {
            val setterType = targetSetter.genericParameterTypes[0]
            val getterType = sourceGetter.genericReturnType
            if (IterWrapper.canWrap(setterType) && IterWrapper.canWrap(getterType)) {
                val containerAdaptor = ContainerAdaptor()

                return if (containerAdaptor.shouldIntercept(
                        sourceGetter,
                        targetSetter,
                        sourceClass,
                        targetClass,
                        copyMethodType
                    )
                )
                    containerAdaptor
                else
                    null

            }
            return null

        }
    }

}