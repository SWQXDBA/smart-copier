package io.github.swqxdba.smartcopier.propertyreader

import java.lang.reflect.Method

/**
 * 自定义属性值读取器判断器
 */
interface CustomPropertyReaderProvider {
    fun tryGerReader(getterMethod:Method): PropertyValueReader?
}