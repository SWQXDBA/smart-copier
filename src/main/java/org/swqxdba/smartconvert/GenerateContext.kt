package org.swqxdba.smartconvert

class GenerateContext (
    val fields :MutableList<FieldWrapper> = mutableListOf(),
){
    fun addField(name:String,value:Any?){
        fields.add(FieldWrapper(name,value))
    }

    fun setValueConverter(valueConverter: PropertyValueConverter){
        val fieldWrapper = FieldWrapper("propertyValueConverter",  valueConverter)
        this.convertField = fieldWrapper
        fields.add(fieldWrapper)
    }

    var convertField:FieldWrapper?=null
}