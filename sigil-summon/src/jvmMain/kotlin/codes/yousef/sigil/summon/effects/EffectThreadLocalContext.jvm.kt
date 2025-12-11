package codes.yousef.sigil.summon.effects

/**
 * JVM implementation using ThreadLocal.
 */
actual class EffectThreadLocalContext actual constructor() {
    private val threadLocal = ThreadLocal<EffectSummonContext?>()

    actual fun get(): EffectSummonContext? = threadLocal.get()

    actual fun set(context: EffectSummonContext) {
        threadLocal.set(context)
    }

    actual fun remove() {
        threadLocal.remove()
    }
}
