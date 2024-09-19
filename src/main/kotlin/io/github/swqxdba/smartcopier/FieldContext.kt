package io.github.swqxdba.smartcopier

import io.github.swqxdba.smartcopier.converter.PropertyValueConverter
import net.sf.cglib.core.ClassEmitter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

internal class FieldContext(
    val fields: MutableList<FieldWrapper> = mutableListOf(),
) {

    fun addField(name: String, value: Any?) {
        fields.add(FieldWrapper(name, value))
    }

    fun addValueConverter(converterFieldName:String,valueConverter: PropertyValueConverter, ce: ClassEmitter):FieldWrapper {
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



}