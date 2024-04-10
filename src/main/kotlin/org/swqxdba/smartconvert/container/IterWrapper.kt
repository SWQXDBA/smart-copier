package org.swqxdba.smartconvert.container

import org.swqxdba.smartconvert.InternalUtil
import org.swqxdba.smartconvert.InternalUtil.findCollectionConstructor
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*


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
            instanceCreator = createFasterArrayInstanceCreator(containerClass.componentType)
            //如果是primitive的数组 要保证设置进去的元素不为null
            if (containerClass.componentType.isPrimitive) {
                fillExecutor = getPrimitiveFillExecutor(containerClass.componentType)
                elementsResolver = getPrimitiveElementsResolver(containerClass.componentType)
            } else {
                fillExecutor = { arr, elements ->
                    val array = arr as Array<Any>
                    var index = 0
                    for (element in elements as Collection<Any>) {
                        array[index++] = element
                    }
                }
                elementsResolver = { arr ->
                    asFastIterList(arr as Array<Any>)
                }
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
                { size -> findCollectionConstructor(containerClass)(size) }
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

/**
 * 用于构建一个快速的可遍历的集合
 */
private fun asFastIterList(arr: Array<Any>): List<Any> {
    val len: Int = arr.size
    return object : AbstractList<Any>() {

        override fun get(index: Int): Any {
            return arr[index]
        }

        override val size: Int = len

        override fun iterator(): MutableIterator<Any> {
            return object : MutableIterator<Any> {
                var cursor: Int = 0

                override fun hasNext(): Boolean {
                    return cursor != len
                }

                override fun next(): Any {
                    return try {
                        arr[cursor++]
                    } catch (e: IndexOutOfBoundsException) {
                        throw NoSuchElementException()
                    }
                }

                override fun remove() {
                    throw UnsupportedOperationException()
                }

            }
        }
    }
}

private fun getPrimitiveFillExecutor(contentType: Type): (Any, Collection<Any?>) -> Unit {
    if (contentType !is Class<*> || !contentType.isPrimitive) {
        throw RuntimeException("internal error! $contentType must be primitive")
    }
    when (contentType) {
        Byte::class.java -> {
            return { arr, elements ->
                val array = arr as ByteArray
                for ((index, element) in elements.withIndex()) {
                    val idx = index
                    if (element != null) {
                        array[idx] = element as Byte
                    }
                }
            }
        }

        Short::class.java -> {
            return { arr, elements ->
                val array = arr as ShortArray
                for ((index, element) in elements.withIndex()) {
                    val idx = index
                    if (element != null) {
                        array[idx] = element as Short
                    }
                }
            }
        }

        Int::class.java -> {
            return { arr, elements ->
                val array = arr as IntArray
                for ((index, element) in elements.withIndex()) {
                    val idx = index
                    if (element != null) {
                        array[idx] = element as Int
                    }
                }
            }
        }

        Long::class.java -> {
            return { arr, elements ->
                val array = arr as LongArray
                for ((index, element) in elements.withIndex()) {
                    val idx = index
                    if (element != null) {
                        array[idx] = element as Long
                    }
                }
            }
        }

        Float::class.java -> {
            return { arr, elements ->
                val array = arr as FloatArray
                for ((index, element) in elements.withIndex()) {
                    val idx = index
                    if (element != null) {
                        array[idx] = element as Float
                    }
                }
            }
        }

        Double::class.java -> {
            return { arr, elements ->
                val array = arr as DoubleArray
                for ((index, element) in elements.withIndex()) {
                    val idx = index
                    if (element != null) {
                        array[idx] = element as Double
                    }
                }
            }
        }

        Char::class.java -> {
            return { arr, elements ->
                val array = arr as CharArray
                for ((index, element) in elements.withIndex()) {
                    val idx = index
                    if (element != null) {
                        array[idx] = element as Char
                    }
                }
            }
        }

        Boolean::class.java -> {
            return { arr, elements ->
                val array = arr as BooleanArray
                for ((index, element) in elements.withIndex()) {
                    val idx = index
                    if (element != null) {
                        array[idx] = element as Boolean
                    }
                }
            }
        }

        else -> {
            throw RuntimeException("Unsupported primitive type: ${contentType.name}")
        }
    }
}

private fun getPrimitiveElementsResolver(contentType: Type): (Any) -> List<Any> {
    if (contentType !is Class<*> || !contentType.isPrimitive) {
        throw RuntimeException("internal error! $contentType must be primitive")
    }
    when (contentType) {
        Byte::class.java -> {
            return { arr ->
                val array = arr as ByteArray
                array.map { it }
            }
        }

        Short::class.java -> {
            return { arr ->
                val array = arr as ShortArray
                array.map { it }
            }
        }

        Int::class.java -> {
            return { arr ->
                val array = arr as IntArray
                array.map { it }
            }
        }

        Long::class.java -> {
            return { arr ->
                val array = arr as LongArray
                array.map { it }
            }
        }

        Float::class.java -> {
            return { arr ->
                val array = arr as FloatArray
                array.map { it }
            }
        }

        Double::class.java -> {
            return { arr ->
                val array = arr as DoubleArray
                array.map { it }
            }
        }

        Char::class.java -> {
            return { arr ->
                val array = arr as CharArray
                array.map { it }
            }
        }

        Boolean::class.java -> {
            return { arr ->
                val array = arr as BooleanArray
                array.map { it }
            }
        }

        else -> {
            throw RuntimeException("Unsupported primitive type: ${contentType.name}")
        }
    }
}

private fun createFasterArrayInstanceCreator(contentType: Type): (Int) -> Any {
    if (contentType !is Class<*>) {
        throw RuntimeException("internal error! $contentType must be Class")
    }
    if (!contentType.isPrimitive) {
        return { size ->
            when (contentType) {
                String::class.java -> {
                    arrayOfNulls<String?>(size)
                }

                Date::class.java -> {
                    arrayOfNulls<Int?>(size)
                }

                Int::class.javaObjectType -> {
                    arrayOfNulls<Int?>(size)
                }

                Long::class.javaObjectType -> {
                    arrayOfNulls<Long?>(size)
                }

                Float::class.javaObjectType -> {
                    arrayOfNulls<Float?>(size)
                }

                Double::class.javaObjectType -> {
                    arrayOfNulls<Double?>(size)
                }

                Boolean::class.javaObjectType -> {
                    arrayOfNulls<Boolean?>(size)
                }

                Char::class.javaObjectType -> {
                    arrayOfNulls<Char?>(size)
                }

                Byte::class.javaObjectType -> {
                    arrayOfNulls<Byte?>(size)
                }

                Short::class.javaObjectType -> {
                    arrayOfNulls<Short?>(size)
                }

                BigDecimal::class.java -> {
                    arrayOfNulls<BigDecimal?>(size)
                }

                BigInteger::class.java -> {
                    arrayOfNulls<BigInteger?>(size)
                }

                else -> {
                    java.lang.reflect.Array.newInstance(contentType, size)
                }
            }

        }
    } else {
        when (contentType) {
            Byte::class.java -> {
                return { size ->
                    ByteArray(size)
                }
            }

            Short::class.java -> {
                return { size ->
                    ShortArray(size)
                }
            }

            Int::class.java -> {
                return { size ->
                    IntArray(size)
                }
            }

            Long::class.java -> {
                return { size ->
                    LongArray(size)
                }
            }

            Float::class.java -> {
                return { size ->
                    FloatArray(size)
                }
            }

            Double::class.java -> {
                return { size ->
                    DoubleArray(size)
                }
            }

            Char::class.java -> {
                return { size ->
                    CharArray(size)
                }
            }

            Boolean::class.java -> {
                return { size ->
                    BooleanArray(size)
                }
            }

            else -> {
                throw RuntimeException("Unsupported primitive type: ${contentType.name}")
            }
        }
    }
}


fun main() {
    println(IterWrapper.canWrap(List::class.java))
    println(IterWrapper.canWrap(IntArray::class.java))
    println(IterWrapper.canWrap(arrayOf(15, null).javaClass))


}