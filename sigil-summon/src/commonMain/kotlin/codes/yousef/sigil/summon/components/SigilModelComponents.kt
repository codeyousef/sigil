package codes.yousef.sigil.summon.components

import codes.yousef.summon.annotation.Composable
import codes.yousef.sigil.schema.ControlsData
import codes.yousef.sigil.schema.ControlsType
import codes.yousef.sigil.schema.ModelData
import codes.yousef.sigil.schema.ModelMaterialOverride
import codes.yousef.sigil.schema.generateNodeId
import codes.yousef.sigil.summon.context.SigilSummonContext

/**
 * Summon component for loading a glTF/GLB model.
 *
 * @param url URL or file path to the model
 * @param position Local position [x, y, z]
 * @param rotation Local rotation in radians [x, y, z]
 * @param scale Local scale [x, y, z]
 * @param visible Whether the model is visible
 * @param castShadow Whether meshes cast shadows
 * @param receiveShadow Whether meshes receive shadows
 * @param name Optional name for debugging
 * @param materialOverrides Optional material overrides (by material/mesh name)
 */
@Composable
fun SigilModel(
    url: String,
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    visible: Boolean = true,
    castShadow: Boolean = true,
    receiveShadow: Boolean = true,
    name: String? = null,
    materialOverrides: List<ModelMaterialOverride> = emptyList()
): String {
    val context = SigilSummonContext.current()

    val modelData = ModelData(
        id = generateNodeId(),
        position = position,
        rotation = rotation,
        scale = scale,
        visible = visible,
        name = name,
        url = url,
        castShadow = castShadow,
        receiveShadow = receiveShadow,
        materialOverrides = materialOverrides
    )

    context.registerNode(modelData)

    return ""
}

/**
 * Summon component for orbit camera controls.
 */
@Composable
fun SigilOrbitControls(
    target: List<Float> = listOf(0f, 0f, 0f),
    enableDamping: Boolean = true,
    dampingFactor: Float = 0.05f,
    minDistance: Float = 1f,
    maxDistance: Float = 1000f,
    minPolarAngle: Float = 0f,
    maxPolarAngle: Float = kotlin.math.PI.toFloat(),
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
    autoRotateSpeed: Float = 2f,
    name: String? = null
): String {
    val context = SigilSummonContext.current()

    val controlsData = ControlsData(
        id = generateNodeId(),
        name = name,
        controlsType = ControlsType.ORBIT,
        target = target,
        enableDamping = enableDamping,
        dampingFactor = dampingFactor,
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
        autoRotate = autoRotate,
        autoRotateSpeed = autoRotateSpeed
    )

    context.registerNode(controlsData)

    return ""
}
