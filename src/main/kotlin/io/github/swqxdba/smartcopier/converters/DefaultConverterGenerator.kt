package io.github.swqxdba.smartcopier.converters

import io.github.swqxdba.smartcopier.CopyMethodType
import io.github.swqxdba.smartcopier.PropertyValueConverter
import io.github.swqxdba.smartcopier.SmartCopier
import io.github.swqxdba.smartcopier.container.ContainerAdaptor
import io.github.swqxdba.smartcopier.container.IterWrapper
import java.lang.reflect.Method

class DefaultConverterGenerator {
    companion object{
        fun tryGetConverter(
            sourceGetter: Method,
            targetSetter: Method,
            sourceClass: Class<*>,
            targetClass: Class<*>,
            copyMethodType: CopyMethodType,
            smartCopier: SmartCopier
        ): PropertyValueConverter? {
            val setterType = targetSetter.genericParameterTypes[0]
            val getterType = sourceGetter.genericReturnType
            // 相同类型不执行转换。
            if(setterType==getterType){
                return null
            }
            if (IterWrapper.canWrap(setterType) && IterWrapper.canWrap(getterType)) {
                val containerAdaptor = ContainerAdaptor(smartCopier)

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
            val returnClassType = sourceGetter.returnType
            val setterClassType = targetSetter.parameterTypes[0]

            val beanConverter = smartCopier.beanConvertProvider?.tryGetConverter(
                returnClassType,
                setterClassType
            )
            if(beanConverter!=null){
                return object :PropertyValueConverter{
                    override fun shouldIntercept(
                        sourceGetter: Method,
                        targetSetter: Method,
                        sourceClass: Class<*>,
                        targetClass: Class<*>,
                        copyMethodType: CopyMethodType
                    ): Boolean {
                        return sourceGetter.returnType==returnClassType && targetSetter.parameterTypes[0]==setterClassType
                    }

                    override fun convert(oldValue: Any?): Any? {
                        if(oldValue==null){
                            return null;
                        }
                        return beanConverter.doConvert(oldValue)
                    }
                }
            }
            return null

        }
    }

}