package io.github.swqxdba.smartcopier.propertyreader

interface PropertyValueReader {
    /**
     * 读取属性值
     * @param src 源对象 (属性所在的对象)
     */
    fun readValue(src: Any?): Any?
}