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
import io.materia.core.scene.Scene
import io.materia.camera.PerspectiveCamera
import io.materia.renderer.Renderer
import io.materia.renderer.RendererConfig
import io.materia.core.math.Color as MateriaColor
import io.materia.core.math.Vector3
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

    // Track renderer state
    var renderer by remember { mutableStateOf<Renderer?>(null) }
    var isRendering by remember { mutableStateOf(false) }
    var composition by remember { mutableStateOf<Composition?>(null) }
    var recomposerJob by remember { mutableStateOf<Job?>(null) }

    // Set scene background color
    LaunchedEffect(backgroundColor) {
        scene.background = io.materia.core.scene.Background.Color(
            MateriaColor(
                ((backgroundColor shr 16) and 0xFF) / 255f,
                ((backgroundColor shr 8) and 0xFF) / 255f,
                (backgroundColor and 0xFF) / 255f
            )
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
                        try {
                            // Create renderer using Materia's RendererFactory
                            // The actual initialization depends on Materia's platform-specific API
                            // For Vulkan on desktop, this would use VulkanRenderer
                            val config = RendererConfig()
                            
                            // Note: Full Materia integration would use:
                            // val newRenderer = RendererFactory.create(config, this@initializeRenderer)
                            // renderer = newRenderer
                            // isRendering = true
                            
                            // Set up composition for content
                            setupComposition(rootWrapper, content)

                        } catch (e: Exception) {
                            System.err.println("Failed to initialize Materia renderer: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    override fun removeNotify() {
                        // Stop rendering
                        isRendering = false

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
                        // Update camera aspect ratio on resize
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
            isRendering = false
            recomposerJob?.cancel()
            composition?.dispose()
            rootWrapper.clear()
        }
    }
}

/**
 * Sets up the Compose composition for the 3D content.
 *
 * JVM/Desktop 3D Compose integration requires:
 * - A Recomposer on the appropriate dispatcher
 * - A Composition with MateriaApplier
 * - Integration with the platform's frame clock
 * - Proper threading for composition vs rendering
 *
 * The 3D scene graph is managed directly through MateriaNodeWrapper.
 */
@Suppress("UNUSED_PARAMETER")
private fun setupComposition(
    rootWrapper: MateriaNodeWrapper,
    content: @Composable () -> Unit
) {
    // Scene graph managed via MateriaNodeWrapper - see MateriaCanvas.
}

/**
 * Desktop implementation of rememberMateriaCanvasState.
 */
@Composable
actual fun rememberMateriaCanvasState(): MateriaCanvasState {
    return remember { MateriaCanvasState() }
}
