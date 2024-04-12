package org.swqxdba.smartconvert.converters

import org.swqxdba.smartconvert.CopyMethodType
import org.swqxdba.smartconvert.InternalUtil
import org.swqxdba.smartconvert.PropertyValueConverter
import org.swqxdba.smartconvert.SmartCopier
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
            val returnClassType = sourceGetter.returnType
            val setterClassType = targetSetter.parameterTypes[0]
            val beanConverter = SmartCopier.beanConvertProvider?.tryGetConverter(
                returnClassType,
                setterClassType
            )
            if(beanConverter!=null){
                return object :PropertyValueConverter{
                    override fun shouldIntercept(
                        sourceGetter2: Method,
                        targetSetter2: Method,
                        sourceClass2: Class<*>,
                        targetClass2: Class<*>,
                        copyMethodType2: CopyMethodType
                    ): Boolean {
                        return sourceGetter2.returnType==returnClassType && targetSetter2.parameterTypes[0]==setterClassType
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