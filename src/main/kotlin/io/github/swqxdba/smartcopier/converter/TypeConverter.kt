package io.github.swqxdba.smartcopier.converter

import java.lang.reflect.Type

@FunctionalInterface
interface TypeConverter {
    fun doConvert(from: Any?):Any?
}

@FunctionalInterface
interface TypeConverterProvider {

    fun tryGetConverter(from: Type, to: Type):TypeConverter?

}

