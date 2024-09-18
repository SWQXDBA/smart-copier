package io.github.swqxdba.smartcopier.typeconverter

import java.lang.reflect.Type

@FunctionalInterface
interface TypeConverter {

    fun shouldConvert(from: Type, to: Type):Boolean

    fun doConvert(from: Any?):Any?
}