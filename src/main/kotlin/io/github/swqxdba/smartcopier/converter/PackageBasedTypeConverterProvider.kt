package io.github.swqxdba.smartcopier.converter

import io.github.swqxdba.smartcopier.Copier
import io.github.swqxdba.smartcopier.SmartCopier
import java.lang.reflect.Constructor
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

/**
 * 基于包名前缀的类型转换器工厂
 */
class PackageBasedTypeConverterProvider(private val basePackage: String, val smartCopier: SmartCopier) :
    TypeConverterProvider {

    private var cache: MutableMap<String, TypeConverter> = ConcurrentHashMap<String, TypeConverter>()


    override fun tryGetConverter(from: Type, to: Type): TypeConverter? {
        if (from !is Class<*> || to !is Class<*>) {
            return null
        }
        val fromClass = from
        val toClass = to
        val key = fromClass.name + toClass.name
        val typeConverter: TypeConverter? = cache[key]
        if (typeConverter != null) {
            return typeConverter
        }
        if (!fromClass.getPackage().name.startsWith(basePackage) || !toClass.getPackage().name.startsWith(basePackage)) {
            return null
        }
        return cache.computeIfAbsent(key) { k: String? ->
            object : TypeConverter {
                val copier: Copier
                var targetConstructor: Constructor<*>

                init {
                    try {
                        targetConstructor = toClass.getConstructor()
                        targetConstructor.setAccessible(true)
                        copier = smartCopier.getCopier(fromClass, toClass)
                    } catch (e: NoSuchMethodException) {
                        throw RuntimeException(e)
                    }
                }

                override fun doConvert(from: Any?): Any {
                    val instance: Any = targetConstructor.newInstance()
                    copier.copy(from, instance)
                    return instance
                }

            }
        }
    }
}