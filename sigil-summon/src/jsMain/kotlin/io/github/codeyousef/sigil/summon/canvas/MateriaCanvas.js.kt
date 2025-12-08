package io.github.codeyousef.sigil.summon.canvas

import code.yousef.summon.annotation.Composable
import io.github.codeyousef.sigil.schema.SigilScene
import io.github.codeyousef.sigil.schema.SigilNodeData
import io.github.codeyousef.sigil.schema.MeshData
import io.github.codeyousef.sigil.schema.GroupData
import io.github.codeyousef.sigil.schema.LightData
import io.github.codeyousef.sigil.schema.CameraData
import io.github.codeyousef.sigil.schema.GeometryType
import io.github.codeyousef.sigil.schema.GeometryParams
import io.github.codeyousef.sigil.schema.LightType
import io.github.codeyousef.sigil.schema.CameraType
import io.github.codeyousef.sigil.schema.SigilJson
import io.github.codeyousef.sigil.schema.SceneSettings
import io.github.codeyousef.sigil.summon.context.SigilSummonContext
import io.materia.engine.scene.Scene
import io.materia.engine.scene.EngineMesh
import io.materia.engine.scene.Group
import io.materia.engine.material.BasicMaterial
import io.materia.engine.material.StandardMaterial
import io.materia.engine.camera.PerspectiveCamera
import io.materia.engine.camera.OrthographicCamera
import io.materia.engine.renderer.WebGPURenderer
import io.materia.engine.renderer.WebGPURendererConfig
import io.materia.engine.core.RenderLoop
import io.materia.engine.core.DisposableContainer
import io.materia.core.Object3D
import io.materia.core.math.Color
import io.materia.core.math.Vector3
import io.materia.geometry.BoxGeometry
import io.materia.geometry.SphereGeometry
import io.materia.geometry.PlaneGeometry
import io.materia.geometry.CylinderGeometry
import io.materia.geometry.ConeGeometry
import io.materia.geometry.TorusGeometry
import io.materia.geometry.CircleGeometry
import io.materia.geometry.RingGeometry
import io.materia.geometry.IcosahedronGeometry
import io.materia.geometry.OctahedronGeometry
import io.materia.geometry.TetrahedronGeometry
import io.materia.geometry.DodecahedronGeometry
import io.materia.geometry.BufferGeometry
import io.materia.light.AmbientLight
import io.materia.light.DirectionalLight
import io.materia.light.PointLight
import io.materia.light.SpotLight
import io.materia.light.HemisphereLight
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLScriptElement

/**
 * JS (Client-side) implementation of MateriaCanvas for Summon.
 *
 * This implementation handles hydration:
 * 1. Locates the pre-rendered container and scene data
 * 2. Parses the serialized scene JSON
 * 3. Creates actual Materia objects from the schema data
 * 4. Initializes the WebGPU renderer
 * 5. Starts the render loop
 */
@Composable
actual fun MateriaCanvas(
    id: String,
    width: String,
    height: String,
    backgroundColor: Int,
    content: @Composable () -> String
): String {
    // On the client, we might be called during initial render
    // In that case, we also execute content to collect nodes
    val context = SigilSummonContext.createClientContext()

    SigilSummonContext.withContext(context) {
        content()
    }

    // Schedule hydration for after render
    scheduleHydration(id, context.buildScene())

    // Return container HTML (same as server for consistency)
    return """<div id="$id" style="width: $width; height: $height;"></div>"""
}

/**
 * Schedule hydration to run after the current execution context.
 */
private fun scheduleHydration(canvasId: String, fallbackScene: SigilScene) {
    window.setTimeout({
        performHydration(canvasId, fallbackScene)
    }, 0)
}

/**
 * Perform the actual hydration process.
 */
