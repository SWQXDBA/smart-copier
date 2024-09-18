package io.github.swqxdba.smartcopier.bean

import io.github.swqxdba.smartcopier.Copier
import io.github.swqxdba.smartcopier.SmartCopier
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractSmartCopierBasedProvider(private val smartCopier: SmartCopier) : BeanConvertProvider {
    private val cache = ConcurrentHashMap<String, BeanConverter>()
    abstract fun shouldConvert(from: Class<*>, to: Class<*>): Boolean

    override fun tryGetConverter(fromClass: Class<*>, toClass: Class<*>): BeanConverter? {
        val key = "${System.identityHashCode(fromClass)}${System.identityHashCode(toClass)}"
        val beanConverter = cache[key]
        if (beanConverter != null) {
            return beanConverter
        }

        if (shouldConvert(fromClass, toClass)) {
            return cache.getOrPut(key) {
                object : BeanConverter {
                    private var copier: Copier = smartCopier.getCopier(fromClass, toClass)
                    private val targetConstructor: Constructor<*> by lazy {
                        try {
                            toClass.getConstructor()
                        } catch (e: NoSuchMethodException) {
                            throw RuntimeException(e)
                        }.also { it.isAccessible = true }
                    }
                    override fun doConvert(from: Any): Any {
                        val instance = targetConstructor.newInstance()
                        copier.copy(from, instance)
                        return instance
                    }
                }
            }
        }
        return null
    }


}
