package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.SigilScene
import codes.yousef.sigil.schema.SigilNodeData
import codes.yousef.sigil.schema.MeshData
import codes.yousef.sigil.schema.ModelData
import codes.yousef.sigil.schema.ModelMaterialOverride
import codes.yousef.sigil.schema.GroupData
import codes.yousef.sigil.schema.LightData
import codes.yousef.sigil.schema.CameraData
import codes.yousef.sigil.schema.ControlsData
import codes.yousef.sigil.schema.ControlsType
import codes.yousef.sigil.schema.AnimationEasing
import codes.yousef.sigil.schema.AnimationKind
import codes.yousef.sigil.schema.AnimationTrigger
import codes.yousef.sigil.schema.CursorHint
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.GeometryParams
import codes.yousef.sigil.schema.HighlightPatch
import codes.yousef.sigil.schema.InteractionMetadata
import codes.yousef.sigil.schema.LightType
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
import io.materia.renderer.TextureFilter
import io.materia.renderer.webgpu.WebGPURenderer
import io.materia.renderer.webgl.WebGLRenderer
import io.materia.renderer.RendererConfig
import io.materia.texture.Texture
import io.materia.texture.Texture2D
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent
import kotlin.js.ExperimentalJsExport
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

private val scope = MainScope()

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

private data class DropEvaluation(
    val accepted: Boolean,
    val result: String
)

