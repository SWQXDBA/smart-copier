package test

import org.junit.jupiter.api.Test
import io.github.swqxdba.smartcopier.*
import java.lang.reflect.Method


class TestPojoProperty {

    private val SmartCopier = SmartCopier()

    data class Data(
        var str: String? = null,
        var primitiveInt: Int = 0,
        var wrapInt: Int? = null,
        var wrapLong: Long? = null,
        var primitiveLong: Long = 0L,
        var intArray: IntArray = intArrayOf(),
        var wrapIntArray: Array<IntArray> = arrayOf(),
        var stringArray: Array<String> = arrayOf(),
        var commonList: List<*>? = null,
        var genericList: List<String>? = null

    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Data

            if (str != other.str) return false
            if (primitiveInt != other.primitiveInt) return false
            if (wrapInt != other.wrapInt) return false
            if (wrapLong != other.wrapLong) return false
            if (primitiveLong != other.primitiveLong) return false
            if (!intArray.contentEquals(other.intArray)) return false
            if (!wrapIntArray.contentDeepEquals(other.wrapIntArray)) return false
            if (!stringArray.contentEquals(other.stringArray)) return false
            if (commonList != other.commonList) return false
            if (genericList != other.genericList) return false

            return true
        }

        override fun hashCode(): Int {
            var result = str?.hashCode() ?: 0
            result = 31 * result + primitiveInt
            result = 31 * result + (wrapInt ?: 0)
            result = 31 * result + (wrapLong?.hashCode() ?: 0)
            result = 31 * result + primitiveLong.hashCode()
            result = 31 * result + intArray.contentHashCode()
            result = 31 * result + wrapIntArray.contentDeepHashCode()
            result = 31 * result + stringArray.contentHashCode()
            result = 31 * result + (commonList?.hashCode() ?: 0)
            result = 31 * result + (genericList?.hashCode() ?: 0)
            return result
        }
    }

    val data = Data(
        "str",
        1,
        null,
        2L,
        3L,
        intArrayOf(3, 1, 2),
        arrayOf(intArrayOf(3, 1, 2), intArrayOf(4, 5, 6)),
        arrayOf("stringarray"),
        listOf(1, "666"),
        listOf("stringlist")
    )

    @Test
    fun baseCopyTest() {

        SmartCopier.debugMode = true
        SmartCopier.debugOutPutDir = "."
        val copier = SmartCopier.getCopier(Data::class.java, Data::class.java)
        val target = Data()
        copier.copy(data, target)

        assert(data == target)

    }

    @Test
    fun baseCopyNonNullTest() {

        val copier = SmartCopier.getCopier(Data::class.java, Data::class.java)
        val target = Data(wrapInt = 1, intArray = intArrayOf(1, 2, 3))
        copier.copyNonNullProperties(data, target)

        assert(target.wrapInt == 1)//没有被null覆盖
        assert(target.intArray.contentEquals(data.intArray))//源值不为null，所以目标值被覆盖了

    }

    @Test
    fun baseMergeTest() {

        val copier = SmartCopier.getCopier(Data::class.java, Data::class.java)
        val target = Data(wrapInt = 999, intArray = intArrayOf(1, 2, 3), genericList = null)
        copier.merge(data, target)

        assert(target.wrapInt == 999)//没有被覆盖
        assert(target.intArray.contentEquals(intArrayOf(1, 2, 3)))//没有被覆盖
        assert(target.genericList == data.genericList)//target中的值为null所以被覆盖了

    }

    @Test
    fun converterTest() {
        val data = Data(
            "str",
            1,
            null,
            2L,
            3L,
            intArrayOf(3, 1, 2),
            arrayOf(intArrayOf(3, 1, 2), intArrayOf(4, 5, 6)),
            arrayOf("stringarray"),
            listOf(1, "666"),
            listOf("stringlist")
        )
        val config = CopyConfig()
        val converter1 = object : PropertyValueConverter {
            override fun shouldIntercept(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass: Class<*>,
                copyMethodType: CopyMethodType
            ): Boolean {
                if (targetSetter.name.lowercase().contains("wraplong")) {
                    return true
                }
                return false
            }

            override fun convert(oldValue: Any?): Any? {
                return -1L
            }

        }
        val converter2 = object : PropertyValueConverter {
            override fun shouldIntercept(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass: Class<*>,
                copyMethodType: CopyMethodType
            ): Boolean {
                if (targetSetter.name == "setStr") {
                    return true
                }
                return false
            }

            override fun convert(oldValue: Any?): Any? {
                return "converted str!!!"
            }

        }
        config.addConverter(converter1, converter2)
        SmartCopier.debugMode = true
        SmartCopier.debugOutPutDir = "."
        val copier = SmartCopier.getCopier(Data::class.java, Data::class.java, config)
        val target = Data()
        copier.copy(data, target)

        assert(target.wrapLong == -1L)
        assert(target.str == "converted str!!!")
    }

    @Test
    fun defaultValueTest() {
        val data = Data(
            wrapInt = 1
        )

        val config = CopyConfig()
        val defaultValueProvider = object : PropertyValueProvider {
            override fun provide(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass: Class<*>,
                copyMethodType: CopyMethodType
            ): Any? {
                if(sourceGetter.name == "getStr"){
                    return "default str"
                }
                if(sourceGetter.name == "getWrapInt"){
                    return 12
                }
                return null
            }
        }

        config.defaultValueProvider = defaultValueProvider
        SmartCopier.debugMode = true
        SmartCopier.debugOutPutDir = "."
        val copier = SmartCopier.getCopier(Data::class.java, Data::class.java, config)
        val target = Data()
        copier.copy(data, target)

        assert(target.str == "default str")//没有这个值 所以使用了默认值
        assert(target.wrapInt == 1)//已经有值 不需要默认值
    }
}