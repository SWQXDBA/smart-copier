package test.benchmark

import cn.hutool.core.bean.BeanUtil
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.swqxdba.smartcopier.CopyConfig
import io.github.swqxdba.smartcopier.SmartCopier
import io.github.swqxdba.smartcopier.converter.PackageBasedTypeConverterProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.BeanUtils
import kotlin.collections.plus

class Benchmark {
    class TestStructTo {
        var field1: Int = 0
        var field2: Long = 0
        var field3: String? = null
        var field4: Double = 0.0
        var field5: Boolean = false
        var field6: Float = 0.0f
        var field7: Short = 0
        var field8: Byte = 0
        var field9: Char = '0'
        var field10: List<Int>? = null
    }

    class TestStructFrom {
        var field1: Int = 100
        var field2: Long = 200L
        var field3: String = "测试字符串"
        var field4: Double = 3.14159
        var field5: Boolean = true
        var field6: Float = 2.718f
        var field7: Short = 500
        var field8: Byte = 127
        var field9: Char = 'A'
        var field10: List<Int> =
            listOf(1, 2, 3, 4, 5).let { it + it }.let { it + it }.let { it + it }.let { it + it }.let { it + it }
    }

    val smartCopier = SmartCopier()
        .apply {
            debugMode = true
            debugOutPutDir = "./benchmarkdebug2"
        }
    val copier = smartCopier.getCopier(TestStructFrom::class.java, TestStructTo::class.java,)

    private fun useSmartCopier(source: TestStructFrom): TestStructTo {
        val target = TestStructTo()
        copier.copy(source, target)
        return target
    }

    private fun useBeanUtils(source: TestStructFrom): TestStructTo {
        val target = TestStructTo()
        BeanUtil.copyProperties(source, target)
        return target
    }

    private fun useSpringBeanUtils(source: TestStructFrom): TestStructTo {
        val target = TestStructTo()
        BeanUtils.copyProperties(source, target)
        return target
    }

    val mapper = ObjectMapper()
    private fun useObjectMapper(source: TestStructFrom): TestStructTo {
        return mapper.convertValue(source, TestStructTo::class.java)
    }

    @Test
    fun test() {
        println("开始性能基准测试...")
        
        val source = TestStructFrom()
        val warmupRounds = 10000  // 预热轮数
        val testRounds = 500000   // 正式测试轮数
        val iterations = 5        // 测试迭代次数
        
        println("测试数据规模:")
        println("- 基本字段: 10个")
        println("- List大小: ${source.field10.size}")
        println("- 预热轮数: $warmupRounds")
        println("- 测试轮数: $testRounds")
        println("- 迭代次数: $iterations")
        println()
        
        // 1. SmartCopier 测试
        println("=== SmartCopier 性能测试 ===")
        val smartCopierTimes = mutableListOf<Long>()
        
        // JVM预热
        println("JVM预热中...")
        repeat(warmupRounds) {
            useSmartCopier(source)
        }
        
        // 正式测试
        repeat(iterations) { iteration ->
            System.gc() // 建议垃圾回收
            Thread.sleep(100) // 短暂休息
            
            val startTime = System.nanoTime()
            repeat(testRounds) {
                useSmartCopier(source)
            }
            val endTime = System.nanoTime()
            val duration = endTime - startTime
            smartCopierTimes.add(duration)
            
            println("第${iteration + 1}轮: ${duration / 1_000_000}ms (${duration / testRounds}ns/op)")
        }
        
        // 2. Spring BeanUtils 测试
        println("\n=== Spring BeanUtils 性能测试 ===")
        val springBeanUtilsTimes = mutableListOf<Long>()
        
        // JVM预热
        println("JVM预热中...")
        repeat(warmupRounds) {
            useSpringBeanUtils(source)
        }
        
        // 正式测试
        repeat(iterations) { iteration ->
            System.gc()
            Thread.sleep(100)
            
            val startTime = System.nanoTime()
            repeat(testRounds) {
                useSpringBeanUtils(source)
            }
            val endTime = System.nanoTime()
            val duration = endTime - startTime
            springBeanUtilsTimes.add(duration)
            
            println("第${iteration + 1}轮: ${duration / 1_000_000}ms (${duration / testRounds}ns/op)")
        }
        
        // 3. BeanUtils 测试
        println("\n=== BeanUtils 性能测试 ===")
        val beanUtilsTimes = mutableListOf<Long>()
        
        // JVM预热
        println("JVM预热中...")
        repeat(warmupRounds) {
            useBeanUtils(source)
        }
        
        // 正式测试
        repeat(iterations) { iteration ->
            System.gc()
            Thread.sleep(100)
            
            val startTime = System.nanoTime()
            repeat(testRounds) {
                useBeanUtils(source)
            }
            val endTime = System.nanoTime()
            val duration = endTime - startTime
            beanUtilsTimes.add(duration)
            
            println("第${iteration + 1}轮: ${duration / 1_000_000}ms (${duration / testRounds}ns/op)")
        }
        
        // 4. ObjectMapper 测试
        println("\n=== ObjectMapper 性能测试 ===")
        val objectMapperTimes = mutableListOf<Long>()
        
        // JVM预热
        println("JVM预热中...")
        repeat(warmupRounds) {
            useObjectMapper(source)
        }
        
        // 正式测试
        repeat(iterations) { iteration ->
            System.gc()
            Thread.sleep(100)
            
            val startTime = System.nanoTime()
            repeat(testRounds) {
                useObjectMapper(source)
            }
            val endTime = System.nanoTime()
            val duration = endTime - startTime
            objectMapperTimes.add(duration)
            
            println("第${iteration + 1}轮: ${duration / 1_000_000}ms (${duration / testRounds}ns/op)")
        }
        
        // 统计结果
        println("\n" + "=".repeat(50))
        println("性能测试结果汇总")
        println("=".repeat(50))
        
        fun printStats(name: String, times: List<Long>) {
            val avgTime = times.average()
            val minTime = times.minOrNull() ?: 0L
            val maxTime = times.maxOrNull() ?: 0L
            val avgPerOp = avgTime / testRounds
            
            println("$name:")
            println("  平均耗时: ${String.format("%.2f", avgTime / 1_000_000)}ms")
            println("  最短耗时: ${minTime / 1_000_000}ms")
            println("  最长耗时: ${maxTime / 1_000_000}ms")
            println("  平均单次操作: ${String.format("%.2f", avgPerOp)}ns")
            println("  吞吐量: ${String.format("%.0f", testRounds * 1_000_000_000.0 / avgTime)} ops/s")
            println()
        }
        
        printStats("SmartCopier", smartCopierTimes)
        printStats("Spring BeanUtils", springBeanUtilsTimes)
        printStats("BeanUtils", beanUtilsTimes)
        printStats("ObjectMapper", objectMapperTimes)
        
        // 性能对比
        val smartCopierAvg = smartCopierTimes.average()
        val springBeanUtilsAvg = springBeanUtilsTimes.average()
        val beanUtilsAvg = beanUtilsTimes.average()
        val objectMapperAvg = objectMapperTimes.average()
        
        println("性能对比 (以SmartCopier为基准):")
        println("SmartCopier:  1.00x")
        println("Spring BeanUtils:    ${String.format("%.2f", springBeanUtilsAvg / smartCopierAvg)}x")
        println("BeanUtils:    ${String.format("%.2f", beanUtilsAvg / smartCopierAvg)}x")
        println("ObjectMapper: ${String.format("%.2f", objectMapperAvg / smartCopierAvg)}x")
        
        println("\n测试完成!")
    }
}