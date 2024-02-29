package org.swqxdba.smartconvert.container

import org.swqxdba.smartconvert.InternalUtil
import org.swqxdba.smartconvert.InternalUtil.findCollectionConstructor
import java.lang.RuntimeException
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.HashSet


class IterWrapper(type: Type) {

    val sizeGetter: (Any) -> Int

    val instanceCreator: (Int) -> Any

    val fillExecutor: (Any, Collection<Any?>) -> Unit

    val elementsResolver: (Any) -> Collection<Any>

    init {

        val containerClass = InternalUtil.getOuterClass(type) ?: throw RuntimeException("can not resolve type $type")
        if (containerClass.isArray) {
            sizeGetter = {
                java.lang.reflect.Array.getLength(it)
            }
            instanceCreator = { size ->
                java.lang.reflect.Array.newInstance(containerClass.componentType, size)
            }
            //如果是primitive的数组 要保证设置进去的元素不为null
            if(containerClass.componentType.isPrimitive){
                fillExecutor = { arr, elements ->
                    var index = 0
                    for (element in elements) {
                        val idx = index++
                        if(element!=null){
                            java.lang.reflect.Array.set(arr,idx,element)
                        }
                    }
                }
            }else{
                fillExecutor = { arr, elements ->
                    var index = 0
                    for (element in elements) {
                        java.lang.reflect.Array.set(arr,index++,element)
                    }
                }
            }

            elementsResolver = { arr ->
               val len =  java.lang.reflect.Array.getLength(arr)

                val list = mutableListOf<Any>()
                for (i in 0..<len) {
                    list.add(java.lang.reflect.Array.get(arr,i))
                }
                list
            }

        } else {
            if (!Collection::class.java.isAssignableFrom(containerClass)) {
                throw RuntimeException("cannot resolve type $containerClass")
            }
            sizeGetter = {
                val c = it as Collection<*>
                c.size
            }
            val modifiers = containerClass.modifiers
            val canNewDirect = !(Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers))
            instanceCreator = if (canNewDirect) {
                {size -> findCollectionConstructor(containerClass)(size) }
            } else {
                if (containerClass.isAssignableFrom(ArrayList::class.java)) {
                    { ArrayList(it) }
                } else if (containerClass.isAssignableFrom(HashSet::class.java)) {
                    { HashSet<Any>(it) }
                } else {
                    throw RuntimeException("cannot resolve type $containerClass")
                }
            }

            fillExecutor = { arr, elements ->
                (arr as java.util.Collection<Any>).addAll(elements)
            }
            elementsResolver = { arr ->
                arr as Collection<Any>
            }
        }
    }

    companion object {

        private val canWrapList = listOf<Class<*>>(
            Collection::class.java,
            Array::class.java,
        )

        fun canWrap(type: Type): Boolean {
            if (type is Class<*>) {
                if (canWrapList.any {
                        it.isAssignableFrom(type)
                    }) {
                    return true
                }
                return type.isArray
            } else {
                val outerClass = InternalUtil.getOuterClass(type) ?: return false
                return canWrap(outerClass)
            }

        }
    }
}

fun main() {
    println(IterWrapper.canWrap(List::class.java))
    println(IterWrapper.canWrap(IntArray::class.java))
    println(IterWrapper.canWrap(arrayOf(15, null).javaClass))


}