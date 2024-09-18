package io.github.swqxdba.smartcopier.converter

import io.github.swqxdba.smartcopier.CopyMethodType
import java.lang.reflect.Method


/**
 * 默认值提供者，当源头中数据为null时 将设置defaultValueProvider提供的默认值,defaultValueProvider对每个属性只会被调用一次
 * 注意 如果属性类型是primitive的 则永远不会使用默认值!
 */
interface DefaultValueProvider {

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