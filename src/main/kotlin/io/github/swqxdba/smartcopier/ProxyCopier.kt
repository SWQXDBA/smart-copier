package io.github.swqxdba.smartcopier

class ProxyCopier : Copier {
    lateinit var copier: Copier
    override fun copy(src: Any?, target: Any?) {
        copier.copy(src, target)
    }

    override fun copyNonNullProperties(src: Any?, target: Any?) {
        copier.copyNonNullProperties(src, target)
    }

    override fun merge(src: Any?, target: Any?) {
        copier.merge(src, target)
    }
}
