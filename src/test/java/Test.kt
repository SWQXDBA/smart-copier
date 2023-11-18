import cn.hutool.core.bean.BeanUtil
import org.swqxdba.*
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap


data class Person(var name: String, var age: Int, var sex: String)
data class Animal(var name: String, var age: Int)

fun main(args: Array<String>) {


    val testCount = 10000000

    val config = CopyConfig(
        defaultValueProvider = object : PropertyValueProvider {
            override fun provide(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass: Class<*>
            ): Any? {
                if (targetSetter.name.lowercase(Locale.getDefault()).contains("name")) {
                    return ""
                } else if (targetSetter.name.lowercase(Locale.getDefault()).contains("ane")) {
                    return 1
                }
                return null
            }

        },
        propertyValueConverter = object : PropertyValueConverter {
            override fun shouldIntercept(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass: Class<*>
            ): Boolean {
                return targetSetter.name.lowercase(Locale.getDefault()).contains("name")
            }

            override fun convert(oldValue: Any?): Any? {
                return "new " + oldValue
            }

        },
        propertyMapperRuleCustomizer = object : PropertyMapperRuleCustomizer {
            override fun mapperRule(
                sourceClass: Class<*>,
                targetClass: Class<*>,
                currentMapper: Map<Method, Method>
            ): Map<Method, Method> {
                val newMap = currentMapper.toMutableMap()
                newMap[Person::class.java.getMethod("getSex")] =
                    Animal::class.java.getMethod("setName", String::class.java)

                return newMap
            }

        }

    )
    val copier = SmartCopier.getCopier(
        Person::class.java, Animal::class.java, null
    )
    val person = Person("name", 15, "男")
    val animal = Animal("name2", 14)
    repeat(2) {

        val start = System.currentTimeMillis()
        repeat(testCount) {
            copier.copy(person, animal)
        }
        println("smart copier use time ${System.currentTimeMillis() - start}")
    }


    repeat(2) {
        val start = System.currentTimeMillis()
        repeat(testCount) {
            BeanUtil.copyProperties(person, animal)
        }
        println("BeanUtil time ${System.currentTimeMillis() - start}")
    }
}


fun main() {
    val name: String? = null
    if (name != null) {
        println(name.toString());
    }
}