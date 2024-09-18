package io.github.swqxdba.smartcopier.bean

import io.github.swqxdba.smartcopier.Copier
import io.github.swqxdba.smartcopier.SmartCopier
import io.github.swqxdba.smartcopier.converter.TypeConverter
import io.github.swqxdba.smartcopier.converter.TypeConverterProvider
import java.lang.reflect.Constructor
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractSmartCopierBasedProvider(private val smartCopier: SmartCopier) : TypeConverterProvider {
    private val cache = ConcurrentHashMap<String, TypeConverter>()
    abstract fun shouldConvert(fromClass: Class<*>, toClass: Class<*>): Boolean

    override fun tryGetConverter(from: Type, to: Type): TypeConverter? {
        val key = "${System.identityHashCode(from)}${System.identityHashCode(to)}"
        val typeConverter = cache[key]
        if (typeConverter != null) {
            return typeConverter
        }
        if(from !is Class<*> || to !is Class<*>){
            return null
        }

        if (shouldConvert(from, to)) {

            return cache.getOrPut(key) {
                object : TypeConverter {
                    private var copier: Copier = smartCopier.getCopier(from, to)
                    private val targetConstructor: Constructor<*> by lazy {
                        try {
                            to.getConstructor()
                        } catch (e: NoSuchMethodException) {
                            throw RuntimeException(e)
                        }.also { it.isAccessible = true }
                    }
                    override fun doConvert(from: Any?): Any? {
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
