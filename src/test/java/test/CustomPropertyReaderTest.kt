package test

import io.github.swqxdba.smartcopier.CopyConfig
import io.github.swqxdba.smartcopier.SmartCopier
import io.github.swqxdba.smartcopier.propertyreader. CustomPropertyReaderProvider
import io.github.swqxdba.smartcopier.propertyreader.PropertyValueReader
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.reflect.Method


class CustomPropertyReaderTest {
    data class Data(var a: Int, var b: Int) {}

    private val SmartCopier = SmartCopier()
    @Test
    fun test() {
        SmartCopier.debugMode=true
        SmartCopier.debugOutPutDir="./degg"
        val target = Data(0, 0)
        var trigger = 0
        val provider: CustomPropertyReaderProvider = object : CustomPropertyReaderProvider {
            override fun tryGetReader(getterMethod: Method): PropertyValueReader? {
                if (getterMethod.name.lowercase().contains("a")) {
                    return object : PropertyValueReader {
                        override fun readValue(src: Any?): Any? {
                            trigger++
                            return (getterMethod(src) as Int) + 199
                        }
                    }
                }
                return null
            }
        }
        SmartCopier.copy(
            Data(1, 2), target, CopyConfig(propertyValueReaderProvider = provider)
        )
        Assertions.assertEquals(1, trigger)
        Assertions.assertEquals(target.a,200)

    }

}