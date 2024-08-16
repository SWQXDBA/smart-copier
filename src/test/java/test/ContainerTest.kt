package test

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import io.github.swqxdba.smartcopier.SmartCopier


class ContainerTest {



    @Test
    fun testArrayAndList(): Unit {
        data class Data1(
            var elements: Array<String>
        )

        data class Data2(
            var elements: List<String>
        )

        val src = Data1(arrayOf("123"))
        val target = Data2(listOf())
        SmartCopier.copy(src, target)
        println(target)
        assert(target.elements[0] == "123")
        target.elements = listOf("456")
        SmartCopier.copy(target, src)
        assert(src.elements[0] == "456")
        println(src)
    }

    @Test
    fun testPrimitiveArrayAndList(): Unit {
        data class Data1(
            var elements: List<Int?>
        )

        data class Data2(
            var elements: IntArray
        )

        val src = Data1(listOf(null,123))
        val target = Data2(intArrayOf())
        SmartCopier.copy(src, target)
        println(target)
        assert(target.elements[0] == 0)
        assert(target.elements[1] == 123)
        target.elements = intArrayOf(456)
        SmartCopier.copy(target, src)
        assert(src.elements[0] == 456)
        println(src)
    }

    @Test
    fun testSetAndList(): Unit {
        data class Data1(
            var elements: Set<String>
        )

        data class Data2(
            var elements: List<String>
        )

        val src = Data1(setOf("123"))
        val target = Data2(listOf())
        SmartCopier.copy(src, target)
        println(target)
        assert(target.elements[0] == "123")
        target.elements = listOf("456")
        SmartCopier.copy(target, src)
        assert(src.elements.contains("456"))
        println(src)
    }

    companion object {

        private val SmartCopier = SmartCopier()

        @JvmStatic
        @BeforeAll
        fun init(): Unit {
            SmartCopier.debugMode = true
            SmartCopier.debugOutPutDir = "."
        }
    }
}
