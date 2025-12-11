package codes.yousef.sigil.summon.effects

/**
 * JS implementation using a simple variable (JS is single-threaded).
 */
actual class EffectThreadLocalContext actual constructor() {
    actual fun get(): EffectSummonContext? = currentEffectContextJs

    actual fun set(context: EffectSummonContext) {
        currentEffectContextJs = context
    }

    actual fun remove() {
        currentEffectContextJs = null
    }
}

private var currentEffectContextJs: EffectSummonContext? = null
