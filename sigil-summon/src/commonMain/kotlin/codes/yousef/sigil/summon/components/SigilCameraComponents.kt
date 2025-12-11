package codes.yousef.sigil.summon.components

import codes.yousef.summon.annotation.Composable
import codes.yousef.sigil.schema.CameraData
import codes.yousef.sigil.schema.CameraType
import codes.yousef.sigil.schema.generateNodeId
import codes.yousef.sigil.summon.context.SigilSummonContext

/**
 * Camera component for scene viewing.
 */
@Composable
fun SigilCamera(
    cameraType: CameraType = CameraType.PERSPECTIVE,
    position: List<Float> = listOf(0f, 2f, 5f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    fov: Float = 75f,
    aspect: Float = 1.777778f,
    near: Float = 0.1f,
    far: Float = 1000f,
    lookAt: List<Float>? = null,
    visible: Boolean = true,
    name: String? = null
): String {
    val context = SigilSummonContext.current()

    val cameraData = CameraData(
        id = generateNodeId(),
        position = position,
        rotation = rotation,
        scale = listOf(1f, 1f, 1f),
        visible = visible,
        name = name,
        cameraType = cameraType,
        fov = fov,
        aspect = aspect,
        near = near,
        far = far,
        lookAt = lookAt
    )

    context.registerNode(cameraData)

    return ""
}
