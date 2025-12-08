package io.github.codeyousef.sigil.summon.components

import codes.yousef.summon.annotation.Composable
import codes.yousef.summon.modifier.Modifier
import io.github.codeyousef.sigil.schema.MeshData
import io.github.codeyousef.sigil.schema.GroupData
import io.github.codeyousef.sigil.schema.LightData
import io.github.codeyousef.sigil.schema.CameraData
import io.github.codeyousef.sigil.schema.GeometryType
import io.github.codeyousef.sigil.schema.GeometryParams
import io.github.codeyousef.sigil.schema.LightType
import io.github.codeyousef.sigil.schema.CameraType
import io.github.codeyousef.sigil.schema.generateNodeId
import io.github.codeyousef.sigil.summon.context.SigilSummonContext

/**
 * Summon component for creating a mesh in the 3D scene.
 *
 * This component registers mesh data in the SigilSummonContext.
 * On the server (JVM), it contributes to the serialized scene data.
 * On the client (JS), it creates actual Materia mesh objects.
 *
 * @param geometryType Type of geometry primitive
 * @param position Local position [x, y, z]
 * @param rotation Local rotation in radians [x, y, z]
 * @param scale Local scale [x, y, z]
 * @param color Material color in ARGB hex format
 * @param metalness PBR metalness (0-1)
 * @param roughness PBR roughness (0-1)
 * @param visible Whether the mesh is visible
 * @param castShadow Whether the mesh casts shadows
 * @param receiveShadow Whether the mesh receives shadows
 * @param name Optional name for debugging
 */
@Composable
fun SigilMesh(
    geometryType: GeometryType = GeometryType.BOX,
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    color: Int = 0xFFFFFFFF.toInt(),
    metalness: Float = 0f,
    roughness: Float = 1f,
    geometryParams: GeometryParams = GeometryParams(),
    visible: Boolean = true,
    castShadow: Boolean = true,
    receiveShadow: Boolean = true,
    name: String? = null
): String {
    val context = SigilSummonContext.current()

    val meshData = MeshData(
        id = generateNodeId(),
        position = position,
        rotation = rotation,
        scale = scale,
        visible = visible,
        name = name,
        geometryType = geometryType,
        geometryParams = geometryParams,
        materialColor = color,
        metalness = metalness,
        roughness = roughness,
        castShadow = castShadow,
        receiveShadow = receiveShadow
    )

    context.registerNode(meshData)

    // Return empty string - no visible HTML for 3D components
    return ""
}

/**
 * Convenience function for creating a box mesh.
 */
@Composable
fun SigilBox(
    width: Float = 1f,
    height: Float = 1f,
    depth: Float = 1f,
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    color: Int = 0xFFFFFFFF.toInt(),
    metalness: Float = 0f,
    roughness: Float = 1f,
    visible: Boolean = true,
    castShadow: Boolean = true,
    receiveShadow: Boolean = true,
    name: String? = null
): String = SigilMesh(
    geometryType = GeometryType.BOX,
    geometryParams = GeometryParams(width = width, height = height, depth = depth),
    position = position,
    rotation = rotation,
    scale = scale,
    color = color,
    metalness = metalness,
    roughness = roughness,
    visible = visible,
    castShadow = castShadow,
    receiveShadow = receiveShadow,
    name = name
)

/**
 * Convenience function for creating a sphere mesh.
 */
@Composable
fun SigilSphere(
    radius: Float = 1f,
    widthSegments: Int = 32,
    heightSegments: Int = 16,
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    color: Int = 0xFFFFFFFF.toInt(),
    metalness: Float = 0f,
    roughness: Float = 1f,
    visible: Boolean = true,
    castShadow: Boolean = true,
    receiveShadow: Boolean = true,
    name: String? = null
): String = SigilMesh(
    geometryType = GeometryType.SPHERE,
    geometryParams = GeometryParams(
        radius = radius,
        widthSegments = widthSegments,
        heightSegments = heightSegments
    ),
    position = position,
    rotation = rotation,
    scale = scale,
    color = color,
    metalness = metalness,
    roughness = roughness,
    visible = visible,
    castShadow = castShadow,
    receiveShadow = receiveShadow,
    name = name
)

/**
 * Convenience function for creating a plane mesh.
 */
@Composable
fun SigilPlane(
    width: Float = 1f,
    height: Float = 1f,
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    color: Int = 0xFFFFFFFF.toInt(),
    visible: Boolean = true,
    receiveShadow: Boolean = true,
    name: String? = null
): String = SigilMesh(
    geometryType = GeometryType.PLANE,
    geometryParams = GeometryParams(width = width, height = height),
    position = position,
    rotation = rotation,
    scale = scale,
    color = color,
    visible = visible,
    castShadow = false,
    receiveShadow = receiveShadow,
    name = name
)

/**
 * Group container for organizing child nodes.
 */
@Composable
fun SigilGroup(
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    visible: Boolean = true,
    name: String? = null,
    content: @Composable () -> String
): String {
    val context = SigilSummonContext.current()

    // Create a list to collect children
    val children = mutableListOf<io.github.codeyousef.sigil.schema.SigilNodeData>()

    // Enter group context
    context.enterGroup(children)

    // Execute content - this will add nodes to children list
    content()

    // Exit group context
    context.exitGroup()

    val groupData = GroupData(
        id = generateNodeId(),
        position = position,
        rotation = rotation,
        scale = scale,
        visible = visible,
        name = name,
        children = children.toList()
    )

    context.registerNode(groupData)

    return ""
}

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
