package codes.yousef.sigil.compose.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import codes.yousef.sigil.compose.canvas.MateriaCanvasState
import codes.yousef.sigil.compose.context.LocalMateriaCanvasState
import codes.yousef.sigil.schema.ModelMaterialOverride
import io.materia.controls.ControlsConfig
import io.materia.controls.OrbitControls
import io.materia.core.math.Color
import io.materia.core.math.Vector3
import io.materia.core.scene.Group
import io.materia.core.scene.Mesh
import io.materia.core.scene.Object3D
import io.materia.geometry.primitives.BoxGeometry
import io.materia.loader.GLTFLoader
import io.materia.material.MeshBasicMaterial
import io.materia.material.MeshStandardMaterial
import kotlin.math.PI

/**
 * Load and render a glTF/GLB model.
 */
@Composable
fun Model(
    url: String,
    position: Vector3 = Vector3.ZERO,
    rotation: Vector3 = Vector3.ZERO,
    scale: Vector3 = Vector3.ONE,
    visible: Boolean = true,
    castShadow: Boolean = true,
    receiveShadow: Boolean = true,
    name: String = "",
    materialOverrides: List<ModelMaterialOverride> = emptyList()
) {
    val group = remember { Group() }
    val loader = remember { GLTFLoader() }
    val currentUrl by rememberUpdatedState(url)

    var loadedNode by remember { mutableStateOf<Object3D?>(null) }

    LaunchedEffect(currentUrl) {
        loadedNode = null
        loadedNode = try {
            loader.load(currentUrl).scene
        } catch (_: Throwable) {
            createFallbackModel()
        }
    }

    LaunchedEffect(loadedNode, castShadow, receiveShadow, materialOverrides) {
        loadedNode?.let { node ->
            applyModelSettings(node, castShadow, receiveShadow, materialOverrides)
        }
    }

    DisposableEffect(loadedNode) {
        loadedNode?.let { node ->
            group.add(node)
        }

        onDispose {
            loadedNode?.let { node ->
                group.remove(node)
            }
        }
    }

    MateriaNode(
        factory = { group },
        update = { modelGroup ->
            modelGroup.position.copy(position)
            modelGroup.rotation.set(rotation.x, rotation.y, rotation.z)
            modelGroup.scale.copy(scale)
            modelGroup.visible = visible
            modelGroup.name = name
        }
    )
}

/**
 * Orbit-style camera controls bound to the active MateriaCanvas.
 */
@Composable
fun OrbitControls(
    target: Vector3 = Vector3.ZERO,
    enableDamping: Boolean = true,
    dampingFactor: Float = 0.05f,
    minDistance: Float = 1f,
    maxDistance: Float = 1000f,
    minPolarAngle: Float = 0f,
    maxPolarAngle: Float = PI.toFloat(),
    minAzimuthAngle: Float = -Float.MAX_VALUE,
    maxAzimuthAngle: Float = Float.MAX_VALUE,
    rotateSpeed: Float = 1f,
    zoomSpeed: Float = 1f,
    panSpeed: Float = 1f,
    keyboardSpeed: Float = 1f,
    enableRotate: Boolean = true,
    enableZoom: Boolean = true,
    enablePan: Boolean = true,
    enableKeys: Boolean = true,
    autoRotate: Boolean = false,
    autoRotateSpeed: Float = 2f
) {
    val canvasState = LocalMateriaCanvasState.current ?: return
    val camera = canvasState.camera ?: return

    val controls = remember(
        camera,
        enableDamping,
        dampingFactor,
        minDistance,
        maxDistance,
        minPolarAngle,
        maxPolarAngle,
        minAzimuthAngle,
        maxAzimuthAngle,
        rotateSpeed,
        zoomSpeed,
        panSpeed,
        keyboardSpeed,
        enableRotate,
        enableZoom,
        enablePan,
        enableKeys,
        autoRotate,
        autoRotateSpeed
    ) {
        OrbitControls(
            camera,
            ControlsConfig(
                minDistance = minDistance,
                maxDistance = maxDistance,
                minPolarAngle = minPolarAngle,
                maxPolarAngle = maxPolarAngle,
                minAzimuthAngle = minAzimuthAngle,
                maxAzimuthAngle = maxAzimuthAngle,
                rotateSpeed = rotateSpeed,
                zoomSpeed = zoomSpeed,
                panSpeed = panSpeed,
                keyboardSpeed = keyboardSpeed,
                enableRotate = enableRotate,
                enableZoom = enableZoom,
                enablePan = enablePan,
                enableKeys = enableKeys,
                enableDamping = enableDamping,
                dampingFactor = dampingFactor,
                autoRotate = autoRotate,
                autoRotateSpeed = autoRotateSpeed
            )
        )
    }

    SideEffect {
        controls.target.copy(target)
    }

    DisposableEffect(controls, canvasState, canvasState.canvas) {
        canvasState.registerControls(controls)
        val binding = bindOrbitControls(controls, canvasState)

        onDispose {
            binding?.dispose()
            canvasState.unregisterControls(controls)
        }
    }
}

private fun applyModelSettings(
    root: Object3D,
    castShadow: Boolean,
    receiveShadow: Boolean,
    overrides: List<ModelMaterialOverride>
) {
    root.traverse { node ->
        val mesh = node as? Mesh ?: return@traverse
        mesh.castShadow = castShadow
        mesh.receiveShadow = receiveShadow
        applyMaterialOverrides(mesh, overrides)
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

private fun createFallbackModel(): Mesh {
    val geometry = BoxGeometry(1f, 1f, 1f)
    val material = MeshBasicMaterial().apply {
        color = Color(0.9f, 0.2f, 0.2f)
        wireframe = true
    }
    return Mesh(geometry, material)
}

internal interface OrbitControlsBinding {
    fun dispose()
}

internal expect fun bindOrbitControls(
    controls: OrbitControls,
    canvasState: MateriaCanvasState
): OrbitControlsBinding?