private fun performHydration(canvasId: String, fallbackScene: SigilScene) {
    // Try to get pre-rendered scene data
    val dataElement = document.getElementById("$canvasId-data") as? HTMLScriptElement
    val scene = if (dataElement != null) {
        val jsonData = dataElement.textContent ?: "{}"
        SigilScene.fromJson(jsonData)
    } else {
        // Use fallback scene from client-side composition
        fallbackScene
    }

    // Get or create the container element
    val container = document.getElementById(canvasId) as? HTMLDivElement
        ?: run {
            console.error("Sigil: Container element '$canvasId' not found")
            return
        }

    // Create canvas element
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.style.width = "100%"
    canvas.style.height = "100%"

    // Clear container and add canvas
    container.innerHTML = ""
    container.appendChild(canvas)

    // Set canvas size
    val rect = container.getBoundingClientRect()
    canvas.width = rect.width.toInt()
    canvas.height = rect.height.toInt()

    // Initialize Materia
    try {
        val hydrator = SigilHydrator(canvas, scene)
        hydrator.initialize()
        hydrator.startRenderLoop()

        // Store reference for cleanup
        js("window")["sigilHydrators"] = js("window.sigilHydrators || {}")
        js("window.sigilHydrators")["canvasId"] = hydrator

    } catch (e: Exception) {
        console.error("Sigil: Failed to hydrate scene: ${e.message}")
    }
}

/**
 * Hydrator class that creates Materia objects from schema data.
 */
