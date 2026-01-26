package codes.yousef.sigil.compose.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import codes.yousef.sigil.compose.applier.MateriaApplier
import codes.yousef.sigil.compose.context.LocalMateriaCanvasState
import codes.yousef.sigil.compose.context.LocalMateriaLightingContext
import codes.yousef.sigil.compose.context.MateriaLightingContext
import codes.yousef.sigil.compose.node.MateriaNodeWrapper
import io.materia.camera.PerspectiveCamera
import io.materia.core.math.Color as MateriaColor
import io.materia.core.math.Vector3
import io.materia.core.scene.Background
import io.materia.core.scene.Scene
import io.materia.renderer.Renderer
import io.materia.renderer.RendererConfig
import io.materia.renderer.RendererFactory
import io.materia.renderer.vulkan.VulkanSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.lwjgl.glfw.GLFW.GLFW_CLIENT_API
import org.lwjgl.glfw.GLFW.GLFW_NO_API
import org.lwjgl.glfw.GLFW.GLFW_RESIZABLE
import org.lwjgl.glfw.GLFW.GLFW_TRUE
import org.lwjgl.glfw.GLFW.GLFW_VISIBLE
import org.lwjgl.glfw.GLFW.glfwCreateWindow
import org.lwjgl.glfw.GLFW.glfwDestroyWindow
import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwSetWindowSize
import org.lwjgl.glfw.GLFW.glfwWindowHint
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import java.awt.Canvas
import java.awt.Dimension
import javax.swing.SwingUtilities
import androidx.compose.runtime.Recomposer

private var glfwInitialized = false

private class JvmRenderState {
    var windowHandle: Long = 0L
    var surface: VulkanSurface? = null
    var renderer: Renderer? = null
    var renderJob: Job? = null
    var running: Boolean = false
}

/**
 * Desktop (JVM) implementation of MateriaCanvas.
 */
