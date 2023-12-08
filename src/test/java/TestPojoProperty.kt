import org.junit.jupiter.api.Test
import org.swqxdba.SmartUtil

private data class Data(
    var str: String? = null,
    var primitiveInt: Int = 0,
    var wrapInt: Int? = null,
    var wrapLong: Long? = null,
    var primitiveLong: Long = 0L,
    var intArray: IntArray,
    var wrapIntArray: Array<IntArray>,
    var stringArray: Array<String>,
    var commonList: List<*>,
    var genericList: List<String>
)

class TestPojoProperty {
    @Test
    fun baseTest() {
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


    }

}