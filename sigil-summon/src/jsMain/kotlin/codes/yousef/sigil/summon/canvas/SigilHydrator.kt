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
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.GeometryParams
import codes.yousef.sigil.schema.LightType
import codes.yousef.sigil.schema.SceneSettings
import io.materia.core.scene.Scene
import io.materia.core.scene.Object3D
import io.materia.core.scene.Mesh
import io.materia.core.scene.Group
import io.materia.core.scene.Background
import io.materia.core.math.Color
import io.materia.core.math.Vector3
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
import io.materia.loader.GLTFLoader
import io.materia.lighting.AmbientLightImpl
import io.materia.lighting.DirectionalLightImpl
import io.materia.lighting.PointLightImpl
import io.materia.lighting.SpotLightImpl
import io.materia.lighting.HemisphereLightImpl
import io.materia.lighting.Light
import io.materia.lighting.DefaultLightingSystem
import io.materia.renderer.Renderer
import io.materia.renderer.webgpu.WebGPURenderer
import io.materia.renderer.webgl.WebGLRenderer
import io.materia.renderer.RendererConfig
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

private val scope = MainScope()

/**
 * Hydrator class that creates Materia objects from schema data.
 */
class SigilHydrator(
    private val canvas: HTMLCanvasElement,
    private val sceneData: SigilScene
) {
    private val materiaScene = Scene()
    private val lightingSystem = DefaultLightingSystem()
    private var renderer: Renderer? = null
    private var animationFrameId: Int = 0
    private var running = false
    private var camera: PerspectiveCamera? = null
    private val nodeMap = mutableMapOf<String, Object3D>()
    private val lights = mutableListOf<Light>()
    private val gltfLoader = GLTFLoader()
    private var orbitControls: OrbitControls? = null
    private var firstPersonControls: FirstPersonControls? = null
    private var controlsCleanup: (() -> Unit)? = null
    private var lastFrameTimeMs: Double = 0.0

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
                nodeMap[nodeData.id] = materiaNode
            }
        }

        // Create renderer — try WebGPU first, fall back to WebGL
        val config = RendererConfig()
        var initialized = false

        // Try WebGPU first
        try {
            val gpu = js("navigator.gpu")
            if (gpu != null && gpu != undefined) {
                val r = WebGPURenderer(canvas)
                val result = r.initialize(config)
                when (result) {
                    is io.materia.core.Result.Success -> {
                        renderer = r
                        r.resize(canvas.width, canvas.height)
                        r.clearColor = intToColor(sceneData.settings.backgroundColor)
                        initialized = true
                        console.log("Sigil: Initialized WebGPU renderer")
                    }
                    is io.materia.core.Result.Error -> {
                        console.warn("Sigil: WebGPU initialization failed (${result.message}), trying WebGL fallback...")
                        r.dispose()
                    }
                }
            } else {
                console.log("Sigil: WebGPU not available, trying WebGL fallback...")
            }
        } catch (e: Throwable) {
            console.warn("Sigil: WebGPU error (${e.message}), trying WebGL fallback...")
        }

        // Fallback to WebGL
        if (!initialized) {
            try {
                val r = WebGLRenderer(canvas)
                val result = r.initialize(config)
                when (result) {
                    is io.materia.core.Result.Success -> {
                        renderer = r
                        r.resize(canvas.width, canvas.height)
                        initialized = true
                        console.log("Sigil: Initialized WebGL renderer (fallback)")
                    }
                    is io.materia.core.Result.Error -> {
                        console.error("Sigil: WebGL initialization also failed: ${result.message}")
                    }
                }
            } catch (e: Throwable) {
                console.error("Sigil: WebGL fallback error: ${e.message}")
            }
        }

        if (!initialized) {
            console.error("Sigil: No renderer could be initialized (tried WebGPU and WebGL)")
            return
        }

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

    fun startRenderLoop() {
        val cam = camera ?: return
        val r = renderer ?: return
        
        running = true
        lastFrameTimeMs = window.performance.now()
        
        fun renderFrame() {
            if (!running) return

            val now = window.performance.now()
            val deltaSeconds = ((now - lastFrameTimeMs) / 1000.0).toFloat()
            lastFrameTimeMs = now

            orbitControls?.update(deltaSeconds)
            firstPersonControls?.update(deltaSeconds)
            
            materiaScene.updateMatrixWorld(true)
            cam.updateMatrixWorld()
            cam.updateProjectionMatrix()
            r.render(materiaScene, cam)

            animationFrameId = window.requestAnimationFrame { renderFrame() }
        }
        
        renderFrame()
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
        controlsCleanup = null
        orbitControls = null
        firstPersonControls = null
        nodeMap.clear()
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
                val asset = gltfLoader.load(data.url)
                val root = asset.scene
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
                nodeMap[childData.id] = childNode
            }
        }

        return group
    }

    private fun applyModelSettings(root: Object3D, data: ModelData) {
        root.traverse { node ->
            val mesh = node as? Mesh ?: return@traverse
            mesh.castShadow = data.castShadow
            mesh.receiveShadow = data.receiveShadow
            applyMaterialOverrides(mesh, data.materialOverrides)
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
@JsExport
object SigilHydratorGlobal {
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
            hydrator.initialize()
            hydrator.startRenderLoop()
        }
    }
}
