package test

import io.github.swqxdba.smartcopier.CopyMethodType
import io.github.swqxdba.smartcopier.SmartCopier
import io.github.swqxdba.smartcopier.converter.DefaultValueProvider
import io.github.swqxdba.smartcopier.converter.PropertyValueConverter
import io.github.swqxdba.smartcopier.converter.PropertyValueConverterProvider
import org.junit.jupiter.api.Test
import java.lang.reflect.Method

class DefaultValueTest {

    data class Data(var a: Int? = 0, var b: Int = 0, var c: Long? = null, var d: String? = null)

    @Test
    fun testDefaultValueProvider() {
        val smartCopier = SmartCopier()
        smartCopier.defaultConfig!!.defaultValueProvider = object : DefaultValueProvider {
            override fun provide(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass:  Class<*>,
                copyMethodType: CopyMethodType
            ): Any? {
                if(targetSetter.parameterTypes[0]==Long::class.javaObjectType){
                    return 1L
                }
                if(targetSetter.parameterTypes[0]==String::class.java){
                    return ""
                }
                return null
            }
        }
        val data = Data()
        val data2 = Data()
        smartCopier.copy(data, data2)
        assert(Data(c=1, d="")==data2)
    }


    //当有转换器时且转换器返回非null时 用转换器的值
    @Test
    fun testDefaultValueProviderWithConverter() {
        val smartCopier = SmartCopier()
        smartCopier.defaultConfig!!.defaultValueProvider = object : DefaultValueProvider {
            override fun provide(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass:  Class<*>,
                copyMethodType: CopyMethodType
            ): Any? {
                if(targetSetter.parameterTypes[0]==Long::class.javaObjectType){
                    return 1L
                }
                if(targetSetter.parameterTypes[0]==String::class.java){
                    return "66"
                }
                return null
            }
        }
        smartCopier.defaultConfig!!.addConverter(object : PropertyValueConverterProvider{
            override fun tryGetConverter(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass: Class<*>,
                copyMethodType: CopyMethodType
            ): PropertyValueConverter? {
                if(targetSetter.parameterTypes[0]==Long::class.javaObjectType){
                    return  object : PropertyValueConverter{
                        override fun convert(oldValue: Any?): Any? {
                             return 2L
                        }
                    }
                }
                if(targetSetter.parameterTypes[0]==String::class.java){
                    return  object : PropertyValueConverter{
                        override fun convert(oldValue: Any?): Any? {
                            return null
                        }
                    }
                }
                return null
            }

        })
        val data = Data()
        val data2 = Data()
        smartCopier.copy(data, data2)
        //2用的是转换器提供的,"66"使用的是默认值提供者
        assert(Data(c=2, d="66")==data2)
    }
}