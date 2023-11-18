package org.swqxdba;

public interface Copier {
    /**
     * 强制对发现的所有属性进行拷贝
     * @param src
     * @param target
     */
    void copy(Object src,Object target);

    /**
     * 当源属性不为null时进行拷贝
     * @param src
     * @param target
     */
    void copyNonNullProperties(Object src,Object target);

    /**
     * 合并两个对象，只对目标对象中为null的属性会被更新
     * @param src
     * @param target
     */
    void merge(Object src,Object target);
}
