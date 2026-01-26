package codes.yousef.sigil.compose.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.materia.camera.PerspectiveCamera
import io.materia.controls.CameraControls
import io.materia.core.scene.Scene
import io.materia.renderer.Renderer

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
     * Platform canvas handle (HTMLCanvasElement on JS, AWT Canvas on JVM).
     */
    var canvas: Any? by mutableStateOf(null)
        internal set

    /**
     * Active renderer instance.
     */
    var renderer: Renderer? = null
        internal set

    /**
     * Active camera used for rendering.
     */
    var camera: PerspectiveCamera? by mutableStateOf(null)
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

    private val controls = mutableSetOf<CameraControls>()

    internal fun registerControls(controls: CameraControls) {
        this.controls.add(controls)
    }

    internal fun unregisterControls(controls: CameraControls) {
        this.controls.remove(controls)
    }

    internal fun controlsSnapshot(): List<CameraControls> = controls.toList()
}

/**
 * Remember and create a MateriaCanvasState for managing canvas lifecycle.
 */
@Composable
expect fun rememberMateriaCanvasState(): MateriaCanvasState
