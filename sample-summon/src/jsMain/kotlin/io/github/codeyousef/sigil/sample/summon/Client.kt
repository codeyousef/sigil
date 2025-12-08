package io.github.codeyousef.sigil.sample.summon

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import io.github.codeyousef.sigil.schema.GeometrySpec
import io.github.codeyousef.sigil.schema.LightData
import io.github.codeyousef.sigil.schema.MaterialSpec
import io.github.codeyousef.sigil.schema.MeshData
import io.github.codeyousef.sigil.schema.SigilScene
import org.w3c.dom.HTMLCanvasElement
import materia.engine.Scene
import materia.engine.WebGPURenderer
import materia.engine.PerspectiveCamera
import materia.engine.EngineMesh
import materia.engine.Group
import materia.engine.BufferGeometry
import materia.engine.BoxGeometry
import materia.engine.SphereGeometry
import materia.engine.PlaneGeometry
import materia.engine.CylinderGeometry
import materia.engine.StandardMaterial
import materia.engine.BasicMaterial
import materia.engine.DirectionalLight
import materia.engine.AmbientLight
import materia.engine.PointLight
import materia.engine.Vector3
import materia.engine.Color
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
            val sigilScene = Json.decodeFromString(SigilScene.serializer(), sceneData)
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

    // Set background color
    sigilScene.environment?.backgroundColor?.let { bgColor ->
        scene.background = Color(bgColor)
    }

    // Create camera
    val cameraConfig = sigilScene.camera
    val camera = PerspectiveCamera(
        fov = cameraConfig.fov.toDouble(),
        aspect = canvas.width.toDouble() / canvas.height.toDouble(),
        near = cameraConfig.near.toDouble(),
        far = cameraConfig.far.toDouble()
    )
    camera.position.set(
        cameraConfig.position[0].toDouble(),
        cameraConfig.position[1].toDouble(),
        cameraConfig.position[2].toDouble()
    )
    camera.lookAt(Vector3(
        cameraConfig.target[0].toDouble(),
        cameraConfig.target[1].toDouble(),
        cameraConfig.target[2].toDouble()
    ))

    // Add nodes to scene
    sigilScene.nodes.forEach { nodeData ->
        when (nodeData) {
            is MeshData -> {
                val mesh = createMesh(nodeData)
                scene.add(mesh)
            }
            is LightData -> {
                val light = createLight(nodeData)
                scene.add(light)
            }
            is io.github.codeyousef.sigil.schema.GroupData -> {
                val group = createGroup(nodeData)
                scene.add(group)
            }
            is io.github.codeyousef.sigil.schema.CameraData -> {
                // Camera already configured above
            }
        }
    }

    // Create and configure renderer
    val renderer = WebGPURenderer(js("{ canvas: canvas }"))
    renderer.setSize(canvas.width, canvas.height)
    renderer.setPixelRatio(window.devicePixelRatio)

    // Animation loop
    fun animate() {
        window.requestAnimationFrame { animate() }
        renderer.render(scene, camera)
    }

    // Start rendering
    animate()
}

/**
 * Creates a Materia mesh from MeshData.
 */
private fun createMesh(meshData: MeshData): EngineMesh {
    val geometry = createGeometry(meshData.geometry)
    val material = createMaterial(meshData.material)
    val mesh = EngineMesh(geometry, material)

    meshData.transform?.let { transform ->
        mesh.position.set(
            transform.position[0].toDouble(),
            transform.position[1].toDouble(),
            transform.position[2].toDouble()
        )
        mesh.rotation.set(
            transform.rotation[0].toDouble() * PI / 180.0,
            transform.rotation[1].toDouble() * PI / 180.0,
            transform.rotation[2].toDouble() * PI / 180.0
        )
        mesh.scale.set(
            transform.scale[0].toDouble(),
            transform.scale[1].toDouble(),
            transform.scale[2].toDouble()
        )
    }

    return mesh
}

/**
 * Creates a BufferGeometry from GeometrySpec.
 */
private fun createGeometry(spec: GeometrySpec): BufferGeometry {
    return when (spec) {
        is GeometrySpec.Box -> BoxGeometry(
            width = spec.width.toDouble(),
            height = spec.height.toDouble(),
            depth = spec.depth.toDouble()
        )
        is GeometrySpec.Sphere -> SphereGeometry(
            radius = spec.radius.toDouble(),
            widthSegments = spec.widthSegments,
            heightSegments = spec.heightSegments
        )
        is GeometrySpec.Plane -> PlaneGeometry(
            width = spec.width.toDouble(),
            height = spec.height.toDouble()
        )
        is GeometrySpec.Cylinder -> CylinderGeometry(
            radiusTop = spec.radiusTop.toDouble(),
            radiusBottom = spec.radiusBottom.toDouble(),
            height = spec.height.toDouble(),
            radialSegments = spec.radialSegments
        )
        is GeometrySpec.Custom -> {
            // For custom geometry, create a basic BufferGeometry
            // Real implementation would parse indices, positions, normals, uvs
            BufferGeometry()
        }
    }
}

/**
 * Creates a Material from MaterialSpec.
 */
private fun createMaterial(spec: MaterialSpec): dynamic {
    return when (spec) {
        is MaterialSpec.Standard -> StandardMaterial(js("{" +
            "color: ${spec.color}," +
            "metalness: ${spec.metalness}," +
            "roughness: ${spec.roughness}" +
        "}"))
        is MaterialSpec.Basic -> BasicMaterial(js("{" +
            "color: ${spec.color}," +
            "wireframe: ${spec.wireframe}" +
        "}"))
        is MaterialSpec.Custom -> {
            // For custom materials, use BasicMaterial as fallback
            BasicMaterial(js("{ color: 0xffffff }"))
        }
    }
}

/**
 * Creates a light from LightData.
 */
private fun createLight(lightData: LightData): dynamic {
    val light = when (lightData.type) {
        "directional" -> DirectionalLight(lightData.color, lightData.intensity.toDouble())
        "ambient" -> AmbientLight(lightData.color, lightData.intensity.toDouble())
        "point" -> PointLight(lightData.color, lightData.intensity.toDouble())
        else -> AmbientLight(lightData.color, lightData.intensity.toDouble())
    }

    lightData.transform?.let { transform ->
        light.position.set(
            transform.position[0].toDouble(),
            transform.position[1].toDouble(),
            transform.position[2].toDouble()
        )
    }

    return light
}

/**
 * Creates a group with children from GroupData.
 */
private fun createGroup(groupData: io.github.codeyousef.sigil.schema.GroupData): Group {
    val group = Group()

    groupData.transform?.let { transform ->
        group.position.set(
            transform.position[0].toDouble(),
            transform.position[1].toDouble(),
            transform.position[2].toDouble()
        )
        group.rotation.set(
            transform.rotation[0].toDouble() * PI / 180.0,
            transform.rotation[1].toDouble() * PI / 180.0,
            transform.rotation[2].toDouble() * PI / 180.0
        )
        group.scale.set(
            transform.scale[0].toDouble(),
            transform.scale[1].toDouble(),
            transform.scale[2].toDouble()
        )
    }

    // Add children
    groupData.children.forEach { childData ->
        when (childData) {
            is MeshData -> group.add(createMesh(childData))
            is LightData -> group.add(createLight(childData))
            is io.github.codeyousef.sigil.schema.GroupData -> group.add(createGroup(childData))
            is io.github.codeyousef.sigil.schema.CameraData -> { /* Cameras not added to groups */ }
        }
    }

    return group
}

// External console object for JS
external object console {
    fun log(message: String)
    fun error(message: String)
}