private class SigilRelativeAssetResolver(
    private val modelUrl: String,
    private val delegate: AssetResolver = AssetResolver.default()
) : AssetResolver {
    private var transformedGlbJson: String? = null

    override suspend fun load(uri: String, basePath: String?): ByteArray {
        if (uri == modelUrl) return loadModelDocument(basePath)
        val resolved = SigilGltfMetadata.resolveAssetPath(uri, basePath, modelUrl)
        return loadDelegateWithRetry(resolved, null)
    }

    private suspend fun loadModelDocument(basePath: String?): ByteArray {
        if (!SigilGltfMetadata.isGlbUrl(modelUrl)) return loadDelegateWithRetry(modelUrl, basePath)

        val json = transformedGlbJson ?: loadGlbDocumentWithCompanionFallback(basePath)
            .also { transformedGlbJson = it }
        return json.encodeToByteArray()
    }

    private suspend fun loadGlbDocumentWithCompanionFallback(basePath: String?): String {
        val glbJson = SigilGltfMetadata.glbToGltfJson(loadDelegateWithRetry(modelUrl, basePath))
        val companionUrl = SigilGltfMetadata.companionGltfUrlForGlb(modelUrl) ?: return glbJson
        val companionJson = try {
            loadDelegateWithRetry(companionUrl, null).decodeToString()
        } catch (_: Throwable) {
            return glbJson
        }

        return if (SigilGltfMetadata.shouldPreferCompanionGltf(glbJson, companionJson)) {
            console.log("Sigil: Using expanded glTF companion for GLB fidelity: $companionUrl")
            SigilGltfMetadata.rewriteRelativeAssetUris(companionJson, companionUrl)
        } else {
            glbJson
        }
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
    private val sceneData: SigilScene
) {
    private var canvas: HTMLCanvasElement = canvas
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
    private var activeDrag: ActiveDrag? = null
    private var hoverDropTarget: Object3D? = null
    private val activeAnimations = mutableListOf<ActiveSceneAnimation>()
    private var lastFrameTimeMs: Double = 0.0
    private var rendererCanvasMayNeedReplacement = false

    suspend fun initialize() {
        // Configure scene from settings
        applySceneSettings(sceneData.settings)

        // Create camera
        camera = PerspectiveCamera(
            fov = 75f,
            aspect = canvas.width.toFloat() / canvas.height.toFloat(),
            near = 0.1f,
            far = 1000f
        ).apply {
            position.set(0f, 2f, 5f)
            lookAt(Vector3.ZERO)
        }

        // Hydrate scene nodes
        for (nodeData in sceneData.rootNodes) {
            val materiaNode = createMateriaNode(nodeData)
            if (materiaNode != null) {
                materiaScene.add(materiaNode)
                registerNode(nodeData, materiaNode)
            }
        }

        val initialized = initializeRenderer(RendererConfig())

        if (!initialized) {
            console.error("Sigil: No renderer could be initialized (tried WebGPU and WebGL)")
            return
        }

        interactionCleanup = attachInteractionHandlers()
        triggerAnimations(AnimationTrigger.SCENE_LOAD)

        // Handle resize
        window.onresize = {
            val rect = canvas.parentElement?.getBoundingClientRect()
            if (rect != null) {
                canvas.width = rect.width.toInt()
                canvas.height = rect.height.toInt()
                renderer?.resize(canvas.width, canvas.height)
                camera?.let { c ->
                    c.aspect = canvas.width.toFloat() / canvas.height.toFloat()
                    c.updateProjectionMatrix()
                }
            }
            Unit
        }
    }

    private suspend fun initializeRenderer(config: RendererConfig): Boolean {
        val preferWebGl = SigilRendererPolicy.preferWebGlFirst(
            userAgent = window.navigator.userAgent,
            webdriver = (window.navigator.asDynamic().webdriver as? Boolean) == true,
            rendererOverride = rendererOverride()
        )

        if (preferWebGl) {
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
        renderer.resize(canvas.width, canvas.height)
        (renderer as? WebGPURenderer)?.clearColor = intToColor(sceneData.settings.backgroundColor)
        if (renderer is WebGLRenderer) {
            bakeSceneTexturesForWebGl()
        }
    }

    private fun rendererOverride(): String? {
        val globalOverride = window.asDynamic().__SIGIL_RENDERER__ as? String
        if (!globalOverride.isNullOrBlank()) return globalOverride

        val legacyOverride = window.asDynamic().SIGIL_RENDERER as? String
        if (!legacyOverride.isNullOrBlank()) return legacyOverride

        val params = js("new URLSearchParams(window.location.search)").unsafeCast<dynamic>()
        return (params.get("sigilRenderer") as? String)
            ?: (params.get("renderer") as? String)
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
            val deltaSeconds = ((now - lastFrameTimeMs) / 1000.0).toFloat()
            lastFrameTimeMs = now

            orbitControls?.update(deltaSeconds)
            firstPersonControls?.update(deltaSeconds)
            updateSceneAnimations(now)
            
            materiaScene.updateMatrixWorld(true)
            val r = renderer ?: return
            cam.updateMatrixWorld()
            cam.updateProjectionMatrix()
            try {
                r.render(materiaScene, cam)
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
        activeAnimations.clear()
        activeDrag = null
        renderer?.dispose()
        renderer = null
    }

    private fun applySceneSettings(settings: SceneSettings) {
        materiaScene.background = Background.Color(intToColor(settings.backgroundColor))
    }

    private fun createMateriaNode(nodeData: SigilNodeData): Object3D? {
        return when (nodeData) {
            is MeshData -> createMesh(nodeData)
            is ModelData -> createModel(nodeData)
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
        }

        scope.launch {
            try {
                val assetResolver = SigilRelativeAssetResolver(data.url)
                val asset = GLTFLoader(assetResolver).load(data.url)
                val root = asset.scene
                hydrateGltfBaseColorTextures(asset.materials, data.url, assetResolver)
                applyModelSettings(root, data)
                group.clear()
                group.add(root)
            } catch (t: Throwable) {
                console.error("Sigil: Failed to load model ${data.url}: ${t.message}")
                group.clear()
                group.add(createModelFallback(data))
            }
        }

        return group
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

    private fun applyModelSettings(root: Object3D, data: ModelData) {
        root.traverse { node ->
            val mesh = node as? Mesh ?: return@traverse
            mesh.castShadow = data.castShadow
            mesh.receiveShadow = data.receiveShadow
            preserveGltfGeometryAttributes(mesh)
            configureMaterialTextureFidelity(mesh.material)
            if (renderer is WebGLRenderer) {
                bakeBaseColorTextureForWebGl(mesh)
            }
            applyMaterialOverrides(mesh, data.materialOverrides)
        }
    }

    private fun bakeSceneTexturesForWebGl() {
        materiaScene.traverse { node ->
            (node as? Mesh)?.let(::bakeBaseColorTextureForWebGl)
        }
    }

    private fun bakeBaseColorTextureForWebGl(mesh: Mesh) {
        if (SigilWebGlTextureBaker.bakeBaseColorTexture(mesh)) {
            when (val material = mesh.material) {
                is MeshStandardMaterial -> {
                    material.vertexColors = true
                    material.needsUpdate = true
                }
                is MeshBasicMaterial -> {
                    material.needsUpdate = true
                }
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
        for (nodePatch in patch.nodes) {
            val node = findPatchTarget(nodePatch) ?: continue
            applyNodePatch(node, nodePatch)
            val nodeId = node.userData["sigilNodeId"] as? String
            val nodeData = nodeId?.let { nodeDataMap[it] }
            if (nodePatch.animations.isNotEmpty()) {
                scheduleAnimations(node, nodePatch.animations, AnimationTrigger.PATCH)
            } else if (nodeData != null) {
                scheduleAnimations(node, nodeData.animations, AnimationTrigger.PATCH)
            }
        }
        materiaScene.updateMatrixWorld(true)
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

    private fun applyNodePatch(node: Object3D, patch: SceneNodePatch) {
        patch.position?.takeIf { it.size >= 3 }?.let { node.position.set(it[0], it[1], it[2]) }
        patch.rotation?.takeIf { it.size >= 3 }?.let { node.rotation.set(it[0], it[1], it[2]) }
        patch.scale?.takeIf { it.size >= 3 }?.let { node.scale.set(it[0], it[1], it[2]) }
        patch.visible?.let { node.visible = it }
        patch.name?.let { node.name = it }
        patch.label?.let { node.userData["sigilLabel"] = it }
        patch.highlight?.let { applyHighlightPatch(node, it) }
    }

    private fun applyHighlightPatch(node: Object3D, patch: HighlightPatch) {
        if (patch.active) {
            setNodeMaterialColor(node, intToColor(patch.color), storeOriginal = true)
            node.userData["sigilHighlightActive"] = true
        } else {
            restoreNodeMaterialColor(node)
            node.userData.remove("sigilHighlightActive")
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

        val gltfJson = try {
            assetResolver.load(modelUrl, null).decodeToString()
        } catch (t: Throwable) {
            console.warn("Sigil: Could not read glTF JSON metadata for $modelUrl: ${t.message}")
            return
        }

        val baseColorTextures = try {
            SigilGltfMetadata.extractBaseColorTextures(gltfJson)
        } catch (t: Throwable) {
            console.warn("Sigil: Could not parse glTF material texture metadata for $modelUrl: ${t.message}")
            return
        }
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

            configureTextureFidelity(texture)
            applyBaseColorTexture(material, texture, textureInfo.baseColorFactor)
        }
    }

    private fun applyBaseColorTexture(
        material: Material,
        texture: Texture,
        baseColorFactor: List<Float>
    ) {
        val color = colorFromBaseColorFactor(baseColorFactor)
        val alpha = baseColorFactor.getOrNull(3) ?: 1f

        when (material) {
            is MeshStandardMaterial -> {
                if (material.map == null) material.map = texture.unsafeCast<Texture2D>()
                material.color = color
                if (alpha < 1f) {
                    material.transparent = true
                    material.opacity = alpha
                }
                material.needsUpdate = true
            }
            is MeshBasicMaterial -> {
                if (material.map == null) material.map = texture
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

    private fun preserveGltfGeometryAttributes(mesh: Mesh) {
        val geometry = mesh.geometry
        if (geometry.hasAttribute("TEXCOORD_0") && !geometry.hasAttribute("uv")) {
            geometry.getAttribute("TEXCOORD_0")?.let { geometry.setAttribute("uv", it) }
        }
        if (geometry.hasAttribute("COLOR_0") && !geometry.hasAttribute("color")) {
            geometry.getAttribute("COLOR_0")?.let { geometry.setAttribute("color", it) }
        }

        val hasVertexColors = geometry.hasAttribute("color") || geometry.hasAttribute("COLOR_0")
        if (hasVertexColors) {
            when (val material = mesh.material) {
                is MeshStandardMaterial -> {
                    material.vertexColors = true
                    material.needsUpdate = true
                }
                is BaseMaterial -> {
                    material.vertexColors = true
                    material.needsUpdate = true
                }
            }
        }
    }

    private fun configureMaterialTextureFidelity(material: Material?) {
        when (material) {
            is MeshStandardMaterial -> {
                configureTextureFidelity(material.map)
                material.needsUpdate = true
            }
            is MeshBasicMaterial -> {
                configureTextureFidelity(material.map)
                material.needsUpdate = true
            }
        }
    }

    private fun configureTextureFidelity(texture: Texture?) {
        texture ?: return
        texture.generateMipmaps = true
        texture.magFilter = TextureFilter.LINEAR
        texture.minFilter = TextureFilter.LINEAR_MIPMAP_LINEAR
        texture.anisotropy = 4f
        texture.markTextureNeedsUpdate()
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
        val hasInteractiveNodes = nodeDataMap.values.any { it.interaction?.enabled == true }
        if (!hasInteractiveNodes) return {}

        canvas.style.setProperty("touch-action", "none")

        val mouseDown: (Event) -> Unit = mouseDown@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseDown
            val hit = pickInteractionHit(mouseEvent) ?: return@mouseDown
            val node = hit.node
            val interaction = interactionForNode(node) ?: return@mouseDown
            dispatchSceneEvent("pointerdown", mouseEvent, node, hit.intersection)

            val drag = interaction.drag
            if (drag?.enabled == true) {
                val constraint = beginDrag(mouseEvent, node, interaction, hit.intersection.point) ?: return@mouseDown
                updateHoverDropTarget(null)
                activeDrag = ActiveDrag(
                    source = node,
                    sourceInteraction = interaction,
                    constraint = constraint
                )
                setCanvasCursor(CursorHint.GRABBING)
                dispatchSceneEvent("dragstart", mouseEvent, node, hit.intersection, activeDrag)
                mouseEvent.preventDefault()
            }
        }

        val mouseMove: (Event) -> Unit = mouseMove@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@mouseMove
            val drag = activeDrag
            if (drag != null) {
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
                mouseEvent.preventDefault()
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
                setCanvasCursor(CursorHint.AUTO)
                mouseEvent.preventDefault()
                return@mouseUp
            }

            val hit = pickInteractive(mouseEvent)
            hit.second?.let { node ->
                dispatchSceneEvent("pointerup", mouseEvent, node, hit.first)
            }
        }

        val click: (Event) -> Unit = click@{ event ->
            val mouseEvent = event as? MouseEvent ?: return@click
            val hit = pickInteractive(mouseEvent)
            hit.second?.let { node ->
                dispatchSceneEvent("click", mouseEvent, node, hit.first)
            }
        }

        canvas.addEventListener("mousedown", mouseDown)
        canvas.addEventListener("mousemove", mouseMove)
        canvas.addEventListener("mouseup", mouseUp)
        canvas.addEventListener("click", click)

        return {
            updateHoverDropTarget(null)
            activeDrag?.target?.let { setDropTargetState(it, null) }
            activeDrag = null
            canvas.removeEventListener("mousedown", mouseDown)
            canvas.removeEventListener("mousemove", mouseMove)
            canvas.removeEventListener("mouseup", mouseUp)
            canvas.removeEventListener("click", click)
            canvas.style.setProperty("cursor", "auto")
        }
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

    private fun pickInteractionHit(
        event: MouseEvent,
        acceptCandidate: (Object3D) -> Boolean = { true }
    ): SigilInteractionHit? {
        val ray = rayForEvent(event) ?: return null
        val hits = hitVolumeHits(ray) + meshRaycasterHits(ray)

        return hits
            .sortedBy { it.intersection.distance }
            .firstOrNull { acceptCandidate(it.node) }
    }

    private fun rayForEvent(event: MouseEvent): Ray? {
        val cam = camera ?: return null
        val pointer = normalizedPointer(event)
        materiaScene.updateMatrixWorld(true)
        return SigilInteractionPicker.rayFromCamera(pointer, cam)
    }

    private fun hitVolumeHits(ray: Ray): List<SigilInteractionHit> {
        val hits = mutableListOf<SigilInteractionHit>()

        nodeMap.values.forEach { node ->
            if (!node.isVisibleInHierarchy()) return@forEach
            val interaction = interactionForNode(node) ?: return@forEach
            if (!interaction.enabled || interaction.hitVolume == null) return@forEach

            SigilInteractionPicker.intersectHitVolume(ray, node, interaction)?.let { hit ->
                hits.add(hit)
            }
        }

        return hits
    }

    private fun meshRaycasterHits(ray: Ray): List<SigilInteractionHit> {
        raycaster.ray.origin.copy(ray.origin)
        raycaster.ray.direction.copy(ray.direction)
        val intersections = raycaster.intersectObject(materiaScene, true)
        val hits = mutableListOf<SigilInteractionHit>()

        for (intersection in intersections) {
            val candidate = findInteractiveNode(intersection.`object`) ?: continue
            if (!candidate.isVisibleInHierarchy()) continue
            hits.add(SigilInteractionHit(intersection, candidate))
        }

        return hits
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
        when (data.kind) {
            AnimationKind.SLIDE -> {
                val vector = data.vector ?: listOf(0f, 0.25f, 0f)
                node.position.set(
                    active.basePosition[0] + vector.getOrElse(0) { 0f } * eased * data.intensity,
                    active.basePosition[1] + vector.getOrElse(1) { 0f } * eased * data.intensity,
                    active.basePosition[2] + vector.getOrElse(2) { 0f } * eased * data.intensity
                )
            }
            AnimationKind.BOB -> {
                val height = (data.vector?.getOrNull(1) ?: 0.2f) * data.intensity
                node.position.y = active.basePosition[1] + sin(progress * PI.toFloat() * 2f) * height
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
            }
            AnimationKind.SHAKE,
            AnimationKind.GLITCH -> {
                val amount = 0.05f * data.intensity * (1f - progress)
                node.position.x = active.basePosition[0] + sin(progress * PI.toFloat() * 18f) * amount
                node.position.y = active.basePosition[1] + sin(progress * PI.toFloat() * 23f) * amount
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
}

/**
 * Register the global SigilHydrator for external access.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
object SigilHydratorGlobal {
    private val hydrators = mutableMapOf<String, SigilHydrator>()

    fun hydrate(canvasId: String, sceneData: dynamic) {
        val jsonString = JSON.stringify(sceneData)
        val scene = SigilScene.fromJson(jsonString)
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
            val hydrator = SigilHydrator(canvas, scene)
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
}
