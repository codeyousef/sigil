package io.github.codeyousef.sigil.summon.canvas

import io.github.codeyousef.sigil.schema.SigilScene
import io.github.codeyousef.sigil.schema.SigilNodeData
import io.github.codeyousef.sigil.schema.MeshData
import io.github.codeyousef.sigil.schema.GroupData
import io.github.codeyousef.sigil.schema.LightData
import io.github.codeyousef.sigil.schema.CameraData
import io.github.codeyousef.sigil.schema.GeometryType
import io.github.codeyousef.sigil.schema.GeometryParams
import io.github.codeyousef.sigil.schema.LightType
import io.github.codeyousef.sigil.schema.SceneSettings
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
import io.materia.lighting.AmbientLightImpl
import io.materia.lighting.DirectionalLightImpl
import io.materia.lighting.PointLightImpl
import io.materia.lighting.SpotLightImpl
import io.materia.lighting.HemisphereLightImpl
import io.materia.lighting.Light
import io.materia.lighting.DefaultLightingSystem
import io.materia.renderer.webgpu.WebGPURenderer
import io.materia.renderer.RendererConfig
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement

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
    private var renderer: WebGPURenderer? = null
    private var animationFrameId: Int = 0
    private var running = false
    private var camera: PerspectiveCamera? = null
    private val nodeMap = mutableMapOf<String, Object3D>()

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

        // Create renderer
        val r = WebGPURenderer(canvas)
        val config = RendererConfig()
        val result = r.initialize(config)
        
        when (result) {
            is io.materia.core.Result.Success -> {
                renderer = r
                r.resize(canvas.width, canvas.height)
            }
            is io.materia.core.Result.Error -> {
                console.error("Sigil: Failed to initialize WebGPU renderer: ${result.message}")
                return
            }
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
        
        fun renderFrame() {
            if (!running) return
            
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
            is GroupData -> createGroup(nodeData)
            is LightData -> {
                // Lights in Materia 0.2.0.0 don't extend Object3D
                // They're managed via LightingSystem
                createLight(nodeData)
                null
            }
            is CameraData -> {
                // Cameras are handled separately
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
        val container = document.getElementById(canvasId) as? HTMLDivElement ?: return

        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.style.width = "100%"
        canvas.style.height = "100%"
        container.innerHTML = ""
        container.appendChild(canvas)

        val rect = container.getBoundingClientRect()
        canvas.width = rect.width.toInt()
        canvas.height = rect.height.toInt()

        scope.launch {
            val hydrator = SigilHydrator(canvas, scene)
            hydrator.initialize()
            hydrator.startRenderLoop()
        }
    }
}
