package io.github.codeyousef.sigil.compose.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.codeyousef.sigil.compose.applier.MateriaApplier
import io.github.codeyousef.sigil.compose.node.MateriaNodeWrapper
import io.materia.engine.scene.Scene
import io.materia.engine.camera.PerspectiveCamera
import io.materia.engine.renderer.WebGPURenderer
import io.materia.engine.renderer.WebGPURendererConfig
import io.materia.engine.core.RenderLoop
import io.materia.engine.core.DisposableContainer
import io.materia.core.math.Color as MateriaColor
import io.materia.core.math.Vector3
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * JavaScript/WebGPU implementation of MateriaCanvas.
 *
 * Creates an HTML Canvas element and initializes WebGPU rendering.
 * The Compose runtime reconciles the scene graph while Materia handles GPU rendering.
 */
@Composable
actual fun MateriaCanvas(
    modifier: Any,
    backgroundColor: Int,
    camera: PerspectiveCamera?,
    content: @Composable () -> Unit
) {
    // Create and remember the Materia scene
    val scene = remember { Scene() }

    // Create or use provided camera
    val renderCamera = remember(camera) {
        camera ?: PerspectiveCamera(
            fov = 75f,
            aspect = 16f / 9f,
            near = 0.1f,
            far = 1000f
        ).apply {
            position.set(0f, 2f, 5f)
            lookAt(Vector3.ZERO)
        }
    }

    // Create the root wrapper for the scene
    val rootWrapper = remember { MateriaNodeWrapper.createRoot(scene) }

    // Track renderer and render loop
    var renderer by remember { mutableStateOf<WebGPURenderer?>(null) }
    var renderLoop by remember { mutableStateOf<RenderLoop?>(null) }
    var canvasElement by remember { mutableStateOf<HTMLCanvasElement?>(null) }

    // Disposable container for cleanup
    val disposables = remember { DisposableContainer() }

    // Set scene background color
    LaunchedEffect(backgroundColor) {
        scene.background = MateriaColor(
            ((backgroundColor shr 16) and 0xFF) / 255f,
            ((backgroundColor shr 8) and 0xFF) / 255f,
            (backgroundColor and 0xFF) / 255f
        )
    }

    // Initialize canvas and renderer
    LaunchedEffect(Unit) {
        // Create canvas element
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.id = "sigil-materia-canvas"
        canvas.style.width = "100%"
        canvas.style.height = "100%"

        // Find or create container
        val container = document.getElementById("sigil-container")
            ?: document.body?.appendChild(
                document.createElement("div").apply {
                    id = "sigil-container"
                    setAttribute("style", "width: 100%; height: 100%;")
                }
            )

        container?.appendChild(canvas)
        canvasElement = canvas

        // Set canvas size based on container
        val rect = canvas.getBoundingClientRect()
        canvas.width = rect.width.toInt()
        canvas.height = rect.height.toInt()

        // Create renderer configuration
        val config = WebGPURendererConfig(
            depthTest = true,
            clearColor = MateriaColor(
                ((backgroundColor shr 16) and 0xFF) / 255f,
                ((backgroundColor shr 8) and 0xFF) / 255f,
                (backgroundColor and 0xFF) / 255f
            ),
            antialias = 4
        )

        try {
            // Create renderer
            val newRenderer = WebGPURenderer(config)

            // Create render surface from canvas
            val surface = CanvasRenderSurface(canvas)
            newRenderer.initialize(surface)
            newRenderer.setSize(canvas.width, canvas.height)

            renderer = newRenderer
            disposables.track(newRenderer)

            // Update camera aspect ratio
            renderCamera.aspect = canvas.width.toFloat() / canvas.height.toFloat()
            renderCamera.updateProjectionMatrix()

            // Start render loop
            val loop = RenderLoop { deltaTime ->
                scene.traverse { obj ->
                    obj.updateMatrixWorld()
                }
                newRenderer.render(scene, renderCamera)
            }
            loop.start()
            renderLoop = loop

        } catch (e: Exception) {
            console.error("Failed to initialize Materia WebGPU renderer: ${e.message}")
        }
    }

    // Handle window resize
    LaunchedEffect(Unit) {
        val resizeHandler: (dynamic) -> Unit = { _ ->
            canvasElement?.let { canvas ->
                val rect = canvas.getBoundingClientRect()
                canvas.width = rect.width.toInt()
                canvas.height = rect.height.toInt()

                renderer?.setSize(canvas.width, canvas.height)
                renderCamera.aspect = canvas.width.toFloat() / canvas.height.toFloat()
                renderCamera.updateProjectionMatrix()
            }
        }

        window.addEventListener("resize", resizeHandler)
    }

    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            renderLoop?.stop()
            rootWrapper.clear()
            disposables.dispose()
            canvasElement?.remove()
        }
    }
}

/**
 * Wrapper for creating a render surface from an HTML Canvas.
 * This integrates with Materia's WebGPU surface abstraction.
 */
external class CanvasRenderSurface(canvas: HTMLCanvasElement)

/**
 * JavaScript implementation of rememberMateriaCanvasState.
 */
@Composable
actual fun rememberMateriaCanvasState(): MateriaCanvasState {
    return remember { MateriaCanvasState() }
}
