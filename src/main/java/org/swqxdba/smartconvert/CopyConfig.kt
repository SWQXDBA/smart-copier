package org.swqxdba.smartconvert

import java.lang.reflect.Method



/**
 * 默认值提供者，当源头中数据为null时 将设置defaultValueProvider提供的默认值,defaultValueProvider对每个属性只会被调用一次
 * 注意 如果属性类型是primitive的 则永远不会使用默认值!
 */
interface PropertyValueProvider {

    /**
     *
     * 用于提供默认值 如果提供的默认值不为null 则会被赋值给目标类的该属性
     *
     * 对于copy,copyNonNullProperties和merge方法 可以提供不同的默认值，不会互相影响,
     *
     * 比如在copyNonNullProperties时可以全部提供null表示不使用默认值。
     *
     * @param targetSetter 目标类该属性的setter方法
     * @param sourceGetter 源类该属性的getter方法
     * @param sourceClass 源类型
     * @param sourceClass 目标类型
     * @param copyMethodType 拷贝方法
     * @return 提供的默认值,可以是null 但是必须与targetSetter的参数兼容(可以赋值给该类型的变量)
     */
    fun provide(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        copyMethodType: CopyMethodType
    ): Any?
}

interface PropertyValueConverter {
    /**
     * 是否对该属性使用拦截
     * @param targetSetter 目标类该属性的setter方法
     * @param sourceGetter 源类该属性的getter方法
     * @param sourceClass 源类型
     * @param sourceClass 目标类型
     * @param copyMethodType 拷贝方法
     * @return 是否对该属性使用拦截
     */
    fun shouldIntercept(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        copyMethodType: CopyMethodType
    ): Boolean

    /**
     * 转换值
     * @param oldValue 原始值 可能为null
     * @return 结果值 如果属性的类型为primitive类型 则不允许返回null
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
)
