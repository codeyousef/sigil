package io.github.codeyousef.sigil.summon.context

/**
 * JVM implementation using ThreadLocal.
 */
actual class ThreadLocalContext actual constructor() {
    private val threadLocal = ThreadLocal<SigilSummonContext?>()

    actual fun get(): SigilSummonContext? = threadLocal.get()

    actual fun set(context: SigilSummonContext) {
        threadLocal.set(context)
    }

    actual fun remove() {
        threadLocal.remove()
    }
}
