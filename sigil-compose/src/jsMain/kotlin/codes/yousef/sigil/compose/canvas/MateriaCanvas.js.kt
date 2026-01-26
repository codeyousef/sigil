package codes.yousef.sigil.compose.canvas

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Recomposer
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
import io.materia.renderer.SurfaceFactory
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * JavaScript/WebGPU implementation of MateriaCanvas.
 */
@Composable
actual fun MateriaCanvas(
    modifier: Any,
    backgroundColor: Int,
    camera: PerspectiveCamera?,
    content: @Composable () -> Unit
) {
    val scene = remember { Scene() }
    val canvasState = rememberMateriaCanvasState()
    val lightingContext = remember { MateriaLightingContext() }
    val scope = rememberCoroutineScope()

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

    val rootWrapper = remember { MateriaNodeWrapper.createRoot(scene) }
    val applier = remember { MateriaApplier(rootWrapper) }
    val frameClock = remember { BroadcastFrameClock() }
    val recomposer = remember { Recomposer(scope.coroutineContext + frameClock) }
    val composition = remember { Composition(applier, recomposer) }
    val currentContent by rememberUpdatedState(content)

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

    var renderer by remember { mutableStateOf<Renderer?>(null) }
    var canvasElement by remember { mutableStateOf<HTMLCanvasElement?>(null) }

    LaunchedEffect(Unit) {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.id = "sigil-materia-canvas"
        canvas.style.width = "100%"
        canvas.style.height = "100%"

        val container = document.getElementById("sigil-container")
            ?: document.body?.appendChild(
                document.createElement("div").apply {
                    id = "sigil-container"
                    setAttribute("style", "width: 100%; height: 100%;")
                }
            )

        container?.appendChild(canvas)
        canvasElement = canvas

        val rect = canvas.getBoundingClientRect()
        canvas.width = rect.width.toInt()
        canvas.height = rect.height.toInt()

        canvasState.scene = scene
        canvasState.camera = renderCamera
        canvasState.canvas = canvas

        val surface = SurfaceFactory.create(canvas)
        val result = RendererFactory.create(surface, RendererConfig())

        when (result) {
            is io.materia.core.Result.Success -> {
                renderer = result.value
                canvasState.renderer = result.value
                canvasState.isInitialized = true
                result.value.resize(canvas.width, canvas.height)
            }
            is io.materia.core.Result.Error -> {
                console.error("Failed to initialize Materia renderer: ${result.message}")
            }
        }
    }

    DisposableEffect(canvasElement, renderer) {
        val canvas = canvasElement
        if (canvas == null) {
            return@DisposableEffect onDispose {}
        }

        val resizeHandler: (dynamic) -> Unit = {
            val rect = canvas.getBoundingClientRect()
            val width = rect.width.toInt().coerceAtLeast(1)
            val height = rect.height.toInt().coerceAtLeast(1)
            canvas.width = width
            canvas.height = height
            renderCamera.aspect = width.toFloat() / height.toFloat()
            renderCamera.updateProjectionMatrix()
            renderer?.resize(width, height)
            Unit
        }

        window.addEventListener("resize", resizeHandler)
        resizeHandler(Unit)

        onDispose {
            window.removeEventListener("resize", resizeHandler)
        }
    }

    DisposableEffect(renderer) {
        val activeRenderer = renderer ?: return@DisposableEffect onDispose {}
        var frameId = 0
        var lastTime = 0.0

        fun renderFrame(time: Double) {
            if (lastTime == 0.0) {
                lastTime = time
            }
            val deltaSeconds = ((time - lastTime) / 1000.0).toFloat()
            lastTime = time

            frameClock.sendFrame((time * 1_000_000).toLong())

            canvasState.controlsSnapshot().forEach { controls ->
                controls.update(deltaSeconds)
            }

            lightingContext.applyToScene(scene)
            scene.updateMatrixWorld(true)
            renderCamera.updateMatrixWorld()
            renderCamera.updateProjectionMatrix()
            activeRenderer.render(scene, renderCamera)

            canvasState.fps = activeRenderer.stats.fps.toFloat()
            canvasState.elapsedTime += deltaSeconds

            frameId = window.requestAnimationFrame { newTime ->
                renderFrame(newTime)
            }
        }

        frameId = window.requestAnimationFrame { time -> renderFrame(time) }

        onDispose {
            window.cancelAnimationFrame(frameId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            renderer?.dispose()
            renderer = null
            canvasState.renderer = null
            canvasState.isInitialized = false
            canvasElement?.remove()
            canvasElement = null
        }
    }
}

/**
 * JavaScript implementation of rememberMateriaCanvasState.
 */
@Composable
actual fun rememberMateriaCanvasState(): MateriaCanvasState {
    return remember { MateriaCanvasState() }
}
