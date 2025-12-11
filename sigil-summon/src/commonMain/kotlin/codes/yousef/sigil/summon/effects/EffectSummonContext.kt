package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.effects.ShaderEffectData
import codes.yousef.sigil.schema.effects.EffectComposerData
import codes.yousef.sigil.schema.effects.SigilCanvasConfig
import codes.yousef.sigil.schema.effects.InteractionConfig

/**
 * Context registry for Sigil effect composition.
 *
 * Similar to SigilSummonContext but specialized for screen-space effects.
 * Tracks registered effects during composition for serialization (server)
 * or hydration (client).
 */
class EffectSummonContext private constructor(
    /**
     * Whether we're running on the server (JVM) or client (JS).
     */
    val isServer: Boolean
) {
    /**
     * Collected effects during composition.
     */
    private val _effects = mutableListOf<ShaderEffectData>()

    /**
     * Read-only view of collected effects.
     */
    val effects: List<ShaderEffectData>
        get() = _effects.toList()

    /**
     * Canvas configuration.
     */
    var canvasConfig: SigilCanvasConfig = SigilCanvasConfig()
        private set

    /**
     * Interaction configuration.
     */
    var interactionConfig: InteractionConfig = InteractionConfig()
        private set

    /**
     * Register an effect in the current context.
     */
    fun registerEffect(effect: ShaderEffectData) {
        _effects.add(effect)
    }

    /**
     * Configure the canvas settings.
     */
    fun configureCanvas(config: SigilCanvasConfig) {
        canvasConfig = config
    }

    /**
     * Configure interaction handling.
     */
    fun configureInteractions(config: InteractionConfig) {
        interactionConfig = config
    }

    /**
     * Build an EffectComposerData from collected effects.
     */
    fun buildComposerData(id: String): EffectComposerData {
        return EffectComposerData(
            id = id,
            effects = _effects.toList()
        )
    }

    /**
     * Clear all collected effects and reset configuration.
     */
    fun clear() {
        _effects.clear()
        canvasConfig = SigilCanvasConfig()
        interactionConfig = InteractionConfig()
    }

    companion object {
        /**
         * Thread-local context for the current composition.
         */
        @PublishedApi
        internal val currentContext = EffectThreadLocalContext()

        /**
         * Get the current context, throwing if none is active.
         */
        fun current(): EffectSummonContext {
            return currentContext.get()
                ?: error("No EffectSummonContext is active. Ensure you're inside a SigilEffectCanvas component.")
        }

        /**
         * Get the current context if one is active, or null otherwise.
         */
        fun currentOrNull(): EffectSummonContext? = currentContext.get()

        /**
         * Create a server-side context for SSR.
         */
        fun createServerContext(): EffectSummonContext = EffectSummonContext(isServer = true)

        /**
         * Create a client-side context for hydration.
         */
        fun createClientContext(): EffectSummonContext = EffectSummonContext(isServer = false)

        /**
         * Run a block with the given context as the current context.
         */
        inline fun <R> withContext(context: EffectSummonContext, block: () -> R): R {
            val previous = currentContext.get()
            currentContext.set(context)
            try {
                return block()
            } finally {
                if (previous != null) {
                    currentContext.set(previous)
                } else {
                    currentContext.remove()
                }
            }
        }
    }
}

/**
 * Platform-specific thread-local implementation for EffectSummonContext.
 */
expect class EffectThreadLocalContext() {
    fun get(): EffectSummonContext?
    fun set(context: EffectSummonContext)
    fun remove()
}
