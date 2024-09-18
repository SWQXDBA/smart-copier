package io.github.swqxdba.smartcopier

import io.github.swqxdba.smartcopier.typeconverter.TypeConvertProvider
import java.io.OutputStream

object SmartCopier {


    /**
     * 用于判断遇到的元素是否为bean 如果兼容则执行转换
     */
    @Volatile
    @JvmStatic
    var typeConvertProvider: TypeConvertProvider? = null

    private val cache = HashMap<String, Copier>()

    private val localCache: ThreadLocal<MutableMap<String, Copier>> = ThreadLocal.withInitial { mutableMapOf() }


    internal var defaultConfig = CopyConfig()

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
    @JvmStatic
    fun getCopier(sourceClass: Class<*>, targetClass: Class<*>, config: CopyConfig? = null): Copier {
        val hash = "" + sourceClass.hashCode() + targetClass.hashCode() + config.hashCode()
        val localCache = localCache.get()
        val localCopier = localCache[hash]
        if (localCopier != null) {
            return localCopier
        }
        synchronized(cache) {
            val copier = cache[hash]
            if (copier != null) {
                localCache[hash] = copier
                return copier
            }
            //二级缓存 用于避免递归导致的溢出
            val proxyCopier = ProxyCopier()
            localCache[hash] = proxyCopier
            cache[hash] = proxyCopier
            proxyCopier.copier = CopierGenerator(sourceClass, targetClass, config).generateCopier()
            return proxyCopier
        }

    }


    @JvmStatic
    @JvmOverloads
    fun copy(src: Any?, target: Any?, config: CopyConfig? = null) {
        if (src == null || target == null) {
            return
        }
        getCopier(src.javaClass, target.javaClass, config).copy(src, target)
    }

    @JvmStatic
    @JvmOverloads
    fun <T> copyToList(src: Iterable<*>?, targetClass: Class<T>, config: CopyConfig? = null): MutableList<T> {
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