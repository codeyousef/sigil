package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.SigilScene
import codes.yousef.sigil.schema.SigilNodeData
import codes.yousef.sigil.schema.MeshData
import codes.yousef.sigil.schema.ModelData
import codes.yousef.sigil.schema.ModelMaterialOverride
import codes.yousef.sigil.schema.TextAlignMode
import codes.yousef.sigil.schema.TextBaselineMode
import codes.yousef.sigil.schema.TextData
import codes.yousef.sigil.schema.TextFacingMode
import codes.yousef.sigil.schema.GroupData
import codes.yousef.sigil.schema.LightData
import codes.yousef.sigil.schema.CameraData
import codes.yousef.sigil.schema.ControlsData
import codes.yousef.sigil.schema.ControlsType
import codes.yousef.sigil.schema.AnimationEasing
import codes.yousef.sigil.schema.AnimationKind
import codes.yousef.sigil.schema.AnimationTrigger
import codes.yousef.sigil.schema.AudioBusData
import codes.yousef.sigil.schema.AudioData
import codes.yousef.sigil.schema.AudioPatch
import codes.yousef.sigil.schema.AudioPatchAction
import codes.yousef.sigil.schema.CameraPatch
import codes.yousef.sigil.schema.CursorHint
import codes.yousef.sigil.schema.FrameStatsTextData
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.GeometryParams
import codes.yousef.sigil.schema.HighlightPatch
import codes.yousef.sigil.schema.InteractionMetadata
import codes.yousef.sigil.schema.LightType
import codes.yousef.sigil.schema.ProceduralAudioData
import codes.yousef.sigil.schema.ProceduralWaveform
import codes.yousef.sigil.schema.RendererPreference
import codes.yousef.sigil.schema.ScreenLayerData
import codes.yousef.sigil.schema.SceneAnimationData
import codes.yousef.sigil.schema.SceneNodePatch
import codes.yousef.sigil.schema.ScenePatch
import codes.yousef.sigil.schema.SceneSettings
import codes.yousef.sigil.schema.SigilJson
import io.materia.core.scene.Intersection
import io.materia.core.scene.Material
import io.materia.core.scene.Raycaster
import io.materia.core.scene.Scene
import io.materia.core.scene.Object3D
import io.materia.core.scene.Mesh
import io.materia.core.scene.Group
import io.materia.core.scene.Background
import io.materia.core.math.Color
import io.materia.core.math.Ray
import io.materia.core.math.Vector2
import io.materia.core.math.Vector3
import io.materia.material.Material as BaseMaterial
import io.materia.material.MeshBasicMaterial
import io.materia.material.MeshStandardMaterial
import io.materia.material.Side
import io.materia.audio.Audio as MateriaAudio
import io.materia.audio.AudioListener
import io.materia.audio.AudioWaveform
import io.materia.audio.BrowserAudioEngine
import io.materia.audio.PositionalAudio
import io.materia.audio.ProceduralAudioSpec
import io.materia.camera.Camera
import io.materia.camera.OrthographicCamera
import io.materia.camera.PerspectiveCamera
import io.materia.controls.ControlsConfig
import io.materia.controls.FirstPersonControls
import io.materia.controls.Key
import io.materia.controls.OrbitControls
import io.materia.controls.PointerButton
import io.materia.geometry.primitives.BoxGeometry
import io.materia.geometry.primitives.SphereGeometry
import io.materia.geometry.primitives.PlaneGeometry
import io.materia.geometry.primitives.CylinderGeometry
import io.materia.geometry.ConeGeometry
import io.materia.geometry.primitives.TorusGeometry
import io.materia.geometry.CircleGeometry
import io.materia.geometry.primitives.RingGeometry
import io.materia.geometry.IcosahedronGeometry
import io.materia.geometry.OctahedronGeometry
import io.materia.geometry.TetrahedronGeometry
import io.materia.geometry.DodecahedronGeometry
import io.materia.geometry.BufferGeometry
import io.materia.geometry.TextAlign as MateriaTextAlign
import io.materia.geometry.TextBaseline as MateriaTextBaseline
import io.materia.geometry.TextGeometry
import io.materia.geometry.TextOptions
import io.materia.loader.AssetResolver
import io.materia.loader.GLTFLoader
import io.materia.lighting.AmbientLightImpl
import io.materia.lighting.DirectionalLightImpl
import io.materia.lighting.PointLightImpl
import io.materia.lighting.SpotLightImpl
import io.materia.lighting.HemisphereLightImpl
import io.materia.lighting.Light
import io.materia.lighting.DefaultLightingSystem
import io.materia.renderer.Renderer
import io.materia.renderer.OverlayFirstPicker
import io.materia.renderer.RenderOverlayLayer
import io.materia.renderer.TextureFilter
import io.materia.renderer.renderWithOverlays
import io.materia.renderer.webgpu.WebGPURenderer
import io.materia.renderer.webgl.WebGLRenderer
import io.materia.renderer.RendererConfig
import io.materia.texture.Texture
import io.materia.texture.Texture2D
import io.materia.performance.AdaptiveResolutionConfig
import io.materia.performance.AdaptiveResolutionController
import io.materia.performance.FrameStatsSmoother
import io.materia.performance.SmoothedFrameStats
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.js.ExperimentalJsExport
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private val scope = MainScope()
private const val SIGIL_GLTF_CACHE_SCOPE = "codes.yousef.sigil.gltf"
private const val SIGIL_MODEL_DATA_KEY = "sigilModelData"
private const val SIGIL_MODEL_LOAD_STATE_KEY = "sigilModelLoadState"
private const val SIGIL_TEXT_DATA_KEY = "sigilTextData"
private const val SIGIL_TEXT_GENERATION_KEY = "sigilTextGeneration"
private const val SIGIL_HIGHLIGHT_PATCH_KEY = "sigilHighlightPatch"

private data class ActiveSceneAnimation(
    val node: Object3D,
    val data: SceneAnimationData,
    val startedAtMs: Double,
    val basePosition: List<Float>,
    val baseScale: List<Float>,
    val baseVisible: Boolean
)

private data class ActiveDrag(
    val source: Object3D,
    val sourceInteraction: InteractionMetadata,
    val constraint: SigilDragSession,
    var target: Object3D? = null,
    var targetState: String? = null,
    var targetAccepted: Boolean? = null,
    var dropResult: String? = null
)

private data class PendingDrag(
    val source: Object3D,
    val sourceInteraction: InteractionMetadata,
    val constraint: SigilDragSession,
    val startIntersection: Intersection?
)

private data class DropEvaluation(
    val accepted: Boolean,
    val result: String
)

private data class RuntimeScreenLayer(
    val data: ScreenLayerData,
    val scene: Scene,
    val camera: OrthographicCamera,
    val root: Group
)

internal data class SigilCanvasDisplayStyle(
    val width: String?,
    val height: String?
) {
    fun restore(canvas: HTMLCanvasElement) {
        width?.let { canvas.style.width = it }
        height?.let { canvas.style.height = it }
    }

    companion object {
        fun capture(canvas: HTMLCanvasElement): SigilCanvasDisplayStyle = SigilCanvasDisplayStyle(
            width = canvas.style.width.takeIf { it.isNotBlank() },
            height = canvas.style.height.takeIf { it.isNotBlank() }
        )
    }
}

private data class RuntimeFrameStatsText(
    val data: FrameStatsTextData,
    val node: Group,
    var lastText: String = "",
    var lastUpdateMs: Double = 0.0
)

private data class ActiveCameraPatch(
    val startedAtMs: Double,
    val durationMs: Int,
    val startPosition: Vector3,
    val startTarget: Vector3,
    val endPosition: Vector3,
    val endTarget: Vector3,
    val easing: AnimationEasing
)

private class SigilRelativeAssetResolver(
    private val modelUrl: String,
    private val delegate: AssetResolver = AssetResolver.default()
) : AssetResolver {
    override val cacheKeyScope: String? = SIGIL_GLTF_CACHE_SCOPE

    private var transformedGlbJson: String? = null

    override suspend fun load(uri: String, basePath: String?): ByteArray {
        if (uri == modelUrl) return loadModelDocument(basePath)
        val resolved = SigilGltfMetadata.resolveAssetPath(uri, basePath, modelUrl)
        return loadDelegateWithRetry(resolved, null)
    }

    private suspend fun loadModelDocument(basePath: String?): ByteArray {
        if (!SigilGltfMetadata.isGlbUrl(modelUrl)) return loadDelegateWithRetry(modelUrl, basePath)

        val json = transformedGlbJson ?: SigilGltfMetadata.glbToGltfJson(loadDelegateWithRetry(modelUrl, basePath))
            .also { transformedGlbJson = it }
        return json.encodeToByteArray()
    }

    private suspend fun loadDelegateWithRetry(uri: String, basePath: String?): ByteArray {
        var lastFailure: Throwable? = null
        repeat(4) { attempt ->
            try {
                return delegate.load(uri, basePath)
            } catch (t: Throwable) {
                lastFailure = t
                if (attempt < 3) delay((attempt + 1) * 120L)
            }
        }
        throw lastFailure ?: IllegalStateException("Asset load failed for $uri")
    }
}

/**
 * Hydrator class that creates Materia objects from schema data.
 */
