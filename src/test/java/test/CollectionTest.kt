package test

import io.github.swqxdba.smartcopier.SmartCopier
import io.github.swqxdba.smartcopier.converter.TypeConverter
import io.github.swqxdba.smartcopier.converter.TypeConverterProvider
import org.junit.jupiter.api.Test
import java.lang.reflect.Type

class CollectionTest {

    data class Data1(var list: List<String> = listOf("1"))


    data class Data2(var list: List<Int> = emptyList())

    @Test
    fun testCollection() {
        val smartCopier = SmartCopier()
        val data1 = Data1()
        val data2 = Data2()
        smartCopier.copy(data1, data2)
        assert(data2.list.isEmpty())
    }

    @Test
    fun testCollectionTypeConverter() {
        val smartCopier = SmartCopier()
        smartCopier.defaultConfig!!.addConverter(object : TypeConverterProvider {
            override fun tryGetConverter(from: Type, to: Type): TypeConverter? {
                if (to == Int::class.javaObjectType) {
                    return object : TypeConverter {
                        override fun doConvert(from: Any?): Any? {
                            return from.toString().toInt()
                        }
                    }
                }
                return null
            }

        })
        val data1 = Data1()
        val data2 = Data2()
        smartCopier.copy(data1, data2)
        assert(data2.list[0] == 1)
    }


}

