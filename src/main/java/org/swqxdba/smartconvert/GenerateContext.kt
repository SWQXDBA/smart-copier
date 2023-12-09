package org.swqxdba.smartconvert

internal class GenerateContext (
    val fields :MutableList<FieldWrapper> = mutableListOf(),
){
    fun addField(name:String,value:Any?){
        fields.add(FieldWrapper(name,value))
    }

    fun addValueConverter(converterFieldName:String, valueConverter: PropertyValueConverter){
        val fieldWrapper = FieldWrapper(converterFieldName,  valueConverter)
        this.convertFields+=fieldWrapper
        fields.add(fieldWrapper)
    }

    var convertFields:MutableList<FieldWrapper> = mutableListOf()

    /**
     * 尝试匹配到一个转换器
     */
    fun matchConverter(block:(PropertyValueConverter)->Boolean):FieldWrapper?{
        for (fieldWrapper in convertFields) {
            val fieldValue = fieldWrapper.value
            if (fieldValue is PropertyValueConverter) {
                if(block(fieldValue)){
                    return fieldWrapper
                }
            }
        }
        return null
    }
}