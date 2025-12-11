package codes.yousef.sigil.summon.components

import codes.yousef.summon.annotation.Composable
import codes.yousef.sigil.schema.LightData
import codes.yousef.sigil.schema.LightType
import codes.yousef.sigil.schema.generateNodeId
import codes.yousef.sigil.summon.context.SigilSummonContext

/**
 * Light component for scene illumination.
 */
@Composable
fun SigilLight(
    lightType: LightType = LightType.POINT,
    position: List<Float> = listOf(0f, 5f, 0f),
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    distance: Float = 0f,
    decay: Float = 2f,
    angle: Float = 0.523599f, // PI/6
    penumbra: Float = 0f,
    castShadow: Boolean = false,
    target: List<Float> = listOf(0f, 0f, 0f),
    visible: Boolean = true,
    name: String? = null
): String {
    val context = SigilSummonContext.current()

    val lightData = LightData(
        id = generateNodeId(),
        position = position,
        rotation = listOf(0f, 0f, 0f),
        scale = listOf(1f, 1f, 1f),
        visible = visible,
        name = name,
        lightType = lightType,
        color = color,
        intensity = intensity,
        distance = distance,
        decay = decay,
        angle = angle,
        penumbra = penumbra,
        castShadow = castShadow,
        target = target
    )

    context.registerNode(lightData)

    return ""
}

/**
 * Ambient light for base scene illumination.
 */
@Composable
fun SigilAmbientLight(
    color: Int = 0xFF404040.toInt(),
    intensity: Float = 0.4f,
    name: String? = null
): String = SigilLight(
    lightType = LightType.AMBIENT,
    color = color,
    intensity = intensity,
    name = name
)

/**
 * Directional light (like sunlight).
 */
@Composable
fun SigilDirectionalLight(
    position: List<Float> = listOf(5f, 10f, 7.5f),
    target: List<Float> = listOf(0f, 0f, 0f),
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    castShadow: Boolean = false,
    name: String? = null
): String = SigilLight(
    lightType = LightType.DIRECTIONAL,
    position = position,
    target = target,
    color = color,
    intensity = intensity,
    castShadow = castShadow,
    name = name
)

/**
 * Point light (omnidirectional).
 */
@Composable
fun SigilPointLight(
    position: List<Float> = listOf(0f, 5f, 0f),
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    distance: Float = 0f,
    decay: Float = 2f,
    castShadow: Boolean = false,
    name: String? = null
): String = SigilLight(
    lightType = LightType.POINT,
    position = position,
    color = color,
    intensity = intensity,
    distance = distance,
    decay = decay,
    castShadow = castShadow,
    name = name
)