class SigilHydrator(
    canvas: HTMLCanvasElement,
    private val sceneData: SigilScene,
    private val sceneEventBindings: List<SigilSceneEventBinding> = emptyList(),
    private val localSceneEventHandlers: Map<String, () -> Unit> = emptyMap()
) {
    private var canvas: HTMLCanvasElement = canvas
    private val authoredCanvasDisplayStyle = SigilCanvasDisplayStyle.capture(canvas)
    private val materiaScene = Scene()
    private val lightingSystem = DefaultLightingSystem()
    private var renderer: Renderer? = null
    private var animationFrameId: Int = 0
    private var running = false
    private var camera: PerspectiveCamera? = null
    private val nodeMap = mutableMapOf<String, Object3D>()
    private val interactionNodeMap = mutableMapOf<String, Object3D>()
    private val nodeDataMap = mutableMapOf<String, SigilNodeData>()
    private val lights = mutableListOf<Light>()
    private var orbitControls: OrbitControls? = null
    private var firstPersonControls: FirstPersonControls? = null
    private var controlsCleanup: (() -> Unit)? = null
    private var interactionCleanup: (() -> Unit)? = null
    private val raycaster = Raycaster()
    private val dragGesture = SigilDragGestureTracker()
    private var pendingDrag: PendingDrag? = null
    private var activeDrag: ActiveDrag? = null
    private var hoverDropTarget: Object3D? = null
    private val activeAnimations = mutableListOf<ActiveSceneAnimation>()
    private val billboardTextNodes = mutableListOf<Object3D>()
    private val screenLayers = mutableListOf<RuntimeScreenLayer>()
    private val overlayRenderLayers = mutableListOf<RenderOverlayLayer>()
    private val frameStatsTextNodes = mutableListOf<RuntimeFrameStatsText>()
    private val overlayFirstPicker = OverlayFirstPicker()
    private val requestGate = SigilRequestGate()
    private val modelSwapTracker = SigilModelSwapTracker()
    private val persistence = SigilPersistenceRuntime.browser()
    private val audioSources = mutableMapOf<String, MateriaAudio>()
    private val audioBuses = mutableMapOf<String, AudioBusData>()
    private var audioListener: AudioListener? = null
    private var activeCameraPatch: ActiveCameraPatch? = null
    private val baseColorTextureMetadataCache = mutableMapOf<String, List<GltfBaseColorTexture>>()
    private val pendingBaseColorHydrations = mutableMapOf<String, CompletableDeferred<Unit>>()
    private val frameStatsSmoother = FrameStatsSmoother(
        sceneData.settings.adaptiveResolution?.sampleWindow ?: 60
    )
    private val adaptiveResolutionController = sceneData.settings.adaptiveResolution
        ?.takeIf { it.enabled }
        ?.let { data ->
            AdaptiveResolutionController(
                config = AdaptiveResolutionConfig(
                    targetFps = data.targetFps.toDouble(),
                    minimumScale = data.minimumDpr,
                    maximumScale = data.maximumDpr,
                    scaleStep = data.scaleStep
                ),
                initialScale = window.devicePixelRatio.toFloat().coerceIn(data.minimumDpr, data.maximumDpr)
            )
        }
    private var renderScale = adaptiveResolutionController?.scale ?: 1f
    private var lastFrameTimeMs: Double = 0.0
    private var lastAdaptiveResolutionCheckMs: Double = 0.0
    private var rendererCanvasMayNeedReplacement = false

    suspend fun initialize() {
        applySceneSettings(sceneData.settings)

        val initialSize = displaySize()
        camera = PerspectiveCamera(
            fov = 75f,
            aspect = initialSize.first.toFloat() / initialSize.second.toFloat(),
            near = 0.1f,
            far = 1000f
        ).apply {
            position.set(0f, 2f, 5f)
            lookAt(Vector3.ZERO)
        }
        audioListener = AudioListener(camera)
        BrowserAudioEngine.unlockOnFirstGesture()
        BrowserAudioEngine.installVisibilityHandling()

        for (nodeData in sceneData.rootNodes) {
            if (nodeData is ScreenLayerData) {
                createScreenLayer(nodeData)
            } else {
                val materiaNode = createMateriaNode(nodeData)
                if (materiaNode != null) {
                    materiaScene.add(materiaNode)
                    registerNode(nodeData, materiaNode)
                }
            }
        }
        materiaScene.updateMatrixWorld(true)
        resizeForDisplay()

        val initialized = initializeRenderer(RendererConfig())

        if (!initialized) {
            console.error("Sigil: No renderer could be initialized (tried WebGPU and WebGL)")
            return
        }

        interactionCleanup = attachInteractionHandlers()
        triggerAnimations(AnimationTrigger.SCENE_LOAD)

        window.onresize = {
            resizeForDisplay()
            Unit
        }
    }

    private suspend fun initializeRenderer(config: RendererConfig): Boolean {
        val rendererOverride = rendererOverride()
        val preferWebGl = SigilRendererPolicy.preferWebGlFirst(
            userAgent = window.navigator.userAgent,
            webdriver = (window.navigator.asDynamic().webdriver as? Boolean) == true,
            rendererOverride = rendererOverride
        )

        if (preferWebGl || shouldAvoidSoftwareWebGpu(rendererOverride)) {
            console.log("Sigil: Preferring WebGL renderer for this browser environment")
            if (initializeWebGlRenderer(config, fallback = false)) return true
            return initializeWebGpuRenderer(config)
        }

        val webGpuInitialized = initializeWebGpuRenderer(config)
        if (webGpuInitialized) return true

        if (rendererCanvasMayNeedReplacement) {
            replaceCanvasForRendererFallback()
            reattachControlsFromSceneData()
            rendererCanvasMayNeedReplacement = false
        }
        return initializeWebGlRenderer(config, fallback = true)
    }

    private suspend fun shouldAvoidSoftwareWebGpu(rendererOverride: String?): Boolean {
        when (rendererOverride?.trim()?.lowercase()) {
            "webgpu", "gpu" -> return false
            "webgl", "webgl2" -> return true
        }

        val gpu = window.navigator.asDynamic().gpu ?: return false
        return try {
            val adapterPromise = gpu.requestAdapter().unsafeCast<kotlin.js.Promise<dynamic>>()
            val adapter = adapterPromise.await() ?: return false
            val info = adapter.info
            val summary = listOf(
                dynamicText(adapter.name),
                dynamicText(info?.vendor),
                dynamicText(info?.architecture),
                dynamicText(info?.device),
                dynamicText(info?.description)
            ).filter { it.isNotBlank() }.joinToString(" ")
            val isSoftware = SigilRendererPolicy.isSoftwareWebGpuAdapter(summary)
            if (isSoftware) {
                console.warn("Sigil: WebGPU adapter appears to be software-backed ($summary); preferring WebGL")
            }
            isSoftware
        } catch (_: Throwable) {
            false
        }
    }

    private fun dynamicText(value: dynamic): String =
        if (value == null || value == undefined) "" else value.toString()

    private suspend fun initializeWebGpuRenderer(config: RendererConfig): Boolean {
        try {
            val gpu = js("navigator.gpu")
            if (gpu == null || gpu == undefined) {
                console.log("Sigil: WebGPU not available, trying WebGL fallback...")
                return false
            }

            rendererCanvasMayNeedReplacement = true
            val r = WebGPURenderer(canvas)
            val result = r.initialize(config)
            return when (result) {
                is io.materia.core.Result.Success -> {
                    rendererCanvasMayNeedReplacement = false
                    renderer = r
                    configureInitializedRenderer(r)
                    console.log("Sigil: Initialized WebGPU renderer")
                    true
                }
                is io.materia.core.Result.Error -> {
                    console.warn("Sigil: WebGPU initialization failed (${result.message}), trying WebGL fallback...")
                    r.dispose()
                    false
                }
            }
        } catch (e: Throwable) {
            console.warn("Sigil: WebGPU error (${e.message}), trying WebGL fallback...")
            return false
        }
    }

    private suspend fun initializeWebGlRenderer(config: RendererConfig, fallback: Boolean): Boolean {
        try {
            val r = WebGLRenderer(canvas)
            val result = r.initialize(config)
            return when (result) {
                is io.materia.core.Result.Success -> {
                    renderer = r
                    configureInitializedRenderer(r)
                    val suffix = if (fallback) " (fallback)" else ""
                    console.log("Sigil: Initialized WebGL renderer$suffix")
                    true
                }
                is io.materia.core.Result.Error -> {
                    val prefix = if (fallback) "Sigil: WebGL initialization also failed" else "Sigil: WebGL initialization failed"
                    console.error("$prefix: ${result.message}")
                    r.dispose()
                    false
                }
            }
        } catch (e: Throwable) {
            val prefix = if (fallback) "Sigil: WebGL fallback error" else "Sigil: WebGL error"
            console.error("$prefix: ${e.message}")
            return false
        }
    }

    private fun configureInitializedRenderer(renderer: Renderer) {
        resizeBackingStore(renderer, canvas.width, canvas.height)
        (renderer as? WebGPURenderer)?.clearColor = intToColor(sceneData.settings.backgroundColor)
        if (renderer is WebGLRenderer) {
            configureSceneTextureFidelity()
        }
    }

    private fun rendererOverride(): String? {
        val globalOverride = window.asDynamic().__SIGIL_RENDERER__ as? String
        if (!globalOverride.isNullOrBlank()) return globalOverride

        val legacyOverride = window.asDynamic().SIGIL_RENDERER as? String
        if (!legacyOverride.isNullOrBlank()) return legacyOverride

        val params = js("new URLSearchParams(window.location.search)").unsafeCast<dynamic>()
        val queryOverride = (params.get("sigilRenderer") as? String)
            ?: (params.get("renderer") as? String)
        if (!queryOverride.isNullOrBlank()) return queryOverride

        return when (sceneData.settings.rendererPreference) {
            RendererPreference.AUTO -> null
            RendererPreference.WEBGL -> "webgl"
            RendererPreference.WEBGPU -> "webgpu"
        }
    }

    private fun replaceCanvasForRendererFallback() {
        val parent = canvas.parentNode ?: return
        val replacement = canvas.cloneNode(false).unsafeCast<HTMLCanvasElement>()
        replacement.width = canvas.width
        replacement.height = canvas.height
        parent.replaceChild(replacement, canvas)
        canvas = replacement
    }

    private fun reattachControlsFromSceneData() {
        controlsCleanup?.invoke()
        controlsCleanup = null
        orbitControls = null
        firstPersonControls = null
        sceneData.rootNodes.forEachControls { controlsData ->
            createControls(controlsData)
        }
    }

    private fun List<SigilNodeData>.forEachControls(block: (ControlsData) -> Unit) {
        for (node in this) {
            when (node) {
                is ControlsData -> block(node)
                is GroupData -> node.children.forEachControls(block)
                else -> Unit
            }
        }
    }

    fun startRenderLoop() {
        val cam = camera ?: return
        
        running = true
        lastFrameTimeMs = window.performance.now()
        
        fun renderFrame() {
            if (!running) return

            val now = window.performance.now()
            val deltaSeconds = ((now - lastFrameTimeMs) / 1000.0).toFloat().coerceIn(0f, 0.1f)
            lastFrameTimeMs = now

            val cameraPatchActive = updateGuidedCamera(now)
            if (!cameraPatchActive) orbitControls?.update(deltaSeconds)
            firstPersonControls?.update(deltaSeconds)
            updateSceneAnimations(now)
            updateBillboardTextNodes()
            audioListener?.updateMatrixWorld()

            val r = renderer ?: return
            cam.updateMatrixWorld()
            cam.updateProjectionMatrix()
            try {
                r.renderWithOverlays(materiaScene, cam, renderOverlayLayers())
            } catch (t: Throwable) {
                console.warn("Sigil: Render error (${t.message})")
                if (r is WebGPURenderer) {
                    scope.launch {
                        switchToWebGlFallbackAfterRenderError(t)
                    }
                } else {
                    stop()
                }
                return
            }

            val smoothedStats = frameStatsSmoother.recordFrame(deltaSeconds, r.stats)
            updateFrameStatsText(now, smoothedStats)
            updateAdaptiveResolution(now, smoothedStats)

            animationFrameId = window.requestAnimationFrame { renderFrame() }
        }
        
        renderFrame()
    }

    private suspend fun switchToWebGlFallbackAfterRenderError(error: Throwable) {
        val oldRenderer = renderer
        if (oldRenderer !is WebGPURenderer || !running) return

        console.warn("Sigil: WebGPU render failed (${error.message}), rebuilding canvas for WebGL fallback...")
        oldRenderer.dispose()
        renderer = null
        replaceCanvasForRendererFallback()
        reattachControlsFromSceneData()

        val initialized = initializeWebGlRenderer(RendererConfig(), fallback = true)
        if (!initialized) {
            console.error("Sigil: Could not recover WebGL renderer after WebGPU render failure")
            stop()
            return
        }

        interactionCleanup?.invoke()
        interactionCleanup = attachInteractionHandlers()
        lastFrameTimeMs = window.performance.now()
        animationFrameId = window.requestAnimationFrame { startRenderLoop() }
    }

    fun stop() {
        running = false
        if (animationFrameId != 0) {
            window.cancelAnimationFrame(animationFrameId)
            animationFrameId = 0
        }
    }

    internal fun screenLayerCountForTesting(): Int = screenLayers.size

    internal fun renderScaleForTesting(): Float = renderScale

    internal fun hydratedTextMeshCountForTesting(): Int = nodeMap.values.count { node ->
        node is Group && node.userData[SIGIL_TEXT_DATA_KEY] is TextData && node.children.isNotEmpty()
    }

    fun dispose() {
        stop()
        controlsCleanup?.invoke()
        interactionCleanup?.invoke()
        controlsCleanup = null
        interactionCleanup = null
        orbitControls = null
        firstPersonControls = null
        nodeMap.clear()
        interactionNodeMap.clear()
        nodeDataMap.clear()
        baseColorTextureMetadataCache.clear()
        pendingBaseColorHydrations.clear()
        activeAnimations.clear()
        billboardTextNodes.clear()
        frameStatsTextNodes.forEach { disposeTextChildren(it.node) }
        frameStatsTextNodes.clear()
        screenLayers.clear()
        overlayRenderLayers.clear()
        audioSources.values.forEach { it.stop() }
        audioSources.clear()
        audioBuses.clear()
        audioListener = null
        activeCameraPatch = null
        requestGate.clear()
        modelSwapTracker.clear()
        frameStatsSmoother.reset()
        pendingDrag = null
        activeDrag = null
        dragGesture.reset()
        renderer?.dispose()
        renderer = null
    }

    private fun applySceneSettings(settings: SceneSettings) {
        materiaScene.background = Background.Color(intToColor(settings.backgroundColor))
    }

    private fun displaySize(): Pair<Int, Int> {
        val rect = (canvas.parentElement ?: canvas).getBoundingClientRect()
        val width = rect.width.roundToInt().takeIf { it > 0 }
            ?: canvas.clientWidth.takeIf { it > 0 }
            ?: (canvas.width / renderScale).roundToInt().coerceAtLeast(1)
        val height = rect.height.roundToInt().takeIf { it > 0 }
            ?: canvas.clientHeight.takeIf { it > 0 }
            ?: (canvas.height / renderScale).roundToInt().coerceAtLeast(1)
        return width to height
    }

    private fun resizeForDisplay() {
        val (displayWidth, displayHeight) = displaySize()
        val renderWidth = (displayWidth * renderScale).roundToInt().coerceAtLeast(1)
        val renderHeight = (displayHeight * renderScale).roundToInt().coerceAtLeast(1)
        if (canvas.width != renderWidth) canvas.width = renderWidth
        if (canvas.height != renderHeight) canvas.height = renderHeight
        renderer?.let { resizeBackingStore(it, renderWidth, renderHeight) }

        camera?.let { worldCamera ->
            worldCamera.aspect = displayWidth.toFloat() / displayHeight.toFloat()
            worldCamera.updateProjectionMatrix()
        }
        updateScreenLayers(displayWidth, displayHeight)
    }

    private fun resizeBackingStore(renderer: Renderer, width: Int, height: Int) {
        renderer.resize(width, height)
        authoredCanvasDisplayStyle.restore(canvas)
    }

    private fun createScreenLayer(data: ScreenLayerData) {
        val overlayScene = Scene()
        val overlayCamera = OrthographicCamera(near = 0.1f, far = 2000f).apply {
            position.set(0f, 0f, 1000f)
            lookAt(Vector3.ZERO)
        }
        val root = Group().apply {
            rotation.set(data.rotation[0], data.rotation[1], data.rotation[2])
            name = data.name ?: ""
        }
        data.children.forEach { childData ->
            createMateriaNode(childData)?.let { child ->
                root.add(child)
                registerNode(childData, child)
            }
        }
        overlayScene.add(root)
        registerNode(data, root)
        screenLayers += RuntimeScreenLayer(data, overlayScene, overlayCamera, root)
        screenLayers.sortBy { it.data.order }
        overlayRenderLayers.clear()
        screenLayers.forEach { layer ->
            overlayRenderLayers += RenderOverlayLayer(
                layer.scene,
                layer.camera,
                clearDepth = layer.data.clearDepth
            )
        }
        val (width, height) = displaySize()
        updateScreenLayers(width, height)
    }

    private fun updateScreenLayers(displayWidth: Int, displayHeight: Int) {
        screenLayers.forEach { layer ->
            layer.camera.setViewBounds(
                left = -displayWidth / 2f,
                right = displayWidth / 2f,
                top = displayHeight / 2f,
                bottom = -displayHeight / 2f
            )
            val placement = SigilScreenLayoutResolver.resolve(layer.data, displayWidth, displayHeight)
            layer.root.position.set(
                placement.x,
                placement.y,
                layer.data.position.getOrElse(2) { 0f }
            )
            layer.root.scale.set(
                layer.data.scale.getOrElse(0) { 1f } * placement.scale,
                layer.data.scale.getOrElse(1) { 1f } * placement.scale,
                layer.data.scale.getOrElse(2) { 1f } * placement.scale
            )
            layer.root.visible = placement.visible
            layer.scene.updateMatrixWorld(true)
        }
    }

    private fun renderOverlayLayers(): List<RenderOverlayLayer> = overlayRenderLayers

    private fun createMateriaNode(nodeData: SigilNodeData): Object3D? {
        return when (nodeData) {
            is MeshData -> createMesh(nodeData)
            is ModelData -> createModel(nodeData)
            is TextData -> createText(nodeData)
            is FrameStatsTextData -> createFrameStatsText(nodeData)
            is AudioData -> createAudio(nodeData)
            is AudioBusData -> createAudioBus(nodeData)
            is ScreenLayerData -> null
            is GroupData -> createGroup(nodeData)
            is LightData -> {
                // Lights in Materia 0.2.0.0 don't extend Object3D
                // They're managed via LightingSystem
                createLight(nodeData)
                null
            }
            is CameraData -> {
                camera?.let { cam ->
                    cam.fov = nodeData.fov
                    cam.near = nodeData.near
                    cam.far = nodeData.far
                    cam.position.set(nodeData.position[0], nodeData.position[1], nodeData.position[2])
                    nodeData.lookAt?.let { target ->
                        cam.lookAt(Vector3(target[0], target[1], target[2]))
                    }
                    cam.updateProjectionMatrix()
                }
                null
            }
            is ControlsData -> {
                createControls(nodeData)
                null
            }
        }
    }

    private fun createMesh(data: MeshData): Mesh {
        val geometry = createGeometry(data.geometryType, data.geometryParams)
        val material = if (data.metalness > 0f || data.roughness < 1f) {
            MeshStandardMaterial(
                color = intToColor(data.materialColor),
                metalness = data.metalness,
                roughness = data.roughness
            )
        } else {
            MeshBasicMaterial().apply {
                color = intToColor(data.materialColor)
            }
        }

        return Mesh(geometry, material).apply {
            position.set(data.position[0], data.position[1], data.position[2])
            rotation.set(data.rotation[0], data.rotation[1], data.rotation[2])
            scale.set(data.scale[0], data.scale[1], data.scale[2])
            visible = data.visible
            castShadow = data.castShadow
            receiveShadow = data.receiveShadow
            name = data.name ?: ""
        }
    }

    private fun createModel(data: ModelData): Group {
        val group = Group().apply {
            position.set(data.position[0], data.position[1], data.position[2])
            rotation.set(data.rotation[0], data.rotation[1], data.rotation[2])
            scale.set(data.scale[0], data.scale[1], data.scale[2])
            visible = data.visible
            name = data.name ?: ""
            userData[SIGIL_MODEL_DATA_KEY] = data
            userData[SIGIL_MODEL_LOAD_STATE_KEY] = SigilModelLoadState()
        }

        preloadModelUrls(data)
        ensureModelHydratedForVisibility(group, data)

        return group
    }

    private fun ensureModelHydratedForVisibility(group: Group, data: ModelData) {
        val loadState = group.userData[SIGIL_MODEL_LOAD_STATE_KEY] as? SigilModelLoadState ?: return
        if (!loadState.tryStartForVisibility(group.visible)) return

        scope.launch {
            try {
                val root = loadModelRoot(data)
                group.clear()
                group.add(root)
                replayDeferredVisualState(group)
            } catch (t: Throwable) {
                console.error("Sigil: Failed to load model ${data.url}: ${t.message}")
                group.clear()
                group.add(createModelFallback(data))
                replayDeferredVisualState(group)
            } finally {
                loadState.complete()
            }
        }
    }

    private fun preloadModelUrls(data: ModelData) {
        data.preloadUrls.distinct().filter { it != data.url }.forEach { url ->
            scope.launch {
                try {
                    val resolver = SigilRelativeAssetResolver(url)
                    val asset = GLTFLoader(resolver).load(url)
                    hydrateGltfBaseColorTextures(asset.materials, url, resolver)
                } catch (t: Throwable) {
                    console.warn("Sigil: Could not preload model $url: ${t.message}")
                }
            }
        }
    }

    private suspend fun loadModelRoot(data: ModelData): Object3D {
        val assetResolver = SigilRelativeAssetResolver(data.url)
        val asset = GLTFLoader(assetResolver).load(data.url)
        hydrateGltfBaseColorTextures(asset.materials, data.url, assetResolver)
        return asset.scene.also { root ->
            SigilModelMaterialIsolation.isolateMutableMaterials(root)
            applyModelSettings(root, data)
        }
    }

    private fun createControls(data: ControlsData) {
        val cam = camera ?: return

        controlsCleanup?.invoke()

        when (data.controlsType) {
            ControlsType.ORBIT -> {
                val config = ControlsConfig(
                    minDistance = data.minDistance,
                    maxDistance = data.maxDistance,
                    minPolarAngle = data.minPolarAngle,
                    maxPolarAngle = data.maxPolarAngle,
                    minAzimuthAngle = data.minAzimuthAngle,
                    maxAzimuthAngle = data.maxAzimuthAngle,
                    rotateSpeed = data.rotateSpeed,
                    zoomSpeed = data.zoomSpeed,
                    panSpeed = data.panSpeed,
                    keyboardSpeed = data.keyboardSpeed,
                    enableRotate = data.enableRotate,
                    enableZoom = data.enableZoom,
                    enablePan = data.enablePan,
                    enableKeys = data.enableKeys,
                    enableDamping = data.enableDamping,
                    dampingFactor = data.dampingFactor,
                    dampingTime = data.dampingTime,
                    settleEpsilon = data.settleEpsilon,
                    maxDeltaTime = data.maxDeltaTime,
                    autoRotate = data.autoRotate,
                    autoRotateSpeed = data.autoRotateSpeed
                )

                val controls = OrbitControls(cam, config).apply {
                    target = Vector3(data.target[0], data.target[1], data.target[2])
                }

                orbitControls = controls
                controlsCleanup = attachOrbitControlsHandlers(controls)
            }

            ControlsType.FIRST_PERSON -> {
                val config = ControlsConfig(
                    rotateSpeed = data.lookSpeed / 0.002f,
                    enableKeys = true,
                    enableRotate = true
                )

                val controls = FirstPersonControls(cam, config).apply {
                    walkSpeed = data.moveSpeed
                    mouseSensitivity = data.lookSpeed
                    enableGravity = data.enableGravity
                    groundHeight = data.groundHeight + data.heightOffset
                    enableCollision = data.enableCollision
                    collisionRadius = data.collisionRadius
                }

                // Apply initial position from data
                if (data.position != listOf(0f, 0f, 0f)) {
                    controls.setPosition(Vector3(data.position[0], data.position[1], data.position[2]))
                }

                firstPersonControls = controls
                controlsCleanup = attachFirstPersonControlsHandlers(controls, data.pointerLock)
            }
        }
    }

    private fun createGroup(data: GroupData): Group {
        val group = Group().apply {
            position.set(data.position[0], data.position[1], data.position[2])
            rotation.set(data.rotation[0], data.rotation[1], data.rotation[2])
            scale.set(data.scale[0], data.scale[1], data.scale[2])
            visible = data.visible
            name = data.name ?: ""
        }

        for (childData in data.children) {
            val childNode = createMateriaNode(childData)
            if (childNode != null) {
                group.add(childNode)
                registerNode(childData, childNode)
            }
        }

        return group
    }

    private fun createAudio(data: AudioData): MateriaAudio {
        val listener = audioListener ?: AudioListener(camera).also { audioListener = it }
        val source: MateriaAudio = if (data.positional) {
            PositionalAudio(listener).apply {
                refDistance = data.refDistance
                maxDistance = data.maxDistance
                rolloffFactor = data.rolloffFactor
            }
        } else {
            MateriaAudio(listener)
        }

        source.position.set(data.position[0], data.position[1], data.position[2])
        source.rotation.set(data.rotation[0], data.rotation[1], data.rotation[2])
        source.scale.set(data.scale[0], data.scale[1], data.scale[2])
        source.visible = data.visible
        source.name = data.name ?: ""
        source.setBus(data.bus)
        source.setVolume(data.volume)
        source.setLoop(data.loop)
        source.autoplay = data.autoplay && data.visible

        data.procedural?.let { source.setProcedural(it.toMateriaSpec()) }
            ?: data.url?.let(source::load)
        if (data.procedural != null && source.autoplay) source.play()
        audioSources[data.id] = source
        return source
    }

    private fun ProceduralAudioData.toMateriaSpec(): ProceduralAudioSpec = ProceduralAudioSpec(
        waveform = when (waveform) {
            ProceduralWaveform.SINE -> AudioWaveform.SINE
            ProceduralWaveform.SQUARE -> AudioWaveform.SQUARE
            ProceduralWaveform.SAWTOOTH -> AudioWaveform.SAWTOOTH
            ProceduralWaveform.TRIANGLE -> AudioWaveform.TRIANGLE
        },
        startFrequencyHz = startFrequencyHz,
        endFrequencyHz = endFrequencyHz,
        durationSeconds = durationSeconds,
        attackSeconds = attackSeconds,
        releaseSeconds = releaseSeconds,
        oscillatorGain = oscillatorGain,
        noiseGain = noiseGain,
        lowPassFrequencyHz = lowPassFrequencyHz
    )

    private fun createAudioBus(data: AudioBusData): Group {
        val storedVolume = data.storageKey
            ?.let { persistence.read(it, data.storageBackend) }
            ?.toFloatOrNull()
            ?.takeIf { it in 0f..1f }
        BrowserAudioEngine.setBusVolume(data.bus, storedVolume ?: data.volume)
        audioBuses[data.bus] = data
        return Group().apply {
            visible = false
            name = data.name ?: "audio-bus-${data.bus}"
            userData["sigilAudioBus"] = data.bus
        }
    }

    private fun createText(data: TextData): Group {
        val group = Group().apply {
            position.set(data.position[0], data.position[1], data.position[2])
            rotation.set(data.rotation[0], data.rotation[1], data.rotation[2])
            scale.set(data.scale[0], data.scale[1], data.scale[2])
            visible = data.visible
            name = data.name ?: ""
            userData[SIGIL_TEXT_DATA_KEY] = data
        }

        if (data.facingMode == TextFacingMode.BILLBOARD) {
            billboardTextNodes.add(group)
        }

        hydrateTextMesh(group, data)

        return group
    }

    private fun createFrameStatsText(data: FrameStatsTextData): Group {
        val textData = TextData(
            id = data.id,
            position = data.position,
            rotation = data.rotation,
            scale = data.scale,
            visible = data.visible,
            name = data.name,
            interaction = data.interaction,
            animations = data.animations,
            text = data.prefix + "--",
            color = data.color,
            size = data.size,
            depth = data.depth,
            curveSegments = 4,
            align = data.align,
            baseline = data.baseline,
            fontUrl = data.fontUrl,
            castShadow = false,
            receiveShadow = false
        )
        val group = createText(textData)
        frameStatsTextNodes += RuntimeFrameStatsText(data = data, node = group)
        return group
    }

    private fun hydrateTextMesh(group: Group, data: TextData) {
        val generation = ((group.userData[SIGIL_TEXT_GENERATION_KEY] as? Int) ?: 0) + 1
        group.userData[SIGIL_TEXT_GENERATION_KEY] = generation
        group.userData[SIGIL_TEXT_DATA_KEY] = data
        scope.launch {
            try {
                val font = SigilTextFontCache.load(data.fontUrl)
                val geometry = TextGeometry(data.text, font, data.toTextOptions())
                val material = MeshBasicMaterial().apply {
                    color = intToColor(data.color)
                    side = Side.DoubleSide
                }
                val mesh = Mesh(geometry, material).apply {
                    castShadow = data.castShadow
                    receiveShadow = data.receiveShadow
                    name = data.name?.let { "$it-text-mesh" } ?: ""
                }

                if (group.userData[SIGIL_TEXT_GENERATION_KEY] != generation) {
                    geometry.dispose()
                    material.dispose()
                    return@launch
                }
                disposeTextChildren(group)
                group.clear()
                group.add(mesh)
                replayDeferredVisualState(group)
            } catch (t: Throwable) {
                console.error("Sigil: Failed to hydrate text ${data.id}: ${t.message}")
            }
        }
    }

    private fun updateTextNode(group: Group, text: String) {
        if (text.isBlank()) return
        val current = group.userData[SIGIL_TEXT_DATA_KEY] as? TextData ?: return
        if (current.text == text) return
        val updated = current.copy(text = text)
        hydrateTextMesh(group, updated)

        val nodeId = group.userData["sigilNodeId"] as? String ?: return
        if (nodeDataMap[nodeId] is TextData) nodeDataMap[nodeId] = updated
    }

    private fun disposeTextChildren(group: Group) {
        group.children.toList().forEach { child ->
            val mesh = child as? Mesh ?: return@forEach
            mesh.geometry.dispose()
            (mesh.material as? BaseMaterial)?.dispose()
        }
    }

    private fun TextData.toTextOptions(): TextOptions = TextOptions(
        size = size,
        height = depth,
        curveSegments = curveSegments,
        bevelEnabled = false,
        bevelThickness = 0.1f,
        bevelSize = 0.05f,
        bevelOffset = 0f,
        bevelSegments = 3,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        textAlign = align.toMateriaTextAlign(),
        textBaseline = baseline.toMateriaTextBaseline(),
        maxWidth = maxWidth,
        wordWrap = wordWrap
    )

    private fun TextAlignMode.toMateriaTextAlign(): MateriaTextAlign = when (this) {
        TextAlignMode.LEFT -> MateriaTextAlign.LEFT
        TextAlignMode.CENTER -> MateriaTextAlign.CENTER
        TextAlignMode.RIGHT -> MateriaTextAlign.RIGHT
        TextAlignMode.JUSTIFY -> MateriaTextAlign.JUSTIFY
    }

    private fun TextBaselineMode.toMateriaTextBaseline(): MateriaTextBaseline = when (this) {
        TextBaselineMode.ALPHABETIC -> MateriaTextBaseline.ALPHABETIC
        TextBaselineMode.TOP -> MateriaTextBaseline.TOP
        TextBaselineMode.HANGING -> MateriaTextBaseline.HANGING
        TextBaselineMode.MIDDLE -> MateriaTextBaseline.MIDDLE
        TextBaselineMode.IDEOGRAPHIC -> MateriaTextBaseline.IDEOGRAPHIC
        TextBaselineMode.BOTTOM -> MateriaTextBaseline.BOTTOM
    }

    private fun updateBillboardTextNodes() {
        val cam = camera ?: return
        for (node in billboardTextNodes) {
            node.quaternion.copy(cam.quaternion)
        }
    }

    private fun updateFrameStatsText(now: Double, stats: SmoothedFrameStats) {
        frameStatsTextNodes.forEach { runtime ->
            if (now - runtime.lastUpdateMs < runtime.data.updateIntervalMs) return@forEach
            runtime.lastUpdateMs = now
            val fps = stats.fps.asDynamic().toFixed(runtime.data.decimalPlaces) as String
            val text = runtime.data.prefix + fps
            if (text != runtime.lastText) {
                runtime.lastText = text
                updateTextNode(runtime.node, text)
            }
        }
    }

    private fun updateAdaptiveResolution(now: Double, stats: SmoothedFrameStats) {
        val controller = adaptiveResolutionController ?: return
        if (now - lastAdaptiveResolutionCheckMs < 500.0) return
        lastAdaptiveResolutionCheckMs = now
        controller.record(stats.fps)?.let { nextScale ->
            renderScale = nextScale
            resizeForDisplay()
        }
    }

    private fun applyModelSettings(root: Object3D, data: ModelData) {
        root.traverse { node ->
            val mesh = node as? Mesh ?: return@traverse
            mesh.castShadow = data.castShadow
            mesh.receiveShadow = data.receiveShadow
            SigilTextureFidelity.configureGltfGeometryAttributes(mesh)
            SigilTextureFidelity.configureMaterial(mesh.material)
            applyMaterialOverrides(mesh, data.materialOverrides)
        }
    }

    private fun configureSceneTextureFidelity() {
        val scenes = listOf(materiaScene) + screenLayers.map { it.scene }
        scenes.forEach { scene ->
            scene.traverse { node ->
                val mesh = node as? Mesh ?: return@traverse
                SigilTextureFidelity.configureMaterial(mesh.material)
            }
        }
    }

    private fun applyMaterialOverrides(mesh: Mesh, overrides: List<ModelMaterialOverride>) {
        if (overrides.isEmpty()) return
        val material = mesh.material ?: return

        val materialName = when (material) {
            is MeshStandardMaterial -> material.name
            is MeshBasicMaterial -> material.name
            else -> ""
        }

        val matching = overrides.filter { override ->
            override.target == null ||
                override.target == mesh.name ||
                (materialName.isNotEmpty() && override.target == materialName)
        }
        if (matching.isEmpty()) return

        var overrideColor: Int? = null
        var overrideMetalness: Float? = null
        var overrideRoughness: Float? = null

        for (override in matching) {
            if (override.color != null) overrideColor = override.color
            if (override.metalness != null) overrideMetalness = override.metalness
            if (override.roughness != null) overrideRoughness = override.roughness
        }

        val didUpdate = overrideColor != null || overrideMetalness != null || overrideRoughness != null

        overrideColor?.let { color ->
            when (material) {
                is MeshStandardMaterial -> material.color = intToColor(color)
                is MeshBasicMaterial -> material.color = intToColor(color)
            }
        }

        if (material is MeshStandardMaterial) {
            overrideMetalness?.let { material.metalness = it }
            overrideRoughness?.let { material.roughness = it }
        }

        if (didUpdate) {
            material.needsUpdate = true
        }

    }

    private fun registerNode(data: SigilNodeData, node: Object3D) {
        nodeMap[data.id] = node
        nodeDataMap[data.id] = data
        applyNodeMetadata(node, data)
    }

    private fun applyNodeMetadata(node: Object3D, data: SigilNodeData) {
        node.userData["sigilNodeId"] = data.id
        data.interaction?.let { interaction ->
            node.userData["sigilInteraction"] = interaction
            interaction.interactionId?.let { interactionId ->
                node.userData["sigilInteractionId"] = interactionId
                interactionNodeMap[interactionId] = node
            }
        } ?: run {
            node.userData.remove("sigilInteraction")
            node.userData.remove("sigilInteractionId")
        }

        val animations = data.animations
        if (animations.isNotEmpty()) {
            node.userData["sigilAnimations"] = animations
        } else {
            node.userData.remove("sigilAnimations")
        }
    }

    fun applyPatch(patch: ScenePatch) {
        patch.camera?.let(::applyCameraPatch)
        patch.audio.forEach(::applyAudioPatch)
        patch.storage.forEach(persistence::apply)

        for (nodePatch in patch.nodes) {
            val node = findPatchTarget(nodePatch) ?: continue
            if (applyNodePatch(node, nodePatch)) {
                refreshNodeWorldMatrix(node)
            }
            val nodeId = node.userData["sigilNodeId"] as? String
            val nodeData = nodeId?.let { nodeDataMap[it] }
            if (nodePatch.animations.isNotEmpty()) {
                scheduleAnimations(node, nodePatch.animations, AnimationTrigger.PATCH)
            } else if (nodeData != null) {
                scheduleAnimations(node, nodeData.animations, AnimationTrigger.PATCH)
            }
        }
    }

    private fun findPatchTarget(patch: SceneNodePatch): Object3D? {
        patch.interactionId?.let { interactionId ->
            interactionNodeMap[interactionId]?.let { return it }
        }
        patch.id?.let { id ->
            nodeMap[id]?.let { return it }
        }
        return null
    }

    private fun applyNodePatch(node: Object3D, patch: SceneNodePatch): Boolean {
        var transformChanged = false
        patch.position?.takeIf { it.size >= 3 }?.let {
            node.position.set(it[0], it[1], it[2])
            transformChanged = true
        }
        patch.rotation?.takeIf { it.size >= 3 }?.let {
            node.rotation.set(it[0], it[1], it[2])
            transformChanged = true
        }
        patch.scale?.takeIf { it.size >= 3 }?.let {
            node.scale.set(it[0], it[1], it[2])
            transformChanged = true
        }
        patch.visible?.let {
            node.visible = it
            if (it) {
                ensureDeferredModelHydrated(node)
            } else {
                (node as? MateriaAudio)?.stop()
            }
        }
        patch.name?.let { node.name = it }
        val updatedText = patch.text ?: patch.label
        if (updatedText != null && node is Group && node.userData[SIGIL_TEXT_DATA_KEY] is TextData) {
            updateTextNode(node, updatedText)
        } else {
            patch.label?.let { node.userData["sigilLabel"] = it }
        }
        patch.modelUrl?.takeIf(String::isNotBlank)?.let { url ->
            (node as? Group)?.let { replaceModelAtomically(it, url) }
        }
        patch.interactionEnabled?.let { enabled -> setInteractionEnabled(node, enabled) }
        patch.highlight?.let { applyHighlightPatch(node, it) }
        return transformChanged
    }

    private fun applyCameraPatch(patch: CameraPatch) {
        val cam = camera ?: return
        val endPosition = patch.position.toVector3()
        val targetValues = patch.orbitTarget ?: patch.lookAt
        val endTarget = targetValues?.toVector3() ?: orbitControls?.target?.clone() ?: Vector3.ZERO.clone()
        val controls = orbitControls
        if (patch.cancelMomentum) {
            controls?.cancelAnimation()
            controls?.cancelMomentum()
        }

        if (patch.durationMs == 0) {
            activeCameraPatch = null
            if (controls != null) {
                controls.setPose(endPosition, endTarget)
            } else {
                cam.position.copy(endPosition)
                cam.lookAt(endTarget)
                cam.updateMatrixWorld(true)
            }
            return
        }

        activeCameraPatch = ActiveCameraPatch(
            startedAtMs = window.performance.now(),
            durationMs = patch.durationMs,
            startPosition = cam.position.clone(),
            startTarget = controls?.target?.clone() ?: patch.lookAt?.toVector3() ?: Vector3.ZERO.clone(),
            endPosition = endPosition,
            endTarget = endTarget,
            easing = patch.easing
        )
    }

    private fun updateGuidedCamera(now: Double): Boolean {
        val active = activeCameraPatch ?: return false
        val progress = ((now - active.startedAtMs) / active.durationMs).toFloat().coerceIn(0f, 1f)
        val eased = easedProgress(progress, active.easing)
        val position = Vector3().lerpVectors(active.startPosition, active.endPosition, eased)
        val target = Vector3().lerpVectors(active.startTarget, active.endTarget, eased)
        val controls = orbitControls
        if (controls != null) {
            controls.setPose(position, target)
        } else {
            camera?.apply {
                this.position.copy(position)
                lookAt(target)
                updateMatrixWorld(true)
            }
        }

        if (progress >= 1f) activeCameraPatch = null
        return progress < 1f
    }

    private fun applyAudioPatch(patch: AudioPatch) {
        if (patch.action == AudioPatchAction.UNLOCK) {
            scope.launch { BrowserAudioEngine.resume() }
            return
        }

        patch.sourceId?.let(audioSources::get)?.let { source ->
            patch.volume?.let(source::setVolume)
            patch.loop?.let(source::setLoop)
            when (patch.action) {
                AudioPatchAction.PLAY -> source.play()
                AudioPatchAction.PAUSE -> source.pause()
                AudioPatchAction.STOP -> source.stop()
                AudioPatchAction.SET_VOLUME, AudioPatchAction.SET_LOOP, AudioPatchAction.UNLOCK -> Unit
            }
        }

        val bus = patch.bus ?: return
        patch.volume?.let { volume ->
            BrowserAudioEngine.setBusVolume(bus, volume)
            if (patch.persist) {
                audioBuses[bus]?.storageKey?.let { key ->
                    persistence.apply(
                        codes.yousef.sigil.schema.StoragePatch(
                            action = codes.yousef.sigil.schema.StoragePatchAction.SET,
                            key = key,
                            value = volume.toString(),
                            backend = audioBuses[bus]?.storageBackend
                                ?: codes.yousef.sigil.schema.StorageBackend.LOCAL_STORAGE
                        )
                    )
                }
            }
        }
    }

    private fun setInteractionEnabled(node: Object3D, enabled: Boolean) {
        val current = interactionForNode(node) ?: return
        node.userData["sigilInteraction"] = current.copy(enabled = enabled)
    }

    private fun replaceModelAtomically(group: Group, modelUrl: String) {
        val current = group.userData[SIGIL_MODEL_DATA_KEY] as? ModelData ?: return
        if (current.url == modelUrl) return
        val nodeId = group.userData["sigilNodeId"] as? String ?: current.id
        val replacement = current.copy(url = modelUrl)
        val generation = modelSwapTracker.begin(nodeId)

        scope.launch {
            try {
                val root = loadModelRoot(replacement)
                if (!modelSwapTracker.isCurrent(nodeId, generation)) return@launch
                group.clear()
                group.add(root)
                group.userData[SIGIL_MODEL_DATA_KEY] = replacement
                nodeDataMap[nodeId] = replacement
                replayDeferredVisualState(group)
                refreshNodeWorldMatrix(group)
            } catch (t: Throwable) {
                console.error("Sigil: Model replacement kept previous asset after $modelUrl failed: ${t.message}")
            }
        }
    }

    private fun ensureDeferredModelHydrated(node: Object3D) {
        val group = node as? Group ?: return
        val data = group.userData[SIGIL_MODEL_DATA_KEY] as? ModelData ?: return
        ensureModelHydratedForVisibility(group, data)
    }

    private fun applyHighlightPatch(node: Object3D, patch: HighlightPatch) {
        if (patch.active) {
            node.userData[SIGIL_HIGHLIGHT_PATCH_KEY] = patch
            setNodeMaterialColor(node, intToColor(patch.color), storeOriginal = true)
            node.userData["sigilHighlightActive"] = true
        } else {
            restoreNodeMaterialColor(node)
            node.userData.remove("sigilHighlightActive")
            node.userData.remove(SIGIL_HIGHLIGHT_PATCH_KEY)
        }
    }

    private fun replayDeferredVisualState(node: Object3D) {
        val patch = node.userData[SIGIL_HIGHLIGHT_PATCH_KEY] as? HighlightPatch
        if (patch?.active == true) {
            setNodeMaterialColor(node, intToColor(patch.color), storeOriginal = true)
        }
    }

    private suspend fun hydrateGltfBaseColorTextures(
        materials: List<Material>,
        modelUrl: String,
        assetResolver: AssetResolver
    ) {
        if (!SigilGltfMetadata.isGltfUrl(modelUrl) && !SigilGltfMetadata.isGlbUrl(modelUrl)) {
            return
        }

        pendingBaseColorHydrations[modelUrl]?.let { pending ->
            pending.await()
            return
        }

        val pending = CompletableDeferred<Unit>()
        pendingBaseColorHydrations[modelUrl] = pending

        try {
            val baseColorTextures = baseColorTexturesForModel(modelUrl, assetResolver)
            if (baseColorTextures.isEmpty()) return

            val textureOptions = SigilTextureOptions(
                generateMipmaps = true,
                flipY = false,
                anisotropy = 4f,
                magFilter = TextureFilter.LINEAR,
                minFilter = TextureFilter.LINEAR_MIPMAP_LINEAR
            )

            for (textureInfo in baseColorTextures) {
                val material = materials.getOrNull(textureInfo.materialIndex) ?: continue
                if (materialHasBaseColorTexture(material)) {
                    applyBaseColorFactor(material, textureInfo.baseColorFactor)
                    continue
                }

                val textureUri = SigilGltfMetadata.resolveAssetPath(textureInfo.uri, modelUrl = modelUrl)
                val texture = try {
                    SigilBrowserTextureLoader.load(
                        uri = textureUri,
                        mimeType = textureInfo.mimeType,
                        assetResolver = assetResolver,
                        options = textureOptions
                    )
                } catch (t: Throwable) {
                    console.warn("Sigil: Could not load glTF baseColor texture $textureUri: ${t.message}")
                    continue
                }

                SigilTextureFidelity.configureTexture(texture)
                applyBaseColorTexture(material, texture, textureInfo.baseColorFactor)
            }
        } finally {
            pending.complete(Unit)
            pendingBaseColorHydrations.remove(modelUrl)
        }
    }

    private suspend fun baseColorTexturesForModel(
        modelUrl: String,
        assetResolver: AssetResolver
    ): List<GltfBaseColorTexture> {
        baseColorTextureMetadataCache[modelUrl]?.let { return it }

        val gltfJson = try {
            assetResolver.load(modelUrl, null).decodeToString()
        } catch (t: Throwable) {
            console.warn("Sigil: Could not read glTF JSON metadata for $modelUrl: ${t.message}")
            ""
        }

        val baseColorTextures = if (gltfJson.isBlank()) {
            emptyList()
        } else {
            try {
                SigilGltfMetadata.extractBaseColorTextures(gltfJson)
            } catch (t: Throwable) {
                console.warn("Sigil: Could not parse glTF material texture metadata for $modelUrl: ${t.message}")
                emptyList()
            }
        }

        baseColorTextureMetadataCache[modelUrl] = baseColorTextures
        return baseColorTextures
    }

    private fun materialHasBaseColorTexture(material: Material): Boolean =
        when (material) {
            is MeshStandardMaterial -> material.map != null
            is MeshBasicMaterial -> material.map != null
            else -> false
        }

    private fun applyBaseColorTexture(
        material: Material,
        texture: Texture,
        baseColorFactor: List<Float>
    ) {
        applyBaseColorFactor(material, baseColorFactor)
        val alpha = baseColorFactor.getOrNull(3) ?: 1f

        when (material) {
            is MeshStandardMaterial -> {
                if (material.map == null) material.map = texture.unsafeCast<Texture2D>()
                material.needsUpdate = true
            }
            is MeshBasicMaterial -> {
                if (material.map == null) material.map = texture
                material.needsUpdate = true
            }
            is BaseMaterial -> {
                material.transparent = alpha < 1f
                material.opacity = alpha
                material.needsUpdate = true
            }
        }
    }

    private fun applyBaseColorFactor(
        material: Material,
        baseColorFactor: List<Float>
    ) {
        val color = colorFromBaseColorFactor(baseColorFactor)
        val alpha = baseColorFactor.getOrNull(3) ?: 1f

        when (material) {
            is MeshStandardMaterial -> {
                material.color = color
                if (alpha < 1f) {
                    material.transparent = true
                    material.opacity = alpha
                }
                material.needsUpdate = true
            }
            is MeshBasicMaterial -> {
                material.color = color
                material.transparent = alpha < 1f
                material.opacity = alpha
                material.needsUpdate = true
            }
            is BaseMaterial -> {
                material.transparent = alpha < 1f
                material.opacity = alpha
                material.needsUpdate = true
            }
        }
    }

    private fun colorFromBaseColorFactor(factor: List<Float>): Color {
        return Color(
            factor.getOrNull(0) ?: 1f,
            factor.getOrNull(1) ?: 1f,
            factor.getOrNull(2) ?: 1f,
            factor.getOrNull(3) ?: 1f
        )
    }

    private fun attachInteractionHandlers(): () -> Unit {
        val hasInteractiveNodes = nodeDataMap.values.any { it.interaction != null }
        if (!hasInteractiveNodes) return {}

        canvas.style.setProperty("touch-action", "none")

        val mouseDown: (Event) -> Unit = mouseDown@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseDown
            dragGesture.beginPointer(pointerPosition(mouseEvent))
            pendingDrag = null
            val hit = pickInteractionHit(mouseEvent) ?: return@mouseDown
            val node = hit.node
            val interaction = interactionForNode(node) ?: return@mouseDown
            suppressControlGesture(mouseEvent)
            dispatchSceneEvent("pointerdown", mouseEvent, node, hit.intersection)

            val drag = interaction.drag
            if (drag?.enabled == true) {
                val constraint = beginDrag(mouseEvent, node, interaction, hit.intersection.point) ?: return@mouseDown
                pendingDrag = PendingDrag(
                    source = node,
                    sourceInteraction = interaction,
                    constraint = constraint,
                    startIntersection = hit.intersection
                )
            }
        }

        val mouseMove: (Event) -> Unit = mouseMove@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseMove
            pendingDrag?.let { pending ->
                suppressControlGesture(mouseEvent)
                if (!dragGesture.movedBeyondThreshold(pointerPosition(mouseEvent))) {
                    return@mouseMove
                }

                val drag = ActiveDrag(
                    source = pending.source,
                    sourceInteraction = pending.sourceInteraction,
                    constraint = pending.constraint
                )
                pendingDrag = null
                activeDrag = drag
                updateHoverDropTarget(null)
                setCanvasCursor(CursorHint.GRABBING)
                dispatchSceneEvent("dragstart", mouseEvent, pending.source, pending.startIntersection, drag)
                updateActiveDrag(drag, mouseEvent)
                return@mouseMove
            }

            val drag = activeDrag
            if (drag != null) {
                suppressControlGesture(mouseEvent)
                updateActiveDrag(drag, mouseEvent)
                return@mouseMove
            }

            val hit = pickInteractive(mouseEvent)
            val node = hit.second
            updateHoverDropTarget(node?.takeIf { isDropTargetNode(it) })
            if (node != null) {
                interactionForNode(node)?.let { setCanvasCursor(it.cursor) }
                dispatchSceneEvent("pointermove", mouseEvent, node, hit.first)
            } else {
                setCanvasCursor(CursorHint.AUTO)
            }
        }

        val mouseUp: (Event) -> Unit = mouseUp@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseUp
            val drag = activeDrag
            if (drag != null) {
                suppressControlGesture(mouseEvent)
                drag.target?.let { target ->
                    dispatchSceneEvent("drop", mouseEvent, target, null, drag)
                    setDropTargetState(target, null)
                } ?: run {
                    drag.targetAccepted = false
                    drag.dropResult = "no-target"
                    dispatchSceneEvent("drop", mouseEvent, drag.source, null, drag)
                }
                dispatchSceneEvent("dragend", mouseEvent, drag.source, null, drag)
                activeDrag = null
                dragGesture.completeDrag()
                setCanvasCursor(CursorHint.AUTO)
                return@mouseUp
            }

            pendingDrag?.let {
                pendingDrag = null
                dragGesture.endWithoutDrag()
            }

            val hit = pickInteractive(mouseEvent)
            hit.second?.let { node ->
                dispatchSceneEvent("pointerup", mouseEvent, node, hit.first)
            }
        }

        val click: (Event) -> Unit = click@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@click
            if (dragGesture.consumeClickSuppression()) {
                suppressControlGesture(mouseEvent)
                return@click
            }

            val hit = pickInteractive(mouseEvent)
            hit.second?.let { node ->
                suppressControlGesture(mouseEvent)
                dispatchSceneEvent("click", mouseEvent, node, hit.first)
            }
        }

        canvas.addEventListener("mousedown", mouseDown, true)
        canvas.addEventListener("mousemove", mouseMove, true)
        canvas.addEventListener("mouseup", mouseUp, true)
        canvas.addEventListener("click", click, true)

        return {
            updateHoverDropTarget(null)
            activeDrag?.target?.let { setDropTargetState(it, null) }
            pendingDrag = null
            activeDrag = null
            dragGesture.reset()
            canvas.removeEventListener("mousedown", mouseDown, true)
            canvas.removeEventListener("mousemove", mouseMove, true)
            canvas.removeEventListener("mouseup", mouseUp, true)
            canvas.removeEventListener("click", click, true)
            canvas.style.setProperty("cursor", "auto")
        }
    }

    private fun pointerPosition(event: MouseEvent): SigilPointerPosition =
        SigilPointerPosition(event.clientX.toFloat(), event.clientY.toFloat())

    private fun suppressControlGesture(event: MouseEvent) {
        event.preventDefault()
        event.stopPropagation()
        event.asDynamic().stopImmediatePropagation()
    }

    private fun pickInteractive(event: MouseEvent): Pair<Intersection?, Object3D?> {
        val hit = pickInteractionHit(event) ?: return Pair(null, null)
        return Pair(hit.intersection, hit.node)
    }

    private fun beginDrag(
        event: MouseEvent,
        node: Object3D,
        interaction: InteractionMetadata,
        hitPoint: Vector3?
    ): SigilDragSession? {
        val ray = rayForEvent(event) ?: return null
        return SigilDragController.begin(
            ray = ray,
            nodePosition = node.position,
            hitPoint = hitPoint,
            metadata = interaction.drag
        )
    }

    private fun pickDropTarget(event: MouseEvent, drag: ActiveDrag): Pair<Object3D?, DropEvaluation?> {
        val hit = pickInteractionHit(event) { candidate ->
            candidate != drag.source && isDropTargetNode(candidate)
        } ?: return Pair(null, null)

        return Pair(hit.node, evaluateDrop(drag, hit.node))
    }

    private fun updateActiveDrag(drag: ActiveDrag, mouseEvent: MouseEvent) {
        updateDragPosition(drag, mouseEvent)
        val (target, evaluation) = pickDropTarget(mouseEvent, drag)
        val targetState = evaluation?.let { dropStateFor(it) }
        if (target != drag.target) {
            drag.target?.let { oldTarget ->
                setDropTargetState(oldTarget, null)
                dispatchSceneEvent("dragleave", mouseEvent, oldTarget, null, drag)
            }
            drag.target = target
            drag.targetState = targetState
            drag.targetAccepted = evaluation?.accepted
            drag.dropResult = evaluation?.result
            target?.let { newTarget ->
                setDropTargetState(newTarget, targetState)
                dispatchSceneEvent("dragenter", mouseEvent, newTarget, null, drag)
            }
        } else {
            drag.targetState = targetState
            drag.targetAccepted = evaluation?.accepted
            drag.dropResult = evaluation?.result
            target?.let { setDropTargetState(it, targetState) }
        }
        dispatchSceneEvent("drag", mouseEvent, drag.source, null, drag)
    }

    private fun pickInteractionHit(
        event: MouseEvent,
        acceptCandidate: (Object3D) -> Boolean = { true }
    ): SigilInteractionHit? {
        val worldCamera = camera ?: return null
        val pointer = normalizedPointer(event)
        return overlayFirstPicker.pick(
            normalizedPointer = pointer,
            scene = materiaScene,
            camera = worldCamera,
            overlays = renderOverlayLayers()
        ) { scene, sceneCamera, normalizedPointer ->
            pickInteractionHitInScene(normalizedPointer, scene, sceneCamera, acceptCandidate)
        }?.value
    }

    private fun pickInteractionHitInScene(
        pointer: Vector2,
        scene: Scene,
        sceneCamera: Camera,
        acceptCandidate: (Object3D) -> Boolean
    ): SigilInteractionHit? {
        val ray = SigilInteractionPicker.rayFromCamera(pointer, sceneCamera)
        val hitVolumeHits = hitVolumeHits(ray, scene)
        val hits = if (shouldRunMeshRaycaster(scene, acceptCandidate)) {
            hitVolumeHits + meshRaycasterHits(ray, scene)
        } else {
            hitVolumeHits
        }

        return hits
            .sortedBy { it.intersection.distance }
            .firstOrNull { acceptCandidate(it.node) }
    }

    private fun shouldRunMeshRaycaster(
        scene: Scene,
        acceptCandidate: (Object3D) -> Boolean
    ): Boolean {
        val acceptedInteractions = nodeMap.values.mapNotNull { node ->
            if (!node.belongsToScene(scene)) return@mapNotNull null
            if (!node.isVisibleInHierarchy()) return@mapNotNull null
            if (!acceptCandidate(node)) return@mapNotNull null
            interactionForNode(node)?.takeIf { it.enabled }
        }

        return SigilInteractionPicker.requiresMeshRaycast(acceptedInteractions)
    }

    private fun rayForEvent(event: MouseEvent): Ray? {
        val cam = camera ?: return null
        val pointer = normalizedPointer(event)
        return SigilInteractionPicker.rayFromCamera(pointer, cam)
    }

    private fun hitVolumeHits(ray: Ray, scene: Scene): List<SigilInteractionHit> {
        val hits = mutableListOf<SigilInteractionHit>()

        nodeMap.values.forEach { node ->
            if (!node.belongsToScene(scene)) return@forEach
            if (!node.isVisibleInHierarchy()) return@forEach
            val interaction = interactionForNode(node) ?: return@forEach
            if (!interaction.enabled || interaction.hitVolume == null) return@forEach

            SigilInteractionPicker.intersectHitVolume(ray, node, interaction)?.let { hit ->
                hits.add(hit)
            }
        }

        return hits
    }

    private fun meshRaycasterHits(ray: Ray, scene: Scene): List<SigilInteractionHit> {
        raycaster.ray.origin.copy(ray.origin)
        raycaster.ray.direction.copy(ray.direction)
        val intersections = raycaster.intersectObject(scene, true)
        val hits = mutableListOf<SigilInteractionHit>()

        for (intersection in intersections) {
            val candidate = findInteractiveNode(intersection.`object`) ?: continue
            if (!candidate.isVisibleInHierarchy()) continue
            hits.add(SigilInteractionHit(intersection, candidate))
        }

        return hits
    }

    private fun Object3D.belongsToScene(scene: Scene): Boolean {
        var current: Object3D? = this
        while (current != null) {
            if (current === scene) return true
            current = current.parent
        }
        return false
    }

    private fun normalizedPointer(event: MouseEvent): Vector2 {
        val rect = canvas.getBoundingClientRect()
        val width = rect.width.takeIf { it > 0.0 } ?: canvas.width.toDouble()
        val height = rect.height.takeIf { it > 0.0 } ?: canvas.height.toDouble()
        val x = (((event.clientX - rect.left) / width) * 2.0 - 1.0).toFloat()
        val y = (-(((event.clientY - rect.top) / height) * 2.0 - 1.0)).toFloat()
        return Vector2(x, y)
    }

    private fun findInteractiveNode(start: Object3D?): Object3D? {
        var current = start
        while (current != null) {
            val interaction = interactionForNode(current)
            if (interaction?.enabled == true) return current
            current = current.parent
        }
        return null
    }

    private fun Object3D.isVisibleInHierarchy(): Boolean {
        var current: Object3D? = this
        while (current != null) {
            if (!current.visible) return false
            current = current.parent
        }
        return true
    }

    private fun interactionForNode(node: Object3D): InteractionMetadata? {
        return node.userData["sigilInteraction"] as? InteractionMetadata
    }

    private fun isDropTargetNode(node: Object3D): Boolean {
        val dropTarget = interactionForNode(node)?.dropTarget ?: return false
        return dropTarget.enabled
    }

    private fun evaluateDrop(drag: ActiveDrag, candidate: Object3D): DropEvaluation {
        val targetInteraction = interactionForNode(candidate)
            ?: return DropEvaluation(accepted = false, result = "missing-target-interaction")
        val dropTarget = targetInteraction.dropTarget
            ?: return DropEvaluation(accepted = false, result = "missing-drop-target")
        if (!dropTarget.enabled) return DropEvaluation(accepted = false, result = "disabled-target")

        val sourceNodeId = drag.source.userData["sigilNodeId"] as? String
        val sourceInteractionId = drag.sourceInteraction.interactionId
        val acceptedKeys = buildList {
            sourceNodeId?.let { add(it) }
            sourceInteractionId?.let { add(it) }
            addAll(drag.sourceInteraction.actions)
        }

        if (dropTarget.accepts.isNotEmpty() && dropTarget.accepts.none { it in acceptedKeys }) {
            return DropEvaluation(accepted = false, result = "source-not-accepted")
        }

        val sourceGroups = drag.sourceInteraction.drag?.dropGroups.orEmpty()
        if (dropTarget.groups.isNotEmpty() && sourceGroups.none { it in dropTarget.groups }) {
            return DropEvaluation(accepted = false, result = "group-mismatch")
        }

        return DropEvaluation(accepted = true, result = "accepted")
    }

    private fun updateDragPosition(drag: ActiveDrag, event: MouseEvent) {
        val ray = rayForEvent(event) ?: return
        val position = SigilDragController.positionFor(drag.constraint, ray) ?: return
        drag.source.position.set(position.x, position.y, position.z)
        refreshNodeWorldMatrix(drag.source)
    }

    private fun refreshNodeWorldMatrix(node: Object3D) {
        node.updateWorldMatrix(updateParents = true, updateChildren = true)
    }

    private fun dropStateFor(evaluation: DropEvaluation): String =
        if (evaluation.accepted) "valid" else "invalid"

    private fun updateHoverDropTarget(target: Object3D?) {
        if (hoverDropTarget == target) return

        hoverDropTarget?.let { setDropTargetState(it, null) }
        hoverDropTarget = target
        target?.let { setDropTargetState(it, "hover") }
    }

    private fun setDropTargetState(node: Object3D, state: String?) {
        val previous = node.userData["sigilDropState"] as? String
        if (previous == state) return

        previous?.let { clearDropTargetVisual(node, it) }

        if (state == null) {
            node.userData.remove("sigilDropState")
        } else {
            node.userData["sigilDropState"] = state
            applyDropTargetVisual(node, state)
        }
    }

    private fun clearDropTargetVisual(node: Object3D, state: String) {
        if (dropTargetStatePatch(node, state) != null) {
            restoreNodeMaterialColor(node)
        }
    }

    private fun applyDropTargetVisual(node: Object3D, state: String) {
        val patch = dropTargetStatePatch(node, state) ?: return
        if (patch.active) {
            applyHighlightPatch(node, patch)
        } else {
            restoreNodeMaterialColor(node)
        }
    }

    private fun dropTargetStatePatch(node: Object3D, state: String): HighlightPatch? {
        val states = interactionForNode(node)?.dropTarget?.states ?: return null
        return when (state) {
            "hover" -> states.hover
            "active" -> states.active
            "valid" -> states.valid ?: states.active
            "invalid" -> states.invalid ?: states.active
            else -> null
        }
    }

    private fun dispatchSceneEvent(
        type: String,
        event: MouseEvent,
        node: Object3D,
        intersection: Intersection?,
        drag: ActiveDrag? = null
    ) {
        val detail = sceneEventDetail(type, event, node, intersection, drag)
        dispatchBrowserEvent("sigil:scene-event", detail)
        dispatchBrowserEvent("sigil:$type", detail)
        dispatchSceneEventBindings(sceneEventPayload(type, node, drag), event)

        if (type != "pointermove" && type != "drag") {
            (node.userData["sigilNodeId"] as? String)?.let { nodeId ->
                nodeDataMap[nodeId]?.let { scheduleAnimations(node, it.animations, AnimationTrigger.INTERACTION) }
            }
        }
    }

    private fun sceneEventDetail(
        type: String,
        event: MouseEvent,
        node: Object3D,
        intersection: Intersection?,
        drag: ActiveDrag?
    ): dynamic {
        val rect = canvas.getBoundingClientRect()
        val detail = js("{}")
        val interaction = interactionForNode(node)
        detail.type = type
        detail.nodeId = node.userData["sigilNodeId"] as? String
        detail.interactionId = interaction?.interactionId ?: node.userData["sigilInteractionId"] as? String
        detail.actions = interaction?.actions?.toTypedArray() ?: emptyArray<String>()
        detail.events = interaction?.events?.toTypedArray() ?: emptyArray<String>()
        detail.dropState = node.userData["sigilDropState"] as? String

        val pointer = js("{}")
        pointer.clientX = event.clientX
        pointer.clientY = event.clientY
        pointer.canvasX = event.clientX - rect.left
        pointer.canvasY = event.clientY - rect.top
        pointer.button = event.button.toInt()
        detail.pointer = pointer

        if (intersection != null) {
            val hit = js("{}")
            hit.distance = intersection.distance
            hit.objectId = intersection.`object`.id
            hit.objectName = intersection.`object`.name
            val point = js("{}")
            point.x = intersection.point.x
            point.y = intersection.point.y
            point.z = intersection.point.z
            hit.point = point
            detail.hit = hit
        }

        if (drag != null) {
            val dragPayload = js("{}")
            dragPayload.sourceNodeId = drag.source.userData["sigilNodeId"] as? String
            dragPayload.sourceInteractionId = drag.sourceInteraction.interactionId
            dragPayload.targetNodeId = drag.target?.userData?.get("sigilNodeId") as? String
            dragPayload.targetInteractionId = drag.target?.userData?.get("sigilInteractionId") as? String
            dragPayload.targetId = drag.target
                ?.let { interactionForNode(it)?.dropTarget?.targetId }
            dragPayload.targetState = drag.targetState
            dragPayload.accepted = drag.targetAccepted
            dragPayload.result = drag.dropResult ?: when (drag.targetAccepted) {
                true -> "accepted"
                false -> "rejected"
                null -> "pending"
            }
            dragPayload.sourceDropGroups = drag.sourceInteraction.drag?.dropGroups.orEmpty().toTypedArray()
            dragPayload.targetGroups = drag.target
                ?.let { interactionForNode(it)?.dropTarget?.groups.orEmpty().toTypedArray() }
                ?: emptyArray<String>()
            detail.drag = dragPayload
        }

        return detail
    }

    private fun sceneEventPayload(
        type: String,
        node: Object3D,
        drag: ActiveDrag?
    ): SigilSceneEventPayload {
        val interaction = interactionForNode(node)
        return SigilSceneEventPayload(
            type = type,
            nodeId = node.userData["sigilNodeId"] as? String,
            interactionId = interaction?.interactionId ?: node.userData["sigilInteractionId"] as? String,
            actions = interaction?.actions.orEmpty(),
            events = interaction?.events.orEmpty(),
            dropState = node.userData["sigilDropState"] as? String,
            drag = drag?.let(::dragPayload)
        )
    }

    private fun dragPayload(drag: ActiveDrag): SigilSceneDragPayload =
        SigilSceneDragPayload(
            sourceNodeId = drag.source.userData["sigilNodeId"] as? String,
            sourceInteractionId = drag.sourceInteraction.interactionId,
            targetNodeId = drag.target?.userData?.get("sigilNodeId") as? String,
            targetInteractionId = drag.target?.userData?.get("sigilInteractionId") as? String,
            targetId = drag.target?.let { interactionForNode(it)?.dropTarget?.targetId },
            targetState = drag.targetState,
            accepted = drag.targetAccepted,
            result = drag.dropResult ?: when (drag.targetAccepted) {
                true -> "accepted"
                false -> "rejected"
                null -> "pending"
            },
            sourceDropGroups = drag.sourceInteraction.drag?.dropGroups.orEmpty(),
            targetGroups = drag.target
                ?.let { interactionForNode(it)?.dropTarget?.groups.orEmpty() }
                ?: emptyList()
        )

    private fun dispatchSceneEventBindings(payload: SigilSceneEventPayload, event: MouseEvent) {
        if (sceneEventBindings.isEmpty()) return

        sceneEventBindings.forEach { binding ->
            if (!binding.match.matches(payload)) return@forEach

            if (binding.preventDefault) event.preventDefault()
            if (binding.stopPropagation) event.stopPropagation()

            invokeSceneEventBinding(binding, payload)
        }
    }

    private fun invokeSceneEventBinding(binding: SigilSceneEventBinding, payload: SigilSceneEventPayload) {
        val requestKey = binding.requestKey ?: binding.callbackId?.let { "callback:$it" }
        if (!requestGate.tryAcquire(requestKey, binding.suppressWhilePending)) return
        binding.optimisticPatch?.let(::applyPatch)
        var waitsForCallback = false

        binding.localHandlerId
            ?.let(localSceneEventHandlers::get)
            ?.let { handler ->
                try {
                    handler.invoke()
                } catch (t: Throwable) {
                    console.error("Sigil: Scene event handler failed: ${t.message}")
                }
            }

        binding.callbackId?.let { callbackId ->
            waitsForCallback = true
            postSummonCallback(callbackId, binding.callbackUrl, binding.reloadOnSuccess) {
                requestGate.release(requestKey)
            }
        }

        binding.url?.let { url ->
            window.location.href = expandSceneEventUrl(url, payload)
        }

        if (!waitsForCallback) requestGate.release(requestKey)
    }

    private fun postSummonCallback(
        callbackId: String,
        callbackUrl: String?,
        reloadOnSuccess: Boolean?,
        onComplete: () -> Unit
    ) {
        var completed = false
        fun completeOnce() {
            if (!completed) {
                completed = true
                onComplete()
            }
        }
        val options = js("{}")
        options.method = "POST"
        val headers = js("{}")
        headers["X-Requested-With"] = "XMLHttpRequest"
        options.headers = headers

        val url = callbackUrl ?: "/summon/callback/${encodeURIComponent(callbackId)}"
        window.asDynamic().fetch(url, options)
            .then({ response: dynamic ->
                response.text()
                    .then({ bodyText: String ->
                        val callbackResponse = parseCallbackResponse(bodyText)
                        val appliedPatch = callbackResponse?.let(::applyCallbackResponse) == true
                        val responseWantsReload = callbackResponse?.wantsReload == true
                        if (
                            reloadOnSuccess == true ||
                            (reloadOnSuccess == null && responseWantsReload && !appliedPatch)
                        ) {
                            window.location.reload()
                        }
                        completeOnce()
                        null
                    })
                    .catch({ _: dynamic ->
                        if (reloadOnSuccess == true) {
                            window.location.reload()
                        }
                        completeOnce()
                        null
                    })
            })
            .catch({ error: dynamic ->
                console.error("Sigil: Summon scene callback failed:", error)
                completeOnce()
                null
            })
    }

    private fun parseCallbackResponse(bodyText: String): SigilSceneEventCallbackResponse? {
        if (bodyText.isBlank()) return null
        return try {
            SigilJson.decodeFromString<SigilSceneEventCallbackResponse>(bodyText)
        } catch (e: Exception) {
            console.warn("Sigil: Scene callback returned non-patch response: ${e.message}")
            null
        }
    }

    private fun applyCallbackResponse(response: SigilSceneEventCallbackResponse): Boolean {
        var applied = false

        response.scenePatchesFor(canvas.id).forEach { patch ->
            applyPatch(patch)
            applied = true
        }

        response.domPatchesToApply().forEach { patch ->
            if (applyDomPatch(patch)) {
                applied = true
            }
        }

        if (applied || response.action != null || response.status != null) {
            dispatchSceneActionResponse(response, applied)
        }

        return applied
    }

    private fun applyDomPatch(patch: SigilDomPatch): Boolean {
        val element = document.querySelector(patch.selector) ?: return false
        when (patch.mode) {
            SigilDomPatchMode.INNER_HTML -> {
                element.innerHTML = patch.html ?: patch.text ?: return false
            }
            SigilDomPatchMode.OUTER_HTML -> {
                element.outerHTML = patch.html ?: patch.text ?: return false
            }
            SigilDomPatchMode.TEXT_CONTENT -> {
                element.textContent = patch.text ?: patch.html ?: return false
            }
            SigilDomPatchMode.REMOVE -> {
                element.parentElement?.removeChild(element) ?: return false
            }
        }
        return true
    }

    private fun dispatchSceneActionResponse(response: SigilSceneEventCallbackResponse, appliedPatch: Boolean) {
        val detail = js("{}")
        detail.action = response.action
        detail.status = response.status
        detail.appliedPatch = appliedPatch
        detail.canvasId = canvas.id
        dispatchBrowserEvent("sigil:scene-action-response", detail)
    }

    private fun expandSceneEventUrl(template: String, payload: SigilSceneEventPayload): String {
        val drag = payload.drag
        val replacements = mapOf(
            "type" to payload.type,
            "nodeId" to payload.nodeId.orEmpty(),
            "interactionId" to payload.interactionId.orEmpty(),
            "dropState" to payload.dropState.orEmpty(),
            "sourceNodeId" to drag?.sourceNodeId.orEmpty(),
            "sourceInteractionId" to drag?.sourceInteractionId.orEmpty(),
            "targetNodeId" to drag?.targetNodeId.orEmpty(),
            "targetInteractionId" to drag?.targetInteractionId.orEmpty(),
            "targetId" to drag?.targetId.orEmpty(),
            "targetState" to drag?.targetState.orEmpty(),
            "accepted" to (drag?.accepted?.toString() ?: ""),
            "result" to drag?.result.orEmpty()
        )
        return replacements.entries.fold(template) { current, (key, value) ->
            current.replace("\${$key}", encodeURIComponent(value))
        }
    }

    private fun dispatchBrowserEvent(eventName: String, detail: dynamic) {
        val canvasEvent = js("new CustomEvent(eventName, { detail: detail, bubbles: true })")
        canvas.dispatchEvent(canvasEvent.unsafeCast<Event>())
        val windowEvent = js("new CustomEvent(eventName, { detail: detail })")
        window.dispatchEvent(windowEvent.unsafeCast<Event>())
    }

    private fun setCanvasCursor(cursor: CursorHint) {
        val cssCursor = when (cursor) {
            CursorHint.AUTO -> "auto"
            CursorHint.POINTER -> "pointer"
            CursorHint.GRAB -> "grab"
            CursorHint.GRABBING -> "grabbing"
            CursorHint.CROSSHAIR -> "crosshair"
            CursorHint.NONE -> "none"
        }
        canvas.style.setProperty("cursor", cssCursor)
    }

    private fun triggerAnimations(trigger: AnimationTrigger) {
        nodeDataMap.forEach { (nodeId, nodeData) ->
            val node = nodeMap[nodeId] ?: return@forEach
            scheduleAnimations(node, nodeData.animations, trigger)
        }
    }

    private fun scheduleAnimations(
        node: Object3D,
        animations: List<SceneAnimationData>,
        trigger: AnimationTrigger
    ) {
        val now = window.performance.now()
        animations
            .filter { it.trigger == trigger }
            .forEach { animation ->
                activeAnimations.add(
                    ActiveSceneAnimation(
                        node = node,
                        data = animation,
                        startedAtMs = now,
                        basePosition = listOf(node.position.x, node.position.y, node.position.z),
                        baseScale = listOf(node.scale.x, node.scale.y, node.scale.z),
                        baseVisible = node.visible
                    )
                )
            }
    }

    private fun updateSceneAnimations(now: Double) {
        if (activeAnimations.isEmpty()) return

        val iterator = activeAnimations.iterator()
        while (iterator.hasNext()) {
            val active = iterator.next()
            val duration = active.data.durationMs.coerceAtLeast(1).toDouble()
            val delay = active.data.delayMs.coerceAtLeast(0).toDouble()
            val repeatCount = active.data.repeat.coerceAtLeast(0) + 1
            val elapsed = now - active.startedAtMs - delay
            if (elapsed < 0.0) continue

            val totalDuration = duration * repeatCount
            val finished = elapsed >= totalDuration
            val cycleElapsed = if (finished) duration else elapsed % duration
            val progress = (cycleElapsed / duration).toFloat().coerceIn(0f, 1f)
            val eased = easedProgress(progress, active.data.easing)

            applySceneAnimationFrame(active, eased, progress)

            if (finished) {
                finishSceneAnimation(active)
                iterator.remove()
            }
        }
    }

    private fun applySceneAnimationFrame(active: ActiveSceneAnimation, eased: Float, progress: Float) {
        val node = active.node
        val data = active.data
        var transformChanged = false
        when (data.kind) {
            AnimationKind.SLIDE -> {
                val vector = data.vector ?: listOf(0f, 0.25f, 0f)
                node.position.set(
                    active.basePosition[0] + vector.getOrElse(0) { 0f } * eased * data.intensity,
                    active.basePosition[1] + vector.getOrElse(1) { 0f } * eased * data.intensity,
                    active.basePosition[2] + vector.getOrElse(2) { 0f } * eased * data.intensity
                )
                transformChanged = true
            }
            AnimationKind.BOB -> {
                val height = (data.vector?.getOrNull(1) ?: 0.2f) * data.intensity
                node.position.y = active.basePosition[1] + sin(progress * PI.toFloat() * 2f) * height
                transformChanged = true
            }
            AnimationKind.THUNK,
            AnimationKind.BOUNCE,
            AnimationKind.PULSE -> {
                val pulse = 1f + sin(progress * PI.toFloat()) * 0.12f * data.intensity
                node.scale.set(
                    active.baseScale[0] * pulse,
                    active.baseScale[1] * pulse,
                    active.baseScale[2] * pulse
                )
                transformChanged = true
            }
            AnimationKind.SHAKE,
            AnimationKind.GLITCH -> {
                val amount = 0.05f * data.intensity * (1f - progress)
                node.position.x = active.basePosition[0] + sin(progress * PI.toFloat() * 18f) * amount
                node.position.y = active.basePosition[1] + sin(progress * PI.toFloat() * 23f) * amount
                transformChanged = true
            }
            AnimationKind.TINT,
            AnimationKind.SUCCESS,
            AnimationKind.FAILURE -> {
                val color = data.color ?: when (data.kind) {
                    AnimationKind.SUCCESS -> 0xFF22C55E.toInt()
                    AnimationKind.FAILURE -> 0xFFEF4444.toInt()
                    else -> 0xFFFFD166.toInt()
                }
                setNodeMaterialColor(node, intToColor(color), storeOriginal = true)
            }
            AnimationKind.VISIBILITY -> {
                node.visible = eased >= 0.5f
            }
        }
        if (transformChanged) {
            refreshNodeWorldMatrix(node)
        }
    }

    private fun finishSceneAnimation(active: ActiveSceneAnimation) {
        when (active.data.kind) {
            AnimationKind.BOB,
            AnimationKind.THUNK,
            AnimationKind.BOUNCE,
            AnimationKind.PULSE,
            AnimationKind.SHAKE,
            AnimationKind.GLITCH -> {
                active.node.position.set(active.basePosition[0], active.basePosition[1], active.basePosition[2])
                active.node.scale.set(active.baseScale[0], active.baseScale[1], active.baseScale[2])
                refreshNodeWorldMatrix(active.node)
            }
            AnimationKind.TINT,
            AnimationKind.SUCCESS,
            AnimationKind.FAILURE -> restoreNodeMaterialColor(active.node)
            AnimationKind.VISIBILITY -> active.node.visible = active.baseVisible
            else -> Unit
        }
    }

    private fun easedProgress(progress: Float, easing: AnimationEasing): Float {
        return when (easing) {
            AnimationEasing.LINEAR -> progress
            AnimationEasing.EASE_IN -> progress * progress
            AnimationEasing.EASE_OUT -> 1f - (1f - progress).pow(2)
            AnimationEasing.EASE_IN_OUT -> {
                if (progress < 0.5f) {
                    2f * progress * progress
                } else {
                    1f - (-2f * progress + 2f).pow(2) / 2f
                }
            }
        }
    }

    private fun setNodeMaterialColor(node: Object3D, color: Color, storeOriginal: Boolean) {
        node.traverse { traversed ->
            val mesh = traversed as? Mesh ?: return@traverse
            setMaterialColor(mesh.material, color, storeOriginal)
        }
    }

    private fun setMaterialColor(material: Material?, color: Color, storeOriginal: Boolean) {
        when (material) {
            is MeshStandardMaterial -> {
                if (storeOriginal && material.userData["sigilOriginalColor"] == null) {
                    material.userData["sigilOriginalColor"] = material.color.clone()
                }
                material.color = color
                material.needsUpdate = true
            }
            is MeshBasicMaterial -> {
                if (storeOriginal && material.userData["sigilOriginalColor"] == null) {
                    material.userData["sigilOriginalColor"] = material.color.clone()
                }
                material.color = color
                material.needsUpdate = true
            }
        }
    }

    private fun restoreNodeMaterialColor(node: Object3D) {
        node.traverse { traversed ->
            val mesh = traversed as? Mesh ?: return@traverse
            restoreMaterialColor(mesh.material)
        }
    }

    private fun restoreMaterialColor(material: Material?) {
        when (material) {
            is MeshStandardMaterial -> {
                val original = material.userData.remove("sigilOriginalColor") as? Color ?: return
                material.color = original
                material.needsUpdate = true
            }
            is MeshBasicMaterial -> {
                val original = material.userData.remove("sigilOriginalColor") as? Color ?: return
                material.color = original
                material.needsUpdate = true
            }
        }
    }

    private fun createModelFallback(data: ModelData): Mesh {
        val geometry = BoxGeometry(1f, 1f, 1f)
        val material = MeshBasicMaterial().apply {
            color = Color(0.9f, 0.2f, 0.2f)
            wireframe = true
        }

        return Mesh(geometry, material).apply {
            castShadow = data.castShadow
            receiveShadow = data.receiveShadow
            name = "SigilModelFallback"
        }
    }

    private fun attachOrbitControlsHandlers(controls: OrbitControls): () -> Unit {
        val canvasElement = canvas
        canvasElement.style.setProperty("touch-action", "none")

        var activeButton: PointerButton? = null

        fun pointerPosition(event: MouseEvent): Pair<Float, Float> {
            val rect = canvasElement.getBoundingClientRect()
            val x = event.clientX - rect.left
            val y = event.clientY - rect.top
            return Pair(x.toFloat(), y.toFloat())
        }

        val mouseDown: (Event) -> Unit = mouseDown@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseDown
            activeCameraPatch = null
            val button = toPointerButton(mouseEvent.button)
            val (x, y) = pointerPosition(mouseEvent)
            activeButton = button
            controls.onPointerDown(x, y, button)
            mouseEvent.preventDefault()
        }

        val mouseMove: (Event) -> Unit = mouseMove@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseMove
            val button = activeButton ?: return@mouseMove
            val (x, y) = pointerPosition(mouseEvent)
            controls.onPointerMove(x, y, button)
            mouseEvent.preventDefault()
        }

        val mouseUp: (Event) -> Unit = mouseUp@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseUp
            val button = activeButton ?: toPointerButton(mouseEvent.button)
            val (x, y) = pointerPosition(mouseEvent)
            controls.onPointerUp(x, y, button)
            activeButton = null
            mouseEvent.preventDefault()
        }

        val wheelHandler: (Event) -> Unit = wheelHandler@{ event ->
            val wheelEvent = event as? WheelEvent ?: return@wheelHandler
            activeCameraPatch = null
            controls.onWheel(wheelEvent.deltaX.toFloat(), wheelEvent.deltaY.toFloat())
            wheelEvent.preventDefault()
        }

        val contextMenu: (Event) -> Unit = contextMenu@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@contextMenu
            mouseEvent.preventDefault()
        }

        val keyDown: (Event) -> Unit = keyDown@{ event ->
            val keyEvent = event as? KeyboardEvent ?: return@keyDown
            toControlsKey(keyEvent.key)?.let { key ->
                activeCameraPatch = null
                controls.onKeyDown(key)
                keyEvent.preventDefault()
            }
        }

        val keyUp: (Event) -> Unit = keyUp@{ event ->
            val keyEvent = event as? KeyboardEvent ?: return@keyUp
            toControlsKey(keyEvent.key)?.let { key ->
                controls.onKeyUp(key)
                keyEvent.preventDefault()
            }
        }

        canvasElement.addEventListener("mousedown", mouseDown)
        canvasElement.addEventListener("mousemove", mouseMove)
        canvasElement.addEventListener("mouseup", mouseUp)
        canvasElement.addEventListener("wheel", wheelHandler)
        canvasElement.addEventListener("contextmenu", contextMenu)
        window.addEventListener("keydown", keyDown)
        window.addEventListener("keyup", keyUp)

        return {
            canvasElement.removeEventListener("mousedown", mouseDown)
            canvasElement.removeEventListener("mousemove", mouseMove)
            canvasElement.removeEventListener("mouseup", mouseUp)
            canvasElement.removeEventListener("wheel", wheelHandler)
            canvasElement.removeEventListener("contextmenu", contextMenu)
            window.removeEventListener("keydown", keyDown)
            window.removeEventListener("keyup", keyUp)
        }
    }

    private fun attachFirstPersonControlsHandlers(
        controls: FirstPersonControls,
        enablePointerLock: Boolean
    ): () -> Unit {
        val canvasElement = canvas
        canvasElement.style.setProperty("touch-action", "none")

        var lastMouseX = 0f
        var lastMouseY = 0f
        var hasLastMouse = false

        val clickHandler: (Event) -> Unit = clickHandler@{ event ->
            if (!enablePointerLock) return@clickHandler
            val mouseEvent = event as? MouseEvent ?: return@clickHandler
            mouseEvent.preventDefault()
            // Request pointer lock on canvas
            canvasElement.asDynamic().requestPointerLock()
        }

        val pointerLockChange: (Event) -> Unit = {
            val locked = document.asDynamic().pointerLockElement == canvasElement
            if (locked) {
                controls.requestPointerLock()
            } else {
                controls.exitPointerLock()
            }
        }

        val mouseDown: (Event) -> Unit = mouseDown@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseDown
            val rect = canvasElement.getBoundingClientRect()
            val x = (mouseEvent.clientX - rect.left).toFloat()
            val y = (mouseEvent.clientY - rect.top).toFloat()
            controls.onPointerDown(x, y, toPointerButton(mouseEvent.button))
            lastMouseX = x
            lastMouseY = y
            hasLastMouse = true
            mouseEvent.preventDefault()
        }

        val mouseMove: (Event) -> Unit = mouseMove@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseMove
            if (controls.isPointerLocked()) {
                // Use movementX/movementY for pointer lock mode
                val dx = (mouseEvent.asDynamic().movementX as? Number)?.toFloat() ?: 0f
                val dy = (mouseEvent.asDynamic().movementY as? Number)?.toFloat() ?: 0f
                controls.onPointerMove(dx, dy, PointerButton.PRIMARY)
            } else {
                val rect = canvasElement.getBoundingClientRect()
                val x = (mouseEvent.clientX - rect.left).toFloat()
                val y = (mouseEvent.clientY - rect.top).toFloat()
                if (hasLastMouse) {
                    controls.onPointerMove(x - lastMouseX, y - lastMouseY, PointerButton.PRIMARY)
                }
                lastMouseX = x
                lastMouseY = y
                hasLastMouse = true
            }
            mouseEvent.preventDefault()
        }

        val mouseUp: (Event) -> Unit = mouseUp@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseUp
            val rect = canvasElement.getBoundingClientRect()
            val x = (mouseEvent.clientX - rect.left).toFloat()
            val y = (mouseEvent.clientY - rect.top).toFloat()
            controls.onPointerUp(x, y, toPointerButton(mouseEvent.button))
            mouseEvent.preventDefault()
        }

        val contextMenu: (Event) -> Unit = contextMenu@{ event ->
            event.preventDefault()
        }

        val keyDown: (Event) -> Unit = keyDown@{ event ->
            val keyEvent = event as? KeyboardEvent ?: return@keyDown
            toControlsKey(keyEvent.key)?.let { key ->
                controls.onKeyDown(key)
                keyEvent.preventDefault()
            }
        }

        val keyUp: (Event) -> Unit = keyUp@{ event ->
            val keyEvent = event as? KeyboardEvent ?: return@keyUp
            toControlsKey(keyEvent.key)?.let { key ->
                controls.onKeyUp(key)
                keyEvent.preventDefault()
            }
        }

        canvasElement.addEventListener("click", clickHandler)
        canvasElement.addEventListener("mousedown", mouseDown)
        canvasElement.addEventListener("mousemove", mouseMove)
        canvasElement.addEventListener("mouseup", mouseUp)
        canvasElement.addEventListener("contextmenu", contextMenu)
        window.addEventListener("keydown", keyDown)
        window.addEventListener("keyup", keyUp)
        document.addEventListener("pointerlockchange", pointerLockChange)

        return {
            canvasElement.removeEventListener("click", clickHandler)
            canvasElement.removeEventListener("mousedown", mouseDown)
            canvasElement.removeEventListener("mousemove", mouseMove)
            canvasElement.removeEventListener("mouseup", mouseUp)
            canvasElement.removeEventListener("contextmenu", contextMenu)
            window.removeEventListener("keydown", keyDown)
            window.removeEventListener("keyup", keyUp)
            document.removeEventListener("pointerlockchange", pointerLockChange)
        }
    }

    private fun toPointerButton(button: Short): PointerButton {
        return when (button.toInt()) {
            1 -> PointerButton.AUXILIARY
            2 -> PointerButton.SECONDARY
            else -> PointerButton.PRIMARY
        }
    }

    private fun toControlsKey(key: String): Key? {
        return when (key.lowercase()) {
            "w" -> Key.W
            "a" -> Key.A
            "s" -> Key.S
            "d" -> Key.D
            "q" -> Key.Q
            "e" -> Key.E
            "shift" -> Key.SHIFT
            " " -> Key.SPACE
            "space" -> Key.SPACE
            "arrowup" -> Key.ARROW_UP
            "arrowdown" -> Key.ARROW_DOWN
            "arrowleft" -> Key.ARROW_LEFT
            "arrowright" -> Key.ARROW_RIGHT
            "escape" -> Key.ESCAPE
            "enter" -> Key.ENTER
            else -> null
        }
    }

    private fun createLight(data: LightData) {
        // Create the light using Impl classes
        val light: Light = when (data.lightType) {
            LightType.AMBIENT -> AmbientLightImpl(
                intToColor(data.color),
                data.intensity
            )
            LightType.DIRECTIONAL -> DirectionalLightImpl(
                intToColor(data.color),
                data.intensity,
                Vector3(
                    data.target[0] - data.position[0],
                    data.target[1] - data.position[1],
                    data.target[2] - data.position[2]
                ).normalize(),
                Vector3(data.position[0], data.position[1], data.position[2])
            )
            LightType.POINT -> PointLightImpl(
                intToColor(data.color),
                data.intensity,
                Vector3(data.position[0], data.position[1], data.position[2]),
                2f, // decay
                data.distance
            )
            LightType.SPOT -> SpotLightImpl(
                intToColor(data.color),
                data.intensity,
                Vector3(data.position[0], data.position[1], data.position[2]),
                Vector3(
                    data.target[0] - data.position[0],
                    data.target[1] - data.position[1],
                    data.target[2] - data.position[2]
                ).normalize(),
                data.angle,
                data.penumbra,
                data.decay,
                data.distance
            )
            LightType.HEMISPHERE -> HemisphereLightImpl(
                intToColor(data.color),
                Color(0.2f, 0.2f, 0.2f),
                data.intensity,
                Vector3(data.position[0], data.position[1], data.position[2])
            )
        }

        // Add light to lighting system
        lightingSystem.addLight(light)
        lights.add(light)
        materiaScene.userData["lights"] = lights.toList()
    }

    private fun createGeometry(type: GeometryType, params: GeometryParams): BufferGeometry {
        return when (type) {
            GeometryType.BOX -> BoxGeometry(
                width = params.width,
                height = params.height,
                depth = params.depth,
                widthSegments = params.widthSegments,
                heightSegments = params.heightSegments,
                depthSegments = params.widthSegments
            )
            GeometryType.SPHERE -> SphereGeometry(
                radius = params.radius,
                widthSegments = params.widthSegments.coerceAtLeast(8),
                heightSegments = params.heightSegments.coerceAtLeast(6)
            )
            GeometryType.PLANE -> PlaneGeometry(
                width = params.width,
                height = params.height,
                widthSegments = params.widthSegments,
                heightSegments = params.heightSegments
            )
            GeometryType.CYLINDER -> CylinderGeometry(
                radiusTop = params.radiusTop,
                radiusBottom = params.radiusBottom,
                height = params.height,
                radialSegments = params.radialSegments,
                heightSegments = params.heightSegments,
                openEnded = params.openEnded
            )
            GeometryType.CONE -> ConeGeometry(
                radius = params.radius,
                height = params.height,
                radialSegments = params.radialSegments,
                heightSegments = params.heightSegments,
                openEnded = params.openEnded
            )
            GeometryType.TORUS -> TorusGeometry(
                radius = params.radius,
                tube = params.tube,
                radialSegments = params.radialSegments,
                tubularSegments = params.tubularSegments
            )
            GeometryType.CIRCLE -> CircleGeometry(
                radius = params.radius,
                segments = params.radialSegments
            )
            GeometryType.RING -> RingGeometry(
                innerRadius = params.innerRadius,
                outerRadius = params.outerRadius,
                thetaSegments = params.radialSegments
            )
            GeometryType.ICOSAHEDRON -> IcosahedronGeometry(
                radius = params.radius,
                detail = params.detail
            )
            GeometryType.OCTAHEDRON -> OctahedronGeometry(
                radius = params.radius,
                detail = params.detail
            )
            GeometryType.TETRAHEDRON -> TetrahedronGeometry(
                radius = params.radius,
                detail = params.detail
            )
            GeometryType.DODECAHEDRON -> DodecahedronGeometry(
                radius = params.radius,
                detail = params.detail
            )
        }
    }

    private fun intToColor(argb: Int): Color {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return Color(r, g, b)
    }

    private fun List<Float>.toVector3(): Vector3 = Vector3(
        getOrElse(0) { 0f },
        getOrElse(1) { 0f },
        getOrElse(2) { 0f }
    )
}

