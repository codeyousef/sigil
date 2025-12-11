package codes.yousef.sigil.summon.components

import codes.yousef.summon.annotation.Composable
import codes.yousef.sigil.schema.MeshData
import codes.yousef.sigil.schema.GroupData
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.GeometryParams
import codes.yousef.sigil.schema.generateNodeId
import codes.yousef.sigil.summon.context.SigilSummonContext
import codes.yousef.sigil.schema.SigilNodeData

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
    val children = mutableListOf<SigilNodeData>()

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
