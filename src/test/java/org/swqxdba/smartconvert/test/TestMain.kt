package org.swqxdba.smartconvert.test

import org.swqxdba.smartconvert.*
import java.lang.reflect.Method
import java.util.*

class Person {
    var name: String? = null
    var age: Long = 1
}

fun main() {
    SmartCopier.debugMode = true
    SmartCopier.debugOutPutDir = "."
    val config = CopyConfig(
        object : PropertyValueProvider {
            override fun provide(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass: Class<*>,
                copyMethodType: CopyMethodType
            ): Any? {
                if(sourceGetter.name.lowercase(Locale.getDefault()).contains("name")){
                    return "default_name!!!"
                }
                if(sourceGetter.name.lowercase().contains("age")){
                    return -1
                }
                return null
            }

        },
        object:PropertyValueConverter{
            override fun shouldIntercept(
                sourceGetter: Method,
                targetSetter: Method,
                sourceClass: Class<*>,
                targetClass: Class<*>,
                copyMethodType: CopyMethodType
            ): Boolean {
                return sourceGetter.name.lowercase(Locale.getDefault()).contains("name")
            }

            override fun convert(oldValue: Any?): Any? {
                return "intercepted_name!!!"
            }

        }
    )
    val newInstance = CopierGenerator(Person::class.java, Person::class.java,config).generateCopier()
    val person = Person()
    person.name = "personName"
    person.age = 666
    val person2 = Person()
    newInstance.copy(person, person2)
    println(person2.name)
    println(person2.age)
}