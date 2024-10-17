package test

import io.github.swqxdba.smartcopier.SmartCopier
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SuperClassTest {
    open class Super {
        var superProperty: Int? = 0
        var isGood: Boolean = true
    }

    open class Son1 : Super() {
        var son1Property: Int? = 1
    }

    class Son2 : Son1() {
        var son2Property: Int = 2
    }

    @Test
    fun main() {
        val son2 = Son2()
        son2.superProperty = 9
        son2.son1Property = 7
        son2.isGood = false
        val smartCopier = SmartCopier()
        smartCopier.debugMode=true
        smartCopier.debugOutPutDir="./supers"

        val copied = Son2()
        smartCopier.copy(son2, copied)
        Assertions.assertTrue(son2.superProperty == copied.superProperty)
        Assertions.assertTrue(son2.son1Property == copied.son1Property)
        Assertions.assertTrue(son2.son2Property == copied.son2Property)
        Assertions.assertTrue(son2.isGood == copied.isGood)


    }
}