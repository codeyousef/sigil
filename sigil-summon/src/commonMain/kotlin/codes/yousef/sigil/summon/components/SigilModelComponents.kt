package codes.yousef.sigil.summon.components

import codes.yousef.summon.annotation.Composable
import codes.yousef.sigil.schema.ControlsData
import codes.yousef.sigil.schema.ControlsType
import codes.yousef.sigil.schema.InteractionMetadata
import codes.yousef.sigil.schema.ModelData
import codes.yousef.sigil.schema.ModelMaterialOverride
import codes.yousef.sigil.schema.SceneAnimationData
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
    materialOverrides: List<ModelMaterialOverride> = emptyList(),
    interaction: InteractionMetadata? = null,
    animations: List<SceneAnimationData> = emptyList(),
    id: String = generateNodeId()
): String {
    val context = SigilSummonContext.current()

    val modelData = ModelData(
        id = id,
        position = position,
        rotation = rotation,
        scale = scale,
        visible = visible,
        name = name,
        interaction = interaction,
        animations = animations,
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
    name: String? = null,
    interaction: InteractionMetadata? = null,
    animations: List<SceneAnimationData> = emptyList(),
    id: String = generateNodeId()
): String {
    val context = SigilSummonContext.current()

    val controlsData = ControlsData(
        id = id,
        name = name,
        interaction = interaction,
        animations = animations,
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

/**
 * Summon component for first-person camera controls.
 *
 * @param moveSpeed Walking speed in units per second
 * @param lookSpeed Mouse sensitivity for camera look
 * @param pointerLock Whether to use pointer lock for mouse capture
 * @param position Initial camera position [x, y, z]
 * @param heightOffset Camera height above ground
 * @param enableGravity Whether gravity is applied
 * @param groundHeight Y position of the ground plane
 * @param enableCollision Whether collision detection is enabled
 * @param collisionRadius Collision sphere radius
 * @param name Optional name for debugging
 */
@Composable
fun SigilFirstPersonControls(
    moveSpeed: Float = 5f,
    lookSpeed: Float = 0.002f,
    pointerLock: Boolean = false,
    position: List<Float> = listOf(0f, 1.6f, 0f),
    heightOffset: Float = 1.6f,
    enableGravity: Boolean = true,
    groundHeight: Float = 0f,
    enableCollision: Boolean = false,
    collisionRadius: Float = 0.5f,
    name: String? = null,
    interaction: InteractionMetadata? = null,
    animations: List<SceneAnimationData> = emptyList(),
    id: String = generateNodeId()
): String {
    val context = SigilSummonContext.current()

    val controlsData = ControlsData(
        id = id,
        position = position,
        name = name,
        interaction = interaction,
        animations = animations,
        controlsType = ControlsType.FIRST_PERSON,
        moveSpeed = moveSpeed,
        lookSpeed = lookSpeed,
        pointerLock = pointerLock,
        heightOffset = heightOffset,
        enableGravity = enableGravity,
        groundHeight = groundHeight,
        enableCollision = enableCollision,
        collisionRadius = collisionRadius
    )

    context.registerNode(controlsData)

    return ""
}
