package org.swqxdba.smartconvert

import org.swqxdba.smartconvert.SmartCopier.getCopier
import org.swqxdba.smartconvert.bean.BeanConvertProvider
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

object SmartCopier {


    /**
     * 用于判断遇到的元素是否为bean 如果兼容则执行转换
     */
    @Volatile
    @JvmStatic
    var beanConvertProvider: BeanConvertProvider? = null

    private val cache = ConcurrentHashMap<String, Copier>()

    /**
     * 二级缓存 用于避免递归导致的溢出
     */
    private val copierGeneratorContext = object {
        var deep = 0
        var innerCache = mutableMapOf<String, Copier>()

        @Synchronized
        fun with(block: (MutableMap<String, Copier>) -> Copier): Copier {
            try {
                deep++
                return block(innerCache)
            } finally {
                deep--
                if (deep == 0) {
                    innerCache.clear()
                }
            }
        }
    }

    /**
     * 是否开启debug模式
     */
    @JvmStatic
    var debugMode: Boolean = false

    /**
     * debug模式下 生成的class文件输出目录
     */
    @JvmStatic
    var debugOutPutDir: String? = null

    /**
     * debug模式下 生成的class byteArray输出流
     */
    @JvmStatic
    var debugOutputStream: OutputStream? = null

    @JvmOverloads
    fun getCopier(sourceClass: Class<*>, targetClass: Class<*>, config: CopyConfig? = null): Copier {
        val hash = "" + sourceClass.hashCode() + targetClass.hashCode() + config.hashCode()

        return cache.computeIfAbsent(hash) {
            copierGeneratorContext.with { innerMap ->
                //避免栈溢出
                val existCopier = innerMap[hash]
                if (existCopier != null) {
                    return@with existCopier
                }
                val proxyCopier = ProxyCopier()
                innerMap[hash] = proxyCopier
                proxyCopier.copier = CopierGenerator(sourceClass, targetClass, config).generateCopier()
                return@with proxyCopier
            }
        }

    }




@JvmStatic
@JvmOverloads
fun copy(src: Any?, target: Any?, config: CopyConfig? = CopyConfig()) {
    if (src == null || target == null) {
        return
    }
    getCopier(src.javaClass, target.javaClass, config).copy(src, target)
}

@JvmStatic
@JvmOverloads
fun <T> copyToList(src: Iterable<*>?, targetClass: Class<T>, config: CopyConfig? = CopyConfig()): MutableList<T> {
    if (src == null) {
        return mutableListOf()
    }
    val iterator = src.iterator()
    if (!iterator.hasNext()) {
        return mutableListOf()
    }
    var element = iterator.next()!!

    val copier = getCopier(element.javaClass, targetClass, config)
    //先获取constructor 避免targetClass.newInstance时的重复安全检查
    val constructor = targetClass.getConstructor()
        ?: throw Exception("copyToList fail, not found default constructor for ${targetClass.name}")
    val result = mutableListOf<T>()
    do {
        val newInstance = constructor.newInstance()
        copier.copy(element, newInstance)
        result.add(newInstance)
        if (!iterator.hasNext()) {
            break
        }
        element = iterator.next()!!
    } while (true)
    return result
}


@JvmStatic
@JvmOverloads
fun copyNonNullProperties(src: Any?, target: Any?, config: CopyConfig? = null) {
    if (src == null || target == null) {
        return
    }
    getCopier(src.javaClass, target.javaClass, config).copyNonNullProperties(src, target)
}

@JvmStatic
@JvmOverloads
fun merge(src: Any?, target: Any?, config: CopyConfig? = null) {
    if (src == null || target == null) {
        return
    }
    getCopier(src.javaClass, target.javaClass, config).merge(src, target)
}
}