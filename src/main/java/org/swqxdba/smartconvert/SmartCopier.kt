package org.swqxdba.smartconvert

import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

object SmartCopier {

    private val cache = ConcurrentHashMap<String,Copier>()

    /**
     * 是否开启debug模式
     */
    @JvmStatic
    var debugMode:Boolean = false

    /**
     * debug模式下 生成的class文件输出目录
     */
    @JvmStatic
    var debugOutPutDir:String? = null

    /**
     * debug模式下 生成的class byteArray输出流
     */
    @JvmStatic
    var debugOutputStream:OutputStream? = null

    @JvmOverloads
    fun getCopier(sourceClass: Class<*>, targetClass: Class<*>, config: CopyConfig? = null): Any {
        val hash = ""+sourceClass.hashCode()+targetClass.hashCode()+config.hashCode()
        return cache.computeIfAbsent(hash){
            CopierGenerator(sourceClass, targetClass, config).generateCopier()
        }
    }
}