@Composable
actual fun MateriaCanvas(
    modifier: Any,
    backgroundColor: Int,
    camera: PerspectiveCamera?,
    content: @Composable () -> Unit
) {
    val actualModifier = if (modifier is Modifier) modifier else Modifier.fillMaxSize()
    val scene = remember { Scene() }
    val canvasState = rememberMateriaCanvasState()
    val lightingContext = remember { MateriaLightingContext() }
    val scope = rememberCoroutineScope()
    val renderScope = remember { CoroutineScope(Dispatchers.Default) }
    val frameClock = remember { BroadcastFrameClock() }
    val recomposer = remember { Recomposer(scope.coroutineContext + frameClock) }
    val rootWrapper = remember { MateriaNodeWrapper.createRoot(scene) }
    val composition = remember { Composition(MateriaApplier(rootWrapper), recomposer) }
    val currentContent by rememberUpdatedState(content)
    val renderState = remember { JvmRenderState() }

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

    LaunchedEffect(renderCamera) {
        canvasState.camera = renderCamera
    }

    DisposableEffect(composition, canvasState, lightingContext) {
        composition.setContent {
            CompositionLocalProvider(
                LocalMateriaLightingContext provides lightingContext,
                LocalMateriaCanvasState provides canvasState
            ) {
                currentContent()
            }
        }

        onDispose {
            composition.dispose()
            lightingContext.dispose()
        }
    }

    LaunchedEffect(recomposer) {
        recomposer.runRecomposeAndApplyChanges()
    }

    LaunchedEffect(backgroundColor) {
        scene.background = Background.Color(
            MateriaColor(
                ((backgroundColor shr 16) and 0xFF) / 255f,
                ((backgroundColor shr 8) and 0xFF) / 255f,
                (backgroundColor and 0xFF) / 255f
            )
        )
    }

    Box(modifier = actualModifier) {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = {
                object : Canvas() {
                    init {
                        preferredSize = Dimension(800, 600)
                        ignoreRepaint = true
                    }

                    override fun addNotify() {
                        super.addNotify()
                        canvasState.canvas = this

                        SwingUtilities.invokeLater {
                            initializeRenderer(this)
                        }
                    }

                    override fun removeNotify() {
                        shutdownRenderer()
                        super.removeNotify()
                    }
                }
            },
            update = { canvas ->
                canvasState.canvas = canvas
                if (renderState.windowHandle != 0L && canvas.width > 0 && canvas.height > 0) {
                    glfwSetWindowSize(renderState.windowHandle, canvas.width, canvas.height)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            shutdownRenderer()
            renderScope.cancel()
            canvasState.isInitialized = false
            canvasState.renderer = null
        }
    }

    fun initializeRenderer(canvas: Canvas) {
        if (renderState.renderer != null || renderState.running) return

        if (!glfwInitialized) {
            if (!glfwInit()) {
                System.err.println("Sigil: Failed to initialize GLFW for Vulkan rendering.")
                return
            }
            glfwInitialized = true
        }

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)

        val width = canvas.width.coerceAtLeast(1)
        val height = canvas.height.coerceAtLeast(1)
        val windowHandle = glfwCreateWindow(width, height, "Sigil Materia", 0L, 0L)
        if (windowHandle == 0L) {
            System.err.println("Sigil: Failed to create GLFW window for Vulkan rendering.")
            return
        }

        renderState.windowHandle = windowHandle
        renderState.surface = VulkanSurface(windowHandle)
        renderState.running = true

        renderState.renderJob = renderScope.launch {
            val surface = renderState.surface ?: return@launch
            val result = RendererFactory.create(surface, RendererConfig())
            when (result) {
                is io.materia.core.Result.Success -> {
                    val renderer = result.value
                    renderState.renderer = renderer
                    canvasState.renderer = renderer
                    canvasState.scene = scene
                    canvasState.isInitialized = true

                    runRenderLoop(renderer, surface)
                }
                is io.materia.core.Result.Error -> {
                    System.err.println("Sigil: Failed to initialize renderer: ${result.message}")
                }
            }
        }
    }

    fun runRenderLoop(renderer: Renderer, surface: VulkanSurface) {
        var lastTime = System.nanoTime()
        var lastWidth = 0
        var lastHeight = 0

        while (renderState.running && !glfwWindowShouldClose(renderState.windowHandle)) {
            val now = System.nanoTime()
            val deltaSeconds = ((now - lastTime) / 1_000_000_000.0).toFloat()
            lastTime = now

            frameClock.sendFrame(now)

            canvasState.controlsSnapshot().forEach { controls ->
                controls.update(deltaSeconds)
            }

            lightingContext.applyToScene(scene)
            scene.updateMatrixWorld(true)
            renderCamera.updateMatrixWorld()
            renderCamera.updateProjectionMatrix()
            renderer.render(scene, renderCamera)

            canvasState.fps = renderer.stats.fps.toFloat()
            canvasState.elapsedTime += deltaSeconds

            val (width, height) = surface.getFramebufferSize()
            if (width != lastWidth || height != lastHeight) {
                renderer.resize(width, height)
                renderCamera.aspect = width.toFloat() / height.toFloat()
                renderCamera.updateProjectionMatrix()
                lastWidth = width
                lastHeight = height
            }

            glfwPollEvents()
            Thread.yield()
        }
    }

    fun shutdownRenderer() {
        renderState.running = false
        renderState.renderJob?.cancel()
        renderState.renderJob = null

        renderState.renderer?.dispose()
        renderState.renderer = null
        canvasState.renderer = null
        canvasState.isInitialized = false

        if (renderState.windowHandle != 0L) {
            glfwDestroyWindow(renderState.windowHandle)
            renderState.windowHandle = 0L
        }

        renderState.surface = null
    }
}

/**
 * Desktop implementation of rememberMateriaCanvasState.
 */
@Composable
actual fun rememberMateriaCanvasState(): MateriaCanvasState {
    return remember { MateriaCanvasState() }
}
