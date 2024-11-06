package io.github.swqxdba.smartcopier

import io.github.swqxdba.smartcopier.converter.*
import io.github.swqxdba.smartcopier.propertyreader.CustomPropertyReaderProvider
import java.lang.reflect.Method
import java.lang.reflect.Type


/**
 * @param defaultValueProvider 默认值提供者
 * @param propertyValueConverterProviders 属性值转换器
 * @param propertyMapperRuleCustomizer 用于客制化属性的对应关系
 * @param propertyValueReaderProvider 用于客制化属性的读取方式
 * @param typeConverterProviders 类型转换器
 * @param propertyValueReaderProvider 属性值读取器
 */
class CopyConfig(
    var defaultValueProvider: DefaultValueProvider? = null,
    var propertyValueConverterProviders: MutableList<PropertyValueConverterProvider>? = null,
    var propertyMapperRuleCustomizer: PropertyMapperRuleCustomizer? = null,
    var propertyValueReaderProvider: CustomPropertyReaderProvider? = null,
    var typeConverterProviders: MutableList<TypeConverterProvider>? = mutableListOf()

) {

    init {
        typeConverterProviders?.add(ContainerTypeConverterProvider(this))
        typeConverterProviders?.add(BoxTypeConverterProvider.instance)
    }

    fun addConverter(vararg converter: PropertyValueConverterProvider) {
        var list = propertyValueConverterProviders
        if (list == null) {
            list = mutableListOf()
        }
        list.addAll(converter)
        propertyValueConverterProviders = list
    }

    fun addConverter(vararg converter: TypeConverterProvider) {
        var list = typeConverterProviders
        if (list == null) {
            list = mutableListOf()
        }
        list.addAll(converter)
        typeConverterProviders = list
    }




    fun findTypeConverter(from: Type, to: Type): TypeConverter? {
        return typeConverterProviders?.firstNotNullOfOrNull { it.tryGetConverter(from, to) }
    }

    fun findPropertyValueConverter(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        copyMethodType: CopyMethodType
    ): PropertyValueConverter? {
        return propertyValueConverterProviders?.firstNotNullOfOrNull {
            it.tryGetConverter(
                sourceGetter, targetSetter, sourceClass, targetClass, copyMethodType
            )
        }
    }

}
