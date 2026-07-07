package codes.yousef.sigil.summon.components

import codes.yousef.sigil.schema.InteractionMetadata
import codes.yousef.sigil.schema.SceneAnimationData
import codes.yousef.sigil.schema.TextAlignMode
import codes.yousef.sigil.schema.TextBaselineMode
import codes.yousef.sigil.schema.TextData
import codes.yousef.sigil.schema.TextFacingMode
import codes.yousef.sigil.schema.generateNodeId
import codes.yousef.sigil.summon.context.SigilSummonContext
import codes.yousef.summon.annotation.Composable

/**
 * Summon component for mesh text inside a Sigil 3D scene.
 */
@Composable
fun SigilText(
    text: String,
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    visible: Boolean = true,
    name: String? = null,
    interaction: InteractionMetadata? = null,
    animations: List<SceneAnimationData> = emptyList(),
    color: Int = 0xFFFFFFFF.toInt(),
    size: Float = 1f,
    depth: Float = 0.02f,
    curveSegments: Int = 12,
    letterSpacing: Float = 0f,
    lineHeight: Float = 1.2f,
    align: TextAlignMode = TextAlignMode.LEFT,
    baseline: TextBaselineMode = TextBaselineMode.ALPHABETIC,
    maxWidth: Float? = null,
    wordWrap: Boolean = false,
    facingMode: TextFacingMode = TextFacingMode.FIXED,
    fontUrl: String? = null,
    castShadow: Boolean = false,
    receiveShadow: Boolean = false,
    id: String = generateNodeId()
): String {
    val context = SigilSummonContext.current()

    val textData = TextData(
        id = id,
        position = position,
        rotation = rotation,
        scale = scale,
        visible = visible,
        name = name,
        interaction = interaction,
        animations = animations,
        text = text,
        color = color,
        size = size,
        depth = depth,
        curveSegments = curveSegments,
        letterSpacing = letterSpacing,
        lineHeight = lineHeight,
        align = align,
        baseline = baseline,
        maxWidth = maxWidth,
        wordWrap = wordWrap,
        facingMode = facingMode,
        fontUrl = fontUrl,
        castShadow = castShadow,
        receiveShadow = receiveShadow
    )

    context.registerNode(textData)

    return ""
}
