package org.swqxdba.smartconvert


interface Copier {
    /**
     * 强制对发现的所有属性进行拷贝
     * @param src
     * @param target
     */
    fun copy(src: Any?, target: Any?)

    /**
     * 当源属性不为null时进行拷贝
     * 如果源属性为null 但是经过了转换后的目标属性不为null 也会执行更新。
     * 如果源属性为null 但是提供了一个非null的默认值，也可能会执行更新
     * @param src
     * @param target
     */
    fun copyNonNullProperties(src: Any?, target: Any?)

    /**
     * 合并两个对象，只对目标对象中为null的属性会被更新
     * 如果某个属性为primitive的 则不会再被merge更新
     * @param src
     * @param target
     */
    fun merge(src: Any?, target: Any?)
}

