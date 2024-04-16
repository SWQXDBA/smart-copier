package io.github.swqxdba.smartcopier

import java.lang.reflect.Method

class PropertyDescriptor{
    var name:String = ""
    var readMethod:Method? = null
    var writeMethod: Method? = null
}