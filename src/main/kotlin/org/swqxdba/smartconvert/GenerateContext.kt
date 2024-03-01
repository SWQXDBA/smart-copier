package org.swqxdba.smartconvert

import net.sf.cglib.core.ClassEmitter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

internal class GenerateContext(
    val fields: MutableList<FieldWrapper> = mutableListOf(),
) {
    private var converterCounter = 0

    fun addField(name: String, value: Any?) {
        fields.add(FieldWrapper(name, value))
    }

    fun addValueConverter(valueConverter: PropertyValueConverter, ce: ClassEmitter):FieldWrapper {
        val fieldWrapper = addStatefulValueConverter(valueConverter, ce)
        this.convertFields += fieldWrapper
        return fieldWrapper
    }

    //添加临时的转换器 这个转换器暂时不会被match到其他字段中 这样保证一个有状态的转换器不会被混用
    fun addStatefulValueConverter(valueConverter: PropertyValueConverter, ce: ClassEmitter):FieldWrapper {
        val converterFieldName = "propertyValueConverter${converterCounter++}"
        val fieldWrapper = FieldWrapper(converterFieldName, valueConverter)

        fields.add(fieldWrapper)
        ce.declare_field(
            Opcodes.ACC_PRIVATE,
            converterFieldName,
            Type.getType(PropertyValueConverter::class.java),
            null
        )
        return fieldWrapper
    }

    var convertFields: MutableList<FieldWrapper> = mutableListOf()

    /**
     * 尝试匹配到一个转换器
     */
    fun matchConverter(block: (PropertyValueConverter) -> Boolean): FieldWrapper? {
        for (fieldWrapper in convertFields) {
            val fieldValue = fieldWrapper.value
            if (fieldValue is PropertyValueConverter) {
                if (block(fieldValue)) {
                    return fieldWrapper
                }
            }
        }
        return null
    }


}