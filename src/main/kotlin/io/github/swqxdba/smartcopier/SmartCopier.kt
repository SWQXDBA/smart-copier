package io.github.swqxdba.smartcopier

import java.io.OutputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class SmartCopier {

    /**
     * 默认的拷贝配置，当getCopier传入的CopyConfig为null时，使用此配置
     */
    var defaultConfig: CopyConfig? = CopyConfig()

    /**
     * 是否开启debug模式
     */
    var debugMode: Boolean = false

    /**
     * debug模式下 生成的class文件输出目录
     */
    var debugOutPutDir: String? = null

    /**
     * debug模式下 生成的class byteArray输出流
     */
    var debugOutputStream: OutputStream? = null


    /**
     * 生成所有copier类型所使用的时间
     */
    val copierGenerateUseMills: Long get() = _copierGenerateUseMills.get()

    /**
     * 生成的copier总数
     */
    val copierCount: Int get() = _copierCount.get()


    private val _copierCount = AtomicInteger()

    private val _copierGenerateUseMills = AtomicLong()


    private val globalCache = HashMap<String, Copier>()

    private val localCache: ThreadLocal<MutableMap<String, Copier>> = ThreadLocal.withInitial { mutableMapOf() }


    /**
     * 获取copier实例
     * @param sourceClass 源对象类型
     * @param targetClass 目标对象类型
     * @param config 拷贝配置 如果为null 则使用默认配置
     */
    @JvmOverloads
    fun getCopier(sourceClass: Class<*>, targetClass: Class<*>, config: CopyConfig? = defaultConfig): Copier {
        val hash = "" + sourceClass.hashCode() + targetClass.hashCode() + System.identityHashCode(config ?: 0)
        val localCache = localCache.get()
        val localCopier = localCache[hash]
        if (localCopier != null) {
            return localCopier
        }
        synchronized(globalCache) {
            val copier = globalCache[hash]
            if (copier != null) {
                localCache[hash] = copier
                return copier
            }
            _copierCount.incrementAndGet()
            //二级缓存 用于避免递归导致的溢出
            val proxyCopier = ProxyCopier()
            localCache[hash] = proxyCopier
            globalCache[hash] = proxyCopier
            val start = System.currentTimeMillis()
            proxyCopier.copier = CopierGenerator(sourceClass, targetClass, config, this).generateCopier()
            _copierGenerateUseMills.addAndGet(System.currentTimeMillis() - start)
            return proxyCopier
        }

    }


    @JvmOverloads
    fun copy(src: Any?, target: Any?, config: CopyConfig? = defaultConfig) {
        if (src == null || target == null) {
            return
        }
        getCopier(src.javaClass, target.javaClass, config).copy(src, target)
    }


    /**
     * 批量拷贝元素，要求src中的每个元素拥有相同的类型
     */
    @JvmOverloads
    fun <T> copyToList(
        src: Iterable<*>?,
        targetClass: Class<T>,
        config: CopyConfig? = defaultConfig,
        ignoreNullProperty: Boolean = false
    ): MutableList<T> {
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
        val result = if (src is Collection<*>) {
            ArrayList<T>(src.size)
        } else {
            mutableListOf()
        }
        if (ignoreNullProperty) {
            do {
                val newInstance = constructor.newInstance()
                copier.copyNonNullProperties(element, newInstance)
                result.add(newInstance)
                if (!iterator.hasNext()) {
                    break
                }
                element = iterator.next()!!
            } while (true)

        } else {
            do {
                val newInstance = constructor.newInstance()
                copier.copy(element, newInstance)
                result.add(newInstance)
                if (!iterator.hasNext()) {
                    break
                }
                element = iterator.next()!!
            } while (true)
        }

        return result
    }


    @JvmOverloads
    fun copyNonNullProperties(src: Any?, target: Any?, config: CopyConfig? = null) {
        if (src == null || target == null) {
            return
        }
        getCopier(src.javaClass, target.javaClass, config).copyNonNullProperties(src, target)
    }


    @JvmOverloads
    fun merge(src: Any?, target: Any?, config: CopyConfig? = null) {
        if (src == null || target == null) {
            return
        }
        getCopier(src.javaClass, target.javaClass, config).merge(src, target)
    }
}