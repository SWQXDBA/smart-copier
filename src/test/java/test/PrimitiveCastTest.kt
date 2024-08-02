package test

import io.github.swqxdba.smartcopier.CopyConfig
import io.github.swqxdba.smartcopier.CopyMethodType
import io.github.swqxdba.smartcopier.PropertyValueConverter
import io.github.swqxdba.smartcopier.SmartCopier
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

class PrimitiveCastTest {

    data class Data(var value: Int)

    data class Data2(var value: Int?)

    /**
     *
     * (int)-converter->(Integer)--/>(int)
     * 这里经过convert后 会从primitive类型变成包装类 然后再尝试赋值给primitive类型。
     * 要校验这种处理是否正确。因为拆箱和装箱是编译器处理的。而不是jvm的功能。
     * 这里要检验SmartCopier生成的拆箱和装箱逻辑是否正确
     */
    @Test
    fun test1() {

        SmartCopier.debugMode=true
        SmartCopier.debugOutPutDir="./degg"
        val d1 = Data(1)
        var propertyValueConverters: MutableList<PropertyValueConverter> =
            mutableListOf(object : PropertyValueConverter {
                override fun shouldIntercept(
                    sourceGetter: Method,
                    targetSetter: Method,
                    sourceClass: Class<*>,
                    targetClass: Class<*>,
                    copyMethodType: CopyMethodType
                ): Boolean {
                    return true
                }

                override fun convert(oldValue: Any?): Any? {
                    return oldValue
                }

            })
        SmartCopier.copy(Data(2), d1, CopyConfig(propertyValueConverters = propertyValueConverters))
    }

    @Test
    fun primitiveWrapperAutoCast() {
        SmartCopier.defaultConfig = CopyConfig(allowPrimitiveWrapperAutoCast = true)
        SmartCopier.debugMode=true
        SmartCopier.debugOutPutDir="./degg"
        //参考基础类型/非基础类型之间是否兼容
        SmartCopier.copy(Data(2), Data2(1))
        SmartCopier.copy(Data2(2), Data(1))
    }
}