@Suppress("UnsafeCastFromDynamic")
private fun encodeURIComponent(value: String): String =
    js("encodeURIComponent(value)") as String

/**
 * Register the global SigilHydrator for external access.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
object SigilHydratorGlobal {
    private val hydrators = mutableMapOf<String, SigilHydrator>()

    fun hydrate(canvasId: String, sceneData: dynamic, sceneEventBindingsData: dynamic = null) {
        val jsonString = JSON.stringify(sceneData)
        val scene = SigilScene.fromJson(jsonString)
        val sceneEventBindings = parseSceneEventBindings(sceneEventBindingsData)
        val element = document.getElementById(canvasId) ?: return

        val canvas: HTMLCanvasElement = when (element) {
            is HTMLCanvasElement -> element  // Server-rendered canvas — reuse
            is HTMLDivElement -> {           // Legacy div placeholder — create canvas
                val c = document.createElement("canvas") as HTMLCanvasElement
                c.style.width = "100%"
                c.style.height = "100%"
                c.style.display = "block"
                element.style.backgroundColor = "transparent"
                element.innerHTML = ""
                element.appendChild(c)
                c
            }
            else -> return
        }

        val rect = (canvas.parentElement ?: canvas).let {
            it.asDynamic().getBoundingClientRect()
        }
        canvas.width = (rect.width as Number).toInt()
        canvas.height = (rect.height as Number).toInt()

        scope.launch {
            val hydrator = SigilHydrator(
                canvas = canvas,
                sceneData = scene,
                sceneEventBindings = sceneEventBindings
            )
            hydrators[canvasId]?.dispose()
            hydrators[canvasId] = hydrator
            hydrator.initialize()
            hydrator.startRenderLoop()
        }
    }

    fun patch(canvasId: String, patchData: dynamic) {
        val hydrator = hydrators[canvasId] ?: return
        val jsonString = if (js("typeof patchData === 'string'") as Boolean) {
            patchData as String
        } else {
            JSON.stringify(patchData)
        }
        val patch = ScenePatch.fromJson(jsonString)
        hydrator.applyPatch(patch)
    }

    fun dispose(canvasId: String) {
        hydrators.remove(canvasId)?.dispose()
    }

    private fun parseSceneEventBindings(data: dynamic): List<SigilSceneEventBinding> {
        if (js("data == null") as Boolean) return emptyList()

        val jsonString = if (js("typeof data === 'string'") as Boolean) {
            data as String
        } else {
            JSON.stringify(data)
        }

        return try {
            SigilJson.decodeFromString(ListSerializer(SigilSceneEventBinding.serializer()), jsonString)
        } catch (e: Exception) {
            console.error("Sigil: Failed to parse scene event actions: ${e.message}")
            emptyList()
        }
    }
}
