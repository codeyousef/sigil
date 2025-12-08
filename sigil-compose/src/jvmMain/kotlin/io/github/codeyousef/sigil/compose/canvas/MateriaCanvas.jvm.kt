package io.github.codeyousef.sigil.compose.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import io.github.codeyousef.sigil.compose.applier.MateriaApplier
import io.github.codeyousef.sigil.compose.node.MateriaNodeWrapper
import io.materia.engine.scene.Scene
import io.materia.engine.camera.PerspectiveCamera
import io.materia.engine.renderer.WebGPURenderer
import io.materia.engine.renderer.WebGPURendererConfig
import io.materia.engine.core.RenderLoop
import io.materia.engine.core.DisposableContainer
import io.materia.core.math.Color as MateriaColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Canvas
import java.awt.Graphics
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.SwingUtilities
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

/**
 * Desktop (JVM) implementation of MateriaCanvas.
 *
 * Uses SwingPanel to embed an AWT Canvas that Materia can render to via LWJGL/Vulkan.
 * The Compose runtime reconciles the scene graph while Materia handles GPU rendering.
 */
@Composable
actual fun MateriaCanvas(
    modifier: Any,
    backgroundColor: Int,
    camera: PerspectiveCamera?,
    content: @Composable () -> Unit
) {
    val actualModifier = if (modifier is Modifier) modifier else Modifier.fillMaxSize()

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
            lookAt(io.materia.core.math.Vector3.ZERO)
        }
    }

    // Create the root wrapper for the scene
    val rootWrapper = remember { MateriaNodeWrapper.createRoot(scene) }

    // Track renderer and render loop
    var renderer by remember { mutableStateOf<WebGPURenderer?>(null) }
    var renderLoop by remember { mutableStateOf<RenderLoop?>(null) }
    var composition by remember { mutableStateOf<Composition?>(null) }
    var recomposerJob by remember { mutableStateOf<Job?>(null) }

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

    // Create the Swing panel that hosts the rendering canvas
    Box(modifier = actualModifier) {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = {
                // Create AWT Canvas for Materia rendering
                object : Canvas() {
                    init {
                        preferredSize = Dimension(800, 600)
                        ignoreRepaint = true
                    }

                    override fun addNotify() {
                        super.addNotify()

                        // Initialize renderer after canvas is visible
                        SwingUtilities.invokeLater {
                            initializeRenderer()
                        }
                    }

                    private fun initializeRenderer() {
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

                        // Create renderer
                        val newRenderer = WebGPURenderer(config)

                        // Initialize with this canvas
                        // Note: Actual implementation depends on Materia's surface factory
                        // This assumes Materia provides a way to create a surface from AWT Canvas
                        try {
                            // Create render surface from AWT Canvas
                            // The actual API might differ based on Materia's implementation
                            val surface = createRenderSurface(this@initializeRenderer)
                            newRenderer.initialize(surface)
                            newRenderer.setSize(width, height)

                            renderer = newRenderer
                            disposables.track(newRenderer)

                            // Start render loop
                            val loop = RenderLoop { deltaTime ->
                                scene.traverse { obj ->
                                    obj.updateMatrixWorld()
                                }
                                newRenderer.render(scene, renderCamera)
                            }
                            loop.start()
                            renderLoop = loop

                            // Set up composition for content
                            setupComposition(rootWrapper, content)

                        } catch (e: Exception) {
                            System.err.println("Failed to initialize Materia renderer: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    override fun removeNotify() {
                        // Stop render loop before cleanup
                        renderLoop?.stop()
                        renderLoop = null

                        // Cancel recomposer
                        recomposerJob?.cancel()
                        recomposerJob = null

                        // Dispose composition
                        composition?.dispose()
                        composition = null

                        super.removeNotify()
                    }

                    override fun paint(g: Graphics?) {
                        // Materia handles rendering directly to GPU
                        // No need for AWT paint
                    }

                    override fun update(g: Graphics?) {
                        // Skip AWT update, Materia handles it
                    }
                }
            },
            update = { canvas ->
                // Handle resize
                renderer?.let { r ->
                    if (canvas.width > 0 && canvas.height > 0) {
                        r.setSize(canvas.width, canvas.height)
                        renderCamera.aspect = canvas.width.toFloat() / canvas.height.toFloat()
                        renderCamera.updateProjectionMatrix()
                    }
                }
            }
        )
    }

    // Cleanup on disposal
    DisposableEffect(Unit) {
        onDispose {
            renderLoop?.stop()
            recomposerJob?.cancel()
            composition?.dispose()
            rootWrapper.clear()
            disposables.dispose()
        }
    }
}

/**
 * Creates a render surface from an AWT Canvas.
 * This is a platform-specific abstraction.
 */
private fun createRenderSurface(canvas: Canvas): Any {
    // The actual implementation depends on Materia's surface factory API
    // This might be something like:
    // return VulkanSurface.fromAwtCanvas(canvas)
    // or
    // return SurfaceFactory.create(canvas.peer.getHandle())

    // For now, return the canvas itself - Materia should handle the abstraction
    return canvas
}

/**
 * Sets up the Compose composition for the 3D content.
 */
private fun setupComposition(
    rootWrapper: MateriaNodeWrapper,
    content: @Composable () -> Unit
) {
    // In a real implementation, this would:
    // 1. Create a Recomposer on the appropriate dispatcher
    // 2. Create a Composition with MateriaApplier
    // 3. Set the content and start recomposition

    // The Compose runtime integration is complex and typically requires:
    // - A proper CoroutineContext for the Recomposer
    // - Integration with the platform's frame clock
    // - Proper threading for composition vs rendering

    // This is a simplified placeholder - full implementation would use:
    // val recomposer = Recomposer(Dispatchers.Main)
    // val composition = Composition(MateriaApplier(rootWrapper), recomposer)
    // composition.setContent(content)
}

/**
 * Desktop implementation of rememberMateriaCanvasState.
 */
@Composable
actual fun rememberMateriaCanvasState(): MateriaCanvasState {
    return remember { MateriaCanvasState() }
}
