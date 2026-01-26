package codes.yousef.sigil.sample.summon

import kotlinx.browser.document
import kotlinx.browser.window
import codes.yousef.sigil.schema.SigilScene
import codes.yousef.sigil.schema.SigilNodeData
import codes.yousef.sigil.schema.MeshData
import codes.yousef.sigil.schema.ModelData
import codes.yousef.sigil.schema.GroupData
import codes.yousef.sigil.schema.LightData
import codes.yousef.sigil.schema.CameraData
import codes.yousef.sigil.schema.ControlsData
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.LightType
import org.w3c.dom.HTMLCanvasElement
import io.materia.core.scene.Scene
import io.materia.core.scene.Background
import io.materia.core.scene.Mesh
import io.materia.core.scene.Group
import io.materia.renderer.webgpu.WebGPURenderer
import io.materia.camera.PerspectiveCamera
import io.materia.geometry.BufferGeometry
import io.materia.geometry.primitives.BoxGeometry
import io.materia.geometry.primitives.SphereGeometry
import io.materia.geometry.primitives.PlaneGeometry
import io.materia.geometry.primitives.CylinderGeometry
import io.materia.material.MeshStandardMaterial
import io.materia.material.MeshBasicMaterial
import io.materia.lighting.AmbientLightImpl
import io.materia.lighting.DirectionalLightImpl
import io.materia.lighting.PointLightImpl
import io.materia.lighting.DefaultLightingSystem
import io.materia.core.math.Vector3
import io.materia.core.math.Color
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.PI

/**
 * Client-side entry point for hydrating SSR-rendered Sigil scenes.
 */
fun main() {
    window.onload = {
        hydrateScenes()
    }
}

/**
 * Finds all canvas elements with sigil-scene data and hydrates them.
 */
private fun hydrateScenes() {
    val canvases = document.querySelectorAll("canvas[data-sigil-scene]")

    for (i in 0 until canvases.length) {
        val canvas = canvases.item(i) as? HTMLCanvasElement ?: continue
        val sceneData = canvas.getAttribute("data-sigil-scene") ?: continue

        try {
            val sigilScene = SigilScene.fromJson(sceneData)
            hydrateCanvas(canvas, sigilScene)
            console.log("Sigil scene hydrated successfully: ${canvas.id}")
        } catch (e: Exception) {
            console.error("Failed to hydrate sigil scene: ${e.message}")
        }
    }
}

/**
 * Hydrates a single canvas with a SigilScene.
 */
private fun hydrateCanvas(canvas: HTMLCanvasElement, sigilScene: SigilScene) {
    // Create Materia scene
    val scene = Scene()
    val lightingSystem = DefaultLightingSystem()

    // Set background color from scene settings
    scene.background = Background.Color(Color(sigilScene.settings.backgroundColor))

    // Create default camera
    val camera = PerspectiveCamera(
        fov = 75f,
        aspect = canvas.width.toFloat() / canvas.height.toFloat(),
        near = 0.1f,
        far = 1000f
    )
    camera.position.set(0f, 0f, 5f)
    camera.lookAt(Vector3(0f, 0f, 0f))

    // Add nodes to scene
    sigilScene.rootNodes.forEach { nodeData ->
        processNode(nodeData, scene, lightingSystem, camera)
    }

    // Create and configure renderer (async initialization)
    GlobalScope.launch {
        val renderer = WebGPURenderer(canvas)
        renderer.setSize(canvas.width, canvas.height, false)

        // Animation loop
        fun animate() {
            window.requestAnimationFrame { animate() }
            renderer.render(scene, camera)
        }

        // Start rendering
        animate()
    }
}

/**
 * Process a scene node, adding it to the scene or updating camera.
 */
private fun processNode(
    nodeData: SigilNodeData,
    scene: Scene,
    lightingSystem: DefaultLightingSystem,
    camera: PerspectiveCamera
) {
    when (nodeData) {
        is MeshData -> {
            val mesh = createMesh(nodeData)
            scene.add(mesh)
        }
        is LightData -> {
            addLight(nodeData, lightingSystem)
        }
        is ModelData -> {
            // Model loading not supported in this sample
        }
        is GroupData -> {
            val group = createGroup(nodeData, lightingSystem, camera)
            scene.add(group)
        }
        is CameraData -> {
            // Update camera from camera data
            camera.fov = nodeData.fov
            camera.near = nodeData.near
            camera.far = nodeData.far
            camera.position.set(
                nodeData.position[0],
                nodeData.position[1],
                nodeData.position[2]
            )
            nodeData.lookAt?.let { target ->
                camera.lookAt(Vector3(target[0], target[1], target[2]))
            }
        }
        is ControlsData -> {
            // Controls are not handled in this sample
        }
    }
}

