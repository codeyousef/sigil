package io.github.codeyousef.sigil.compose.applier

import androidx.compose.runtime.Applier
import io.github.codeyousef.sigil.compose.node.MateriaNodeWrapper

/**
 * Compose Runtime Applier implementation for Materia scene graph manipulation.
 *
 * This applier bridges the Compose runtime's reconciliation algorithm with
 * Materia's Object3D scene graph, enabling declarative 3D scene composition.
 *
 * The applier follows Compose's tree manipulation protocol:
 * - insertTopDown/insertBottomUp for node insertion
 * - remove for node removal
 * - move for node reordering
 * - onClear for complete scene reset
 *
 * Thread Safety: This applier should only be used from the main/render thread.
 */
class MateriaApplier(
    root: MateriaNodeWrapper
) : Applier<MateriaNodeWrapper> {

    /**
     * Stack tracking the current position in the scene graph during composition.
     * The bottom of the stack is always the root scene.
     */
    private val nodeStack = mutableListOf<MateriaNodeWrapper>(root)

    /**
     * Current node being composed into.
     */
    override var current: MateriaNodeWrapper
        get() = nodeStack.last()
        set(value) {
            // Replace the top of the stack
            if (nodeStack.isNotEmpty()) {
                nodeStack[nodeStack.lastIndex] = value
            } else {
                nodeStack.add(value)
            }
        }

    /**
     * Move down into a child node for composition.
     * Called when entering a node's content lambda.
     */
    override fun down(node: MateriaNodeWrapper) {
        nodeStack.add(node)
    }

    /**
     * Move up to the parent node after composing children.
     * Called when exiting a node's content lambda.
     */
    override fun up() {
        require(nodeStack.size > 1) {
            "Cannot move up from root node"
        }
        nodeStack.removeLast()
    }

    /**
     * Insert nodes using a top-down approach.
     * For Materia's scene graph, we perform the actual insertion here
     * since parent-child relationships are established during insertion.
     */
    override fun insertTopDown(index: Int, instance: MateriaNodeWrapper) {
        current.insert(index, instance)
    }

    /**
     * Insert nodes using a bottom-up approach.
     * Since we handle insertion in insertTopDown, this is a no-op.
     * Some scene graphs prefer bottom-up for efficiency (building subtrees
     * before attaching to parent), but Materia handles this fine top-down.
     */
    override fun insertBottomUp(index: Int, instance: MateriaNodeWrapper) {
        // No-op: insertion is handled in insertTopDown
        // This is valid as per Compose's Applier contract
    }

    /**
     * Remove a range of children from the current node.
     *
     * @param index Starting index of children to remove
     * @param count Number of children to remove
     */
    override fun remove(index: Int, count: Int) {
        current.remove(index, count)
    }

    /**
     * Move children from one position to another within the current node.
     * This handles list reordering operations.
     *
     * @param from Starting index of the range to move
     * @param to Destination index
     * @param count Number of items to move
     */
    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    /**
     * Clear the entire scene graph.
     * Called when the composition is completely reset or disposed.
     *
     * CRITICAL: This must properly dispose all GPU resources to prevent
     * memory leaks. We clear all children from the root scene and
     * dispose their resources.
     */
    override fun onClear() {
        // Clear all children from the root, disposing resources
        val root = nodeStack.first()
        root.clear()

        // Reset the stack to just the root
        nodeStack.clear()
        nodeStack.add(root)
    }

    /**
     * Called at the start of applying changes.
     * We could use this for batching or deferring GPU operations.
     */
    fun onBeginChanges() {
        // Could implement change batching here if needed
    }

    /**
     * Called at the end of applying changes.
     * We could use this to flush batched operations.
     */
    fun onEndChanges() {
        // Could flush batched changes here if needed
    }
}
