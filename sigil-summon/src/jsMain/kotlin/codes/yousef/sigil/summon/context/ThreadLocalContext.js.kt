package codes.yousef.sigil.summon.context

/**
 * JS implementation using a simple variable (JS is single-threaded).
 */
actual class ThreadLocalContext actual constructor() {
    actual fun get(): SigilSummonContext? = currentContextJs

    actual fun set(context: SigilSummonContext) {
        currentContextJs = context
    }

    actual fun remove() {
        currentContextJs = null
    }
}

private var currentContextJs: SigilSummonContext? = null
