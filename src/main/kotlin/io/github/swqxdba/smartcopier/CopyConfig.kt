package io.github.swqxdba.smartcopier

import io.github.swqxdba.smartcopier.converter.*
import io.github.swqxdba.smartcopier.propertyreader.CustomPropertyReaderProvider
import java.lang.reflect.Method
import java.lang.reflect.Type


/**
 * @param defaultValueProvider 默认值提供者
 * @param propertyValueConverters 属性值转换器
 * @param propertyMapperRuleCustomizer 用于客制化属性的对应关系
 * @param propertyValueReaderProvider 用于客制化属性的读取方式
 * @param typeConverters 类型转换器
 * @param propertyValueReaderProvider 属性值读取器
 * @param allowPrimitiveWrapperAutoCast 是否允许自动拆箱和装箱
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


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CopyConfig) return false

        if (defaultValueProvider != other.defaultValueProvider) return false
        if (propertyValueConverterProviders != other.propertyValueConverterProviders) return false
        if (propertyMapperRuleCustomizer != other.propertyMapperRuleCustomizer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultValueProvider?.hashCode() ?: 0
        result = 31 * result + (propertyValueConverterProviders?.hashCode() ?: 0)
        result = 31 * result + (propertyMapperRuleCustomizer?.hashCode() ?: 0)
        return result
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
