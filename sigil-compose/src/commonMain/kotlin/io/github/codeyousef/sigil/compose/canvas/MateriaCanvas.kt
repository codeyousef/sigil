package io.github.codeyousef.sigil.compose.canvas

import androidx.compose.runtime.Composable
import io.materia.core.scene.Scene
import io.materia.camera.PerspectiveCamera

/**
 * Entry point for rendering Materia 3D scenes within Compose.
 *
 * MateriaCanvas provides the bridge between the Compose layout system and
 * Materia's GPU rendering pipeline. It manages:
 * - GPU context initialization
 * - Scene graph composition using Compose's reconciliation
 * - Render loop integration
 * - Resource cleanup on disposal
 *
 * Platform implementations:
 * - Desktop (JVM): Uses AWT Canvas with Vulkan
 * - Web (JS): Uses HTML Canvas with WebGPU
 * - Android: Uses SurfaceView/TextureView
 * - iOS: Uses UIView with Metal (via MoltenVK)
 *
 * @param modifier Compose modifier for layout positioning and sizing
 * @param backgroundColor Background color as ARGB int
 * @param camera The camera to use for rendering (null = auto-create)
 * @param content Composable lambda containing 3D scene nodes
 */
@Composable
expect fun MateriaCanvas(
    modifier: Any = Unit, // Will be Modifier on actual platforms
    backgroundColor: Int = 0xFF1A1A2E.toInt(),
    camera: PerspectiveCamera? = null,
    content: @Composable () -> Unit
)

/**
 * State holder for MateriaCanvas.
 * Manages the scene, renderer, and render loop lifecycle.
 */
class MateriaCanvasState {
    /**
     * The Materia scene being rendered.
     */
    var scene: Scene? = null
        internal set

    /**
     * Whether the canvas is currently initialized and rendering.
     */
    var isInitialized: Boolean = false
        internal set

    /**
     * Current frames per second.
     */
    var fps: Float = 0f
        internal set

    /**
     * Total elapsed time since initialization.
     */
    var elapsedTime: Float = 0f
        internal set
}

/**
 * Remember and create a MateriaCanvasState for managing canvas lifecycle.
 */
@Composable
expect fun rememberMateriaCanvasState(): MateriaCanvasState
