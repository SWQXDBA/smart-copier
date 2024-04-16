package io.github.swqxdba.smartcopier.bean
@FunctionalInterface
interface BeanConvertProvider {
    /**
     * 尝试获取转换器 如果返回null表示不支持。
     */
    fun tryGetConverter(from: Class<*>, to: Class<*>):BeanConverter?
}
@FunctionalInterface
interface BeanConverter {
    /**
     * 转换bean，调用时 保证不传入null，且必须保证该方法的返回值不为null
     */
    fun doConvert(from: Any):Any
}