class SigilHydrator(
    private val canvas: HTMLCanvasElement,
    private val sceneData: SigilScene
) {
    private val materiaScene = Scene()
    private var renderer: WebGPURenderer? = null
    private var renderLoop: RenderLoop? = null
    private var camera: PerspectiveCamera? = null
    private val disposables = DisposableContainer()
    private val nodeMap = mutableMapOf<String, Object3D>()

    fun initialize() {
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

        // Create renderer
        val config = WebGPURendererConfig(
            depthTest = true,
            clearColor = intToColor(sceneData.settings.backgroundColor),
            antialias = 4
        )

        renderer = WebGPURenderer(config).also { r ->
            val surface = CanvasRenderSurface(canvas)
            r.initialize(surface)
            r.setSize(canvas.width, canvas.height)
            disposables.track(r)
        }

        // Handle resize
        window.addEventListener("resize") {
            val rect = canvas.parentElement?.getBoundingClientRect()
            if (rect != null) {
                canvas.width = rect.width.toInt()
                canvas.height = rect.height.toInt()
                renderer?.setSize(canvas.width, canvas.height)
                camera?.let { c ->
                    c.aspect = canvas.width.toFloat() / canvas.height.toFloat()
                    c.updateProjectionMatrix()
                }
            }
        }
    }

    fun startRenderLoop() {
        val cam = camera ?: return
        val r = renderer ?: return

        renderLoop = RenderLoop { deltaTime ->
            materiaScene.traverse { obj ->
                obj.updateMatrixWorld()
            }
            r.render(materiaScene, cam)
        }
        renderLoop?.start()
    }

    fun stop() {
        renderLoop?.stop()
        renderLoop = null
    }

    fun dispose() {
        stop()
        nodeMap.clear()
        disposables.dispose()
    }

    private fun applySceneSettings(settings: SceneSettings) {
        materiaScene.background = intToColor(settings.backgroundColor)

        if (settings.fogEnabled) {
            materiaScene.fog = io.materia.engine.scene.Fog(
                intToColor(settings.fogColor),
                settings.fogNear,
                settings.fogFar
            )
        }
    }

    private fun createMateriaNode(nodeData: SigilNodeData): Object3D? {
        return when (nodeData) {
            is MeshData -> createMesh(nodeData)
            is GroupData -> createGroup(nodeData)
            is LightData -> createLight(nodeData)
            is CameraData -> createCamera(nodeData)
        }
    }

    private fun createMesh(data: MeshData): EngineMesh {
        val geometry = createGeometry(data.geometryType, data.geometryParams)
        val material = if (data.metalness > 0f || data.roughness < 1f) {
            StandardMaterial(
                color = intToColor(data.materialColor),
                metalness = data.metalness,
                roughness = data.roughness
            )
        } else {
            BasicMaterial(color = intToColor(data.materialColor))
        }

        disposables.track(geometry)
        disposables.track(material)

        return EngineMesh(geometry, material).apply {
            position.set(data.position[0], data.position[1], data.position[2])
            rotation.set(data.rotation[0], data.rotation[1], data.rotation[2])
            scale.set(data.scale[0], data.scale[1], data.scale[2])
            visible = data.visible
            castShadow = data.castShadow
            receiveShadow = data.receiveShadow
            name = data.name ?: ""
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

    private fun createLight(data: LightData): io.materia.light.Light {
        val light: io.materia.light.Light = when (data.lightType) {
            LightType.AMBIENT -> AmbientLight(
                intToColor(data.color),
                data.intensity
            )
            LightType.DIRECTIONAL -> DirectionalLight(
                intToColor(data.color),
                data.intensity
            ).apply {
                target.position.set(data.target[0], data.target[1], data.target[2])
                castShadow = data.castShadow
            }
            LightType.POINT -> PointLight(
                intToColor(data.color),
                data.intensity,
                data.distance,
                data.decay
            ).apply {
                castShadow = data.castShadow
            }
            LightType.SPOT -> SpotLight(
                intToColor(data.color),
                data.intensity,
                data.distance,
                data.angle,
                data.penumbra,
                data.decay
            ).apply {
                target.position.set(data.target[0], data.target[1], data.target[2])
                castShadow = data.castShadow
            }
            LightType.HEMISPHERE -> HemisphereLight(
                intToColor(data.color),
                Color(0.2f, 0.2f, 0.2f), // Default ground color
                data.intensity
            )
        }

        light.position.set(data.position[0], data.position[1], data.position[2])
        light.visible = data.visible
        light.name = data.name ?: ""

        return light
    }

    private fun createCamera(data: CameraData): io.materia.engine.camera.Camera {
        return when (data.cameraType) {
            CameraType.PERSPECTIVE -> PerspectiveCamera(
                fov = data.fov,
                aspect = data.aspect,
                near = data.near,
                far = data.far
            ).apply {
                position.set(data.position[0], data.position[1], data.position[2])
                data.lookAt?.let { lookAt(Vector3(it[0], it[1], it[2])) }
                name = data.name ?: ""
            }
            CameraType.ORTHOGRAPHIC -> OrthographicCamera(
                left = data.orthoBounds[0],
                right = data.orthoBounds[1],
                top = data.orthoBounds[2],
                bottom = data.orthoBounds[3],
                near = data.near,
                far = data.far
            ).apply {
                position.set(data.position[0], data.position[1], data.position[2])
                data.lookAt?.let { lookAt(Vector3(it[0], it[1], it[2])) }
                name = data.name ?: ""
            }
        }
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
 * External declaration for canvas render surface.
 */
external class CanvasRenderSurface(canvas: HTMLCanvasElement)

/**
 * Register the global SigilHydrator for external access.
 */
@JsExport
object SigilHydratorGlobal {
    fun hydrate(canvasId: String, sceneData: dynamic) {
        val jsonString = JSON.stringify(sceneData)
        val scene = SigilScene.fromJson(jsonString)
        val container = document.getElementById(canvasId) as? HTMLDivElement ?: return

        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.style.width = "100%"
        canvas.style.height = "100%"
        container.innerHTML = ""
        container.appendChild(canvas)

        val rect = container.getBoundingClientRect()
        canvas.width = rect.width.toInt()
        canvas.height = rect.height.toInt()

        val hydrator = SigilHydrator(canvas, scene)
        hydrator.initialize()
        hydrator.startRenderLoop()
    }
}

// Make hydrator globally accessible
private fun initGlobalHydrator() {
    js("window.SigilHydrator = SigilHydratorGlobal")
}
