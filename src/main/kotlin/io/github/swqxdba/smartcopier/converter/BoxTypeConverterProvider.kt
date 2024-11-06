package io.github.swqxdba.smartcopier.converter

import io.github.swqxdba.smartcopier.InternalUtil.getPrimitiveClass
import java.lang.reflect.Type

/**
 * 自动拆箱类型转换器
 */
open class BoxTypeConverterProvider(
    private val defaultInt: Int = IntArray(1)[0],
    private val defaultLong: Long = LongArray(1)[0],
    private val defaultFloat: Float = FloatArray(1)[0],
    private val defaultDouble: Double = DoubleArray(1)[0],
    private val defaultBoolean: Boolean = BooleanArray(1)[0],
    private val defaultShort: Short = ShortArray(1)[0],
    private val defaultByte: Byte = ByteArray(1)[0],
    private val defaultChar: Char = CharArray(1)[0]
) : TypeConverterProvider {

    companion object {
        internal val instance = BoxTypeConverterProvider()
    }

    override fun tryGetConverter(from: Type, to: Type): TypeConverter? {
        if (to !is Class<*> || from !is Class<*>) {
            return null
        }
        val fromP = getPrimitiveClass(from) ?: return null
        val toP = getPrimitiveClass(to) ?: return null

        if (fromP != toP) {
            return null
        }
        if (!to.isPrimitive) {
            return object : TypeConverter {
                override fun doConvert(from: Any?): Any? {
                    return from
                }
            }
        } else {
            val defaultValue = when (to) {
                Int::class.java -> defaultInt
                Long::class.java -> defaultLong
                Float::class.java -> defaultFloat
                Double::class.java -> defaultDouble
                Boolean::class.java -> defaultBoolean
                Short::class.java -> defaultShort
                Byte::class.java -> defaultByte
                Char::class.java -> defaultChar
                else -> throw IllegalArgumentException("Unsupported primitive type: $to")
            }
            return object : TypeConverter {
                override fun doConvert(from: Any?): Any {
                    return from ?: defaultValue
                }
            }
        }


    }


}