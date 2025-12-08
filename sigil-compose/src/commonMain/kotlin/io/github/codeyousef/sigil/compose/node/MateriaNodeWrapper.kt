package io.github.codeyousef.sigil.compose.node

import io.materia.engine.scene.EngineMesh
import io.materia.engine.scene.Scene
import io.materia.core.math.Vector3
import io.materia.core.Object3D
import io.materia.engine.core.Disposable
import io.materia.engine.core.DisposableContainer
import io.materia.light.Light
import io.materia.light.AmbientLight
import io.materia.light.DirectionalLight
import io.materia.light.PointLight
import io.materia.light.SpotLight
import io.materia.engine.camera.PerspectiveCamera
import io.materia.engine.camera.OrthographicCamera

/**
 * Wrapper class for Materia Object3D nodes to manage parent-child relationships
 * in the Compose runtime's scene graph reconciliation.
 *
 * This wrapper provides a consistent interface for:
 * - Inserting child nodes at specific indices
 * - Removing child nodes
 * - Moving child nodes within the hierarchy
 * - Proper resource disposal
 */
class MateriaNodeWrapper(
    val internalNode: Object3D
) : Disposable {
    private val children = mutableListOf<MateriaNodeWrapper>()
    private val disposables = DisposableContainer()

    /**
     * Parent wrapper in the scene graph hierarchy
     */
    var parent: MateriaNodeWrapper? = null
        private set

    /**
     * Read-only view of child wrappers
     */
    val childrenList: List<MateriaNodeWrapper>
        get() = children.toList()

    /**
     * Number of children in this node
     */
    val childCount: Int
        get() = children.size

    /**
     * Insert a child wrapper at the specified index.
     * This adds the internal Materia node to the scene graph.
     */
    fun insert(index: Int, instance: MateriaNodeWrapper) {
        require(index in 0..children.size) {
            "Insert index $index out of bounds for size ${children.size}"
        }

        // Remove from previous parent if any
        instance.parent?.remove(instance)

        // Add to our tracking list
        children.add(index, instance)
        instance.parent = this

        // Add the internal Materia node to the scene graph
        internalNode.add(instance.internalNode)

        // Track for disposal
        if (instance.internalNode is Disposable) {
            disposables.track(instance.internalNode as Disposable)
        }
    }

    /**
     * Remove a number of children starting at the specified index.
     * This removes the internal Materia nodes from the scene graph.
     */
    fun remove(index: Int, count: Int) {
        require(index >= 0 && index + count <= children.size) {
            "Remove range [$index, ${index + count}) out of bounds for size ${children.size}"
        }

        val toRemove = children.subList(index, index + count).toList()

        for (child in toRemove) {
            // Remove from Materia scene graph
            internalNode.remove(child.internalNode)
            child.parent = null

            // Untrack from disposables
            if (child.internalNode is Disposable) {
                disposables.untrack(child.internalNode as Disposable)
            }
        }

        // Remove from our tracking list
        children.subList(index, index + count).clear()
    }

    /**
     * Remove a specific child wrapper.
     */
    fun remove(child: MateriaNodeWrapper) {
        val index = children.indexOf(child)
        if (index >= 0) {
            remove(index, 1)
        }
    }

    /**
     * Move children from one position to another within this node's hierarchy.
     * This reorders both the tracking list and can affect render order.
     */
    fun move(from: Int, to: Int, count: Int) {
        require(from >= 0 && from + count <= children.size) {
            "Move source range [$from, ${from + count}) out of bounds for size ${children.size}"
        }
        require(to >= 0 && to <= children.size - count) {
            "Move destination $to out of bounds for size ${children.size} with count $count"
        }

        if (from == to) return

        // Extract the items to move
        val itemsToMove = children.subList(from, from + count).toList()

        // Remove from original position
        children.subList(from, from + count).clear()

        // Calculate adjusted target position
        val adjustedTo = if (to > from) to - count else to

        // Insert at new position
        children.addAll(adjustedTo, itemsToMove)

        // Note: Materia's Object3D.children order typically doesn't affect rendering
        // order for opaque objects, but might for transparent objects.
        // For strict ordering, we'd need to remove and re-add all children.
    }

    /**
     * Clear all children from this node.
     * Disposes all child resources.
     */
    fun clear() {
        for (child in children) {
            internalNode.remove(child.internalNode)
            child.parent = null
            child.dispose()
        }
        children.clear()
    }

    /**
     * Get child at the specified index.
     */
    fun getChildAt(index: Int): MateriaNodeWrapper? {
        return children.getOrNull(index)
    }

    /**
     * Find a child by predicate.
     */
    fun findChild(predicate: (MateriaNodeWrapper) -> Boolean): MateriaNodeWrapper? {
        return children.find(predicate)
    }

    /**
     * Update the position of the internal node.
     */
    fun setPosition(x: Float, y: Float, z: Float) {
        internalNode.position.set(x, y, z)
    }

    /**
     * Update the rotation of the internal node (euler angles in radians).
     */
    fun setRotation(x: Float, y: Float, z: Float) {
        internalNode.rotation.set(x, y, z)
    }

    /**
     * Update the scale of the internal node.
     */
    fun setScale(x: Float, y: Float, z: Float) {
        internalNode.scale.set(x, y, z)
    }

    /**
     * Set the visibility of the internal node.
     */
    fun setVisible(visible: Boolean) {
        internalNode.visible = visible
    }

    /**
     * Set the name of the internal node.
     */
    fun setName(name: String) {
        internalNode.name = name
    }

    override val isDisposed: Boolean
        get() = disposables.isDisposed

    /**
     * Dispose this wrapper and all its resources.
     * This recursively disposes all children first.
     */
    override fun dispose() {
        // Dispose children first (bottom-up)
        for (child in children) {
            child.dispose()
        }
        children.clear()

        // Dispose tracked resources
        disposables.dispose()

        // Dispose the internal node if it's disposable
        if (internalNode is Disposable) {
            (internalNode as Disposable).dispose()
        }

        // Remove from parent
        parent?.let { p ->
            p.internalNode.remove(internalNode)
        }
        parent = null
    }

    override fun toString(): String {
        val nodeType = internalNode::class.simpleName ?: "Unknown"
        val name = internalNode.name.ifEmpty { "unnamed" }
        return "MateriaNodeWrapper($nodeType: $name, children=${children.size})"
    }

    companion object {
        /**
         * Create a root wrapper for a Scene.
         */
        fun createRoot(scene: Scene): MateriaNodeWrapper {
            return MateriaNodeWrapper(scene)
        }

        /**
         * Create a wrapper for a Group node.
         */
        fun createGroup(): MateriaNodeWrapper {
            return MateriaNodeWrapper(io.materia.engine.scene.Group())
        }
    }
}
