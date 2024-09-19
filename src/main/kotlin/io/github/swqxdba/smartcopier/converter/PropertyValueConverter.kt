package io.github.swqxdba.smartcopier.converter

interface PropertyValueConverter {
    /**
     * 转换值
     * @param oldValue 原始值 可能为null
     * @return 结果值 如果属性的类型为primitive类型 则不允许返回null
     * 注意这个方法名"convert"后续不要修改 因为它在生成字节码的时候被引用了
     */
    fun convert(oldValue: Any?): Any?
}