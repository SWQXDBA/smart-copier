import cn.hutool.core.bean.BeanUtil
import org.swqxdba.*
import java.lang.reflect.Method
import java.util.*


data class Person(var name: String, var age: Int, var sex: String)
data class Animal(var name: String, var age: Int)

fun main(args: Array<String>) {
    val copier = SmartCopier.getCopier(Person::class.java, Animal::class.java, CopyConfig(
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

        },
        replaceWithNull = false

    )
    )
    val person = Person("name", 15, "男")
    val animal = Animal("name2", 14)
    run {

        val start = System.currentTimeMillis()
        repeat(1000000) {
            copier.copy(person, animal)
        }
        println("use time ${System.currentTimeMillis() - start}")
    }

    run {
        val start = System.currentTimeMillis()
        repeat(1000000) {
            BeanUtil.copyProperties(person, animal)
        }
        println("use time ${System.currentTimeMillis() - start}")
    }
    run {
        val start = System.currentTimeMillis()
        repeat(1000000) {
            BeanUtil.copyProperties(person, animal)
        }
        println("use time ${System.currentTimeMillis() - start}")
    }
}


