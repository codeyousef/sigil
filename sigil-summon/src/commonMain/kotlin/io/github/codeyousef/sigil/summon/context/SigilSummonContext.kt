package io.github.codeyousef.sigil.summon.context

import io.github.codeyousef.sigil.schema.SigilNodeData
import io.github.codeyousef.sigil.schema.SigilScene
import io.github.codeyousef.sigil.schema.SceneSettings

/**
 * Context registry for Sigil-Summon integration.
 *
 * Since Summon renders to HTML strings on JVM (server-side), components
 * cannot "return" Materia objects. Instead, they register scene data
 * in this context, which is then serialized to JSON for client hydration.
 *
 * On the JS client, this context is used to collect hydrated node data
 * and create actual Materia objects.
 */
class SigilSummonContext private constructor(
    /**
     * Whether we're running on the server (JVM) or client (JS).
     */
    val isServer: Boolean
) {
    /**
     * Collected scene nodes during composition.
     */
    private val _nodes = mutableListOf<SigilNodeData>()

    /**
     * Read-only view of collected nodes.
     */
    val nodes: List<SigilNodeData>
        get() = _nodes.toList()

    /**
     * Scene settings configured during composition.
     */
    var settings: SceneSettings = SceneSettings()
        private set

    /**
     * Stack of parent nodes for tracking nested groups.
     */
    private val parentStack = mutableListOf<MutableList<SigilNodeData>>()

    /**
     * Current parent's children list (or root nodes if no parent).
     */
    private val currentChildren: MutableList<SigilNodeData>
        get() = parentStack.lastOrNull() ?: _nodes

    /**
     * Register a node in the current context.
     * If we're inside a group, the node is added as a child of that group.
     */
    fun registerNode(node: SigilNodeData) {
        currentChildren.add(node)
    }

    /**
     * Enter a group context for nesting children.
     */
    fun enterGroup(children: MutableList<SigilNodeData>) {
        parentStack.add(children)
    }

    /**
     * Exit the current group context.
     */
    fun exitGroup() {
        if (parentStack.isNotEmpty()) {
            parentStack.removeLast()
        }
    }

    /**
     * Configure scene settings.
     */
    fun configureSettings(configure: SceneSettings.() -> SceneSettings) {
        settings = settings.configure()
    }

    /**
     * Build the final SigilScene from collected nodes.
     */
    fun buildScene(): SigilScene {
        return SigilScene(
            rootNodes = _nodes.toList(),
            settings = settings
        )
    }

    /**
     * Clear all collected nodes.
     */
    fun clear() {
        _nodes.clear()
        parentStack.clear()
        settings = SceneSettings()
    }

    companion object {
        /**
         * Thread-local context for the current composition.
         */
        private val currentContext = ThreadLocalContext()

        /**
         * Get the current context, throwing if none is active.
         */
        fun current(): SigilSummonContext {
            return currentContext.get()
                ?: error("No SigilSummonContext is active. Ensure you're inside a MateriaCanvas component.")
        }

        /**
         * Get the current context if one is active, or null otherwise.
         */
        fun currentOrNull(): SigilSummonContext? = currentContext.get()

        /**
         * Create a server-side context for SSR.
         */
        fun createServerContext(): SigilSummonContext = SigilSummonContext(isServer = true)

        /**
         * Create a client-side context for hydration.
         */
        fun createClientContext(): SigilSummonContext = SigilSummonContext(isServer = false)

        /**
         * Run a block with the given context as the current context.
         */
        inline fun <R> withContext(context: SigilSummonContext, block: () -> R): R {
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
 * Platform-specific thread-local implementation.
 * This is expect/actual to handle JVM vs JS differences.
 */
expect class ThreadLocalContext() {
    fun get(): SigilSummonContext?
    fun set(context: SigilSummonContext)
    fun remove()
}
