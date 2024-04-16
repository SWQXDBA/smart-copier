package io.github.swqxdba.smartcopier

import net.sf.cglib.core.CodeEmitter
import org.objectweb.asm.Type


fun CodeEmitter.dupForType(type: Type) {
    if (type.size == 1) {
        dup()
    } else {
        dup2()
    }
}
fun CodeEmitter.popForType(type: Type) {
    if (type.size == 1) {
        pop()
    } else {
        pop2()
    }
}