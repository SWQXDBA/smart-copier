package io.github.swqxdba.smartcopier

import net.sf.cglib.core.CodeEmitter
import net.sf.cglib.core.Constants
import net.sf.cglib.core.Local
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


//////以下几个方法是为了修复 cglib 的错误调用 其中CodeEmitter的load_local变成了委托调用
///    private void load_local(Type t, int pos) {
//        // TODO: make t == null ok?
//        mv.visitVarInsn(t.getOpcode(Constants.ILOAD), pos);
//    }
// 这里应该改为 this.visitVarInsn(t.getOpcode(Constants.ILOAD), pos); 这样可以从父类中调用remap方法

fun CodeEmitter.loadLocal(t: Type, pos: Int) {
    this.visitVarInsn(t.getOpcode(Constants.ILOAD), pos)
}

fun CodeEmitter.storeLocal(t: Type, pos: Int) {
    this.visitVarInsn(t.getOpcode(Constants.ISTORE), pos)
}

fun CodeEmitter.storeLocal(local: Local) {
    storeLocal(local.type, local.index)
}

fun CodeEmitter.loadLocal(local: Local) {
    loadLocal(local.type, local.index)
}
