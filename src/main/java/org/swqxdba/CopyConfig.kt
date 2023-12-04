package org.swqxdba

import java.lang.reflect.Method



/**
 * 默认值提供者，当源头中数据为null时 将设置defaultValueProvider提供的默认值,defaultValueProvider对每个属性只会被调用一次
 */
interface PropertyValueProvider {

    /**
     * @param targetSetter 目标类该属性的setter方法
     * @param sourceGetter 源类该属性的getter方法
     * @param sourceClass 源类型
     * @param sourceClass 目标类型
     * @return 提供的默认值,可以是null 但是必须与targetSetter的参数兼容(可以赋值给该类型的变量)
     */
    fun provide(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>
    ): Any?
}

interface PropertyValueConverter {
    /**
     * 是否对该属性使用拦截
     * @param targetSetter 目标类该属性的setter方法
     * @param sourceGetter 源类该属性的getter方法
     * @param sourceClass 源类型
     * @param sourceClass 目标类型
     * @return 是否对该属性使用拦截
     */
    fun shouldIntercept(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>
    ): Boolean

    /**
     * 转换值
     * @param oldValue 原始值 可能为null
     * @return 结果值
     */
    fun convert(oldValue: Any?): Any?
}

/**
 * 用于客制化属性之间的对应关系
 */
interface PropertyMapperRuleCustomizer {

    /**
     * 客制化映射规则，会对返回的规则进行检测，如果map.key不是属于sourceClass的方法，或者map.key不是属于targetClass的方法会抛出异常。
     * @param 源类型
     * @param 目标类型
     * @param currentMapper 通过默认规则解析出来的映射关系
     * @return 新的映射关系
     */
    fun mapperRule(
        sourceClass: Class<*>,
        targetClass: Class<*>,
        currentMapper: Map<Method, Method>
    ): Map<Method, Method>
}

/**
 * @param defaultValueProvider 默认值提供者
 * @param propertyValueConverter 属性值转换器
 * @param propertyMapperRuleCustomizer 用于客制化属性的对应关系
 */
class CopyConfig(
    var defaultValueProvider: PropertyValueProvider? = null,
    var propertyValueConverter: PropertyValueConverter? = null,
    var propertyMapperRuleCustomizer: PropertyMapperRuleCustomizer? = null,
    var incompatibleTypesOption: IncompatibleTypesOption = IncompatibleTypesOption.IGNORE
)
enum class IncompatibleTypesOption{

    IGNORE,//忽略拷贝不兼容的属性
    CAST//执行强制转换
}