package io.github.swqxdba.smartcopier.converter

import io.github.swqxdba.smartcopier.CopyMethodType
import java.lang.reflect.Method

interface PropertyValueConverterProvider {
    /**
     * 是否对该属性使用拦截
     * @param targetSetter 目标类该属性的setter方法
     * @param sourceGetter 源类该属性的getter方法
     * @param sourceClass 源类型
     * @param sourceClass 目标类型
     * @param copyMethodType 拷贝方法
     * @return 是否对该属性使用拦截
     */
    fun tryGetConverter(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        copyMethodType: CopyMethodType
    ): PropertyValueConverter?
}