package io.github.codeyousef.sigil.compose.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import io.github.codeyousef.sigil.compose.applier.MateriaApplier
import io.github.codeyousef.sigil.compose.node.MateriaNodeWrapper
import io.materia.core.scene.Object3D

/**
 * Generic Composable node for creating Materia scene graph objects.
 *
 * This is the core building block that bridges Compose's ComposeNode API
 * with Materia's Object3D hierarchy.
 *
 * @param T The type of Materia Object3D being created
 * @param factory Lambda to create the Materia node
 * @param update Lambda to update the node's properties
 */
@Composable
inline fun <reified T : Object3D> MateriaNode(
    crossinline factory: () -> T,
    crossinline update: (T) -> Unit = {}
) {
    ComposeNode<MateriaNodeWrapper, MateriaApplier>(
        factory = {
            MateriaNodeWrapper(factory())
        },
        update = {
            set(Unit) {
                @Suppress("UNCHECKED_CAST")
                update(this.internalNode as T)
            }
        }
    )
}

/**
 * Composable node that can have children.
 */
@Composable
inline fun <reified T : Object3D> MateriaNodeWithContent(
    crossinline factory: () -> T,
    crossinline update: (T) -> Unit = {},
    content: @Composable () -> Unit
) {
    ComposeNode<MateriaNodeWrapper, MateriaApplier>(
        factory = {
            MateriaNodeWrapper(factory())
        },
        update = {
            set(Unit) {
                @Suppress("UNCHECKED_CAST")
                update(this.internalNode as T)
            }
        },
        content = content
    )
}