/**
 * Creates a Materia mesh from MeshData.
 */
private fun createMesh(meshData: MeshData): Mesh {
    val geometry = createGeometry(meshData.geometryType, meshData.geometryParams)
    val material = createMaterial(meshData.materialColor, meshData.metalness, meshData.roughness)
    val mesh = Mesh(geometry, material)

    mesh.position.set(
        meshData.position[0],
        meshData.position[1],
        meshData.position[2]
    )
    mesh.rotation.set(
        meshData.rotation[0],
        meshData.rotation[1],
        meshData.rotation[2]
    )
    mesh.scale.set(
        meshData.scale[0],
        meshData.scale[1],
        meshData.scale[2]
    )
    mesh.visible = meshData.visible

    return mesh
}

/**
 * Creates a BufferGeometry from GeometryType and params.
 */
private fun createGeometry(
    type: GeometryType,
    params: codes.yousef.sigil.schema.GeometryParams
): BufferGeometry {
    return when (type) {
        GeometryType.BOX -> BoxGeometry(
            width = params.width,
            height = params.height,
            depth = params.depth
        )
        GeometryType.SPHERE -> SphereGeometry(
            radius = params.radius,
            widthSegments = params.widthSegments.coerceAtLeast(8),
            heightSegments = params.heightSegments.coerceAtLeast(6)
        )
        GeometryType.PLANE -> PlaneGeometry(
            width = params.width,
            height = params.height
        )
        GeometryType.CYLINDER -> CylinderGeometry(
            radiusTop = params.radiusTop,
            radiusBottom = params.radiusBottom,
            height = params.height,
            radialSegments = params.radialSegments
        )
        else -> {
            // For other geometry types, create a basic box as fallback
            BoxGeometry(1f, 1f, 1f)
        }
    }
}

/**
 * Creates a Material from color and PBR properties.
 */
private fun createMaterial(
    color: Int,
    metalness: Float,
    roughness: Float
): io.materia.core.scene.Material {
    return if (metalness > 0f || roughness < 1f) {
        MeshStandardMaterial(
            color = Color(color),
            metalness = metalness,
            roughness = roughness
        )
    } else {
        MeshBasicMaterial().apply {
            this.color = Color(color)
        }
    }
}

/**
 * Adds a light to the LightingSystem from LightData.
 */
private fun addLight(lightData: LightData, lightingSystem: DefaultLightingSystem) {
    val position = lightData.position
    val lightColor = Color(lightData.color)
    
    when (lightData.lightType) {
        LightType.DIRECTIONAL -> {
            val direction = Vector3(-position[0], -position[1], -position[2])
            lightingSystem.addLight(DirectionalLightImpl(
                color = lightColor,
                intensity = lightData.intensity,
                direction = direction
            ))
        }
        LightType.AMBIENT -> {
            lightingSystem.addLight(AmbientLightImpl(
                color = lightColor,
                intensity = lightData.intensity
            ))
        }
        LightType.POINT -> {
            lightingSystem.addLight(PointLightImpl(
                color = lightColor,
                intensity = lightData.intensity,
                position = Vector3(position[0], position[1], position[2]),
                distance = lightData.distance.takeIf { it > 0 } ?: 100f,
                decay = lightData.decay
            ))
        }
        else -> {
            // Fallback to ambient light for unsupported types
            lightingSystem.addLight(AmbientLightImpl(
                color = lightColor,
                intensity = lightData.intensity
            ))
        }
    }
}

/**
 * Creates a group with children from GroupData.
 */
private fun createGroup(
    groupData: GroupData,
    lightingSystem: DefaultLightingSystem,
    camera: PerspectiveCamera
): Group {
    val group = Group()

    group.position.set(
        groupData.position[0],
        groupData.position[1],
        groupData.position[2]
    )
    group.rotation.set(
        groupData.rotation[0],
        groupData.rotation[1],
        groupData.rotation[2]
    )
    group.scale.set(
        groupData.scale[0],
        groupData.scale[1],
        groupData.scale[2]
    )
    group.visible = groupData.visible

    // Add children
    groupData.children.forEach { childData ->
        when (childData) {
            is MeshData -> group.add(createMesh(childData))
            is LightData -> addLight(childData, lightingSystem)
            is ModelData -> { /* Model loading not supported in this sample */ }
            is GroupData -> group.add(createGroup(childData, lightingSystem, camera))
            is CameraData -> { /* Cameras are handled at scene level */ }
            is ControlsData -> { /* Controls are handled at scene level */ }
        }
    }

    return group
}

// External console object for JS
external object console {
    fun log(message: String)
    fun error(message: String)
}
