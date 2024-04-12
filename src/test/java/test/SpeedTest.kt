package test

import cn.hutool.core.bean.BeanUtil
import org.junit.jupiter.api.Test
import org.swqxdba.smartconvert.SmartCopier

class SpeedTest {

    class Data(
        var field1: String? = null,
        var field2: String? = null,
        var field3: String? = null,
        var field4: String? = null,
        var field5: String? = null,
        var field6: String? = null,
        var field7: String? = null,
        var field8: String? = null,
        var field9: List<Int>? = arrayListOf(1, 2, 3, 4, 5, 67, 1, 4, 1, 2)

    )

    //用于预热
    class HotData(val field1: String?)

    fun hardCodeCopy(src: Data?, target: Data?) {
        if (src != null && target != null) {
            target.field1 = src.field1
            target.field2 = src.field2
            target.field3 = src.field3
            target.field4 = src.field4
            target.field5 = src.field5
            target.field6 = src.field6
            target.field7 = src.field7
            target.field8 = src.field8
            src.field9?.let {
                val field9 = arrayListOf<Int>()
                it.forEach { field9.add(it) }
                target.field9 = field9
            }
        }
    }

    fun testTime(testName: String, block: () -> Unit) {
        val start = System.currentTimeMillis()
        block()
        println("$testName use ${System.currentTimeMillis() - start} mills")
    }

    @Test
    fun test() {
        SmartCopier.getCopier(HotData::class.java, HotData::class.java)//预热

        val testCount = 1_000_00
        val cur = System.currentTimeMillis()
        val copier = SmartCopier.getCopier(Data::class.java, Data::class.java)
        println("generate copier use ${System.currentTimeMillis() - cur} mills")

        val src = Data("1", "2")
        var target = Data()


        repeat(2) { cnt ->
            testTime("hard code copy") {
                src.field3 = "" + cnt
                repeat(testCount) {
                    hardCodeCopy(src, target)
                }
            }
        }

        repeat(2) { cnt ->
            testTime("smart copier") {
                src.field3 = "" + cnt
                repeat(testCount) {
                    copier.copy(src, target)
                }
            }
        }

        repeat(2) { cnt ->
            testTime("static smart copier") {
                repeat(testCount) {
                    src.field3 = "" + cnt
                    SmartCopier.copy(src, target)
                }
            }
        }


        repeat(2) { cnt ->
            testTime("BeanUtil") {
                repeat(testCount) {
                    src.field3 = "" + cnt
                    BeanUtil.copyProperties(src, target)
                }
            }
        }

    }

    @Test
    fun test2() {
        SmartCopier.getCopier(HotData::class.java, HotData::class.java)//预热

        val testCount = 100_000_00
        val cur = System.currentTimeMillis()
        val copier = SmartCopier.getCopier(Data::class.java, Data::class.java)
        println("generate copier use ${System.currentTimeMillis() - cur} mills")

        val src = Data("1", "2")
        var target = Data()


        repeat(2) {
            testTime("hard code copy") {
                repeat(testCount) {
                    hardCodeCopy(src, target)
                }
            }
        }

        repeat(2) {
            testTime("smart copier") {
                repeat(testCount) {
                    copier.copy(src, target)
                }
            }
        }


    }
}