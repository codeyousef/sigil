package codes.yousef.sigil.compose.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.materia.core.math.Vector3
import io.materia.core.scene.Mesh
import io.materia.core.scene.Group
import io.materia.material.MeshBasicMaterial
import io.materia.material.MeshStandardMaterial
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.GeometryParams

/**
 * Create a mesh with the specified geometry and material properties.
 *
 * @param geometryType Type of primitive geometry
 * @param geometryParams Geometry-specific parameters
 * @param position Local position [x, y, z]
 * @param rotation Local rotation in radians [x, y, z]
 * @param scale Local scale [x, y, z]
 * @param color Material color (ARGB int)
 * @param metalness PBR metalness (0-1)
 * @param roughness PBR roughness (0-1)
 * @param visible Whether the mesh is visible
 * @param castShadow Whether the mesh casts shadows
 * @param receiveShadow Whether the mesh receives shadows
 * @param name Optional name for debugging
 */
@Composable
fun Mesh(
    geometryType: GeometryType = GeometryType.BOX,
    geometryParams: GeometryParams = GeometryParams(),
    position: Vector3 = Vector3.ZERO,
    rotation: Vector3 = Vector3.ZERO,
    scale: Vector3 = Vector3.ONE,
    color: Int = 0xFFFFFFFF.toInt(),
    metalness: Float = 0f,
    roughness: Float = 1f,
    visible: Boolean = true,
    castShadow: Boolean = true,
    receiveShadow: Boolean = true,
    name: String = ""
) {
    val geometry = remember(geometryType, geometryParams) {
        createGeometry(geometryType, geometryParams)
    }

    val material = remember(color, metalness, roughness) {
        if (metalness > 0f || roughness < 1f) {
            MeshStandardMaterial().apply {
                this.color = intToColor(color)
                this.metalness = metalness
                this.roughness = roughness
            }
        } else {
            MeshBasicMaterial().apply {
                this.color = intToColor(color)
            }
        }
    }

    MateriaNode(
        factory = { Mesh(geometry, material) },
        update = { mesh ->
            mesh.position.copy(position)
            mesh.rotation.set(rotation.x, rotation.y, rotation.z)
            mesh.scale.copy(scale)
            mesh.visible = visible
            mesh.castShadow = castShadow
            mesh.receiveShadow = receiveShadow
            mesh.name = name
        }
    )
}

/**
 * Convenience function for creating a box mesh.
 */
@Composable
fun Box(
    width: Float = 1f,
    height: Float = 1f,
    depth: Float = 1f,
    position: Vector3 = Vector3.ZERO,
    rotation: Vector3 = Vector3.ZERO,
    scale: Vector3 = Vector3.ONE,
    color: Int = 0xFFFFFFFF.toInt(),
    metalness: Float = 0f,
    roughness: Float = 1f,
    visible: Boolean = true,
    castShadow: Boolean = true,
    receiveShadow: Boolean = true,
    name: String = ""
) {
    Mesh(
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
}

/**
 * Convenience function for creating a sphere mesh.
 */
@Composable
fun Sphere(
    radius: Float = 1f,
    widthSegments: Int = 32,
    heightSegments: Int = 16,
    position: Vector3 = Vector3.ZERO,
    rotation: Vector3 = Vector3.ZERO,
    scale: Vector3 = Vector3.ONE,
    color: Int = 0xFFFFFFFF.toInt(),
    metalness: Float = 0f,
    roughness: Float = 1f,
    visible: Boolean = true,
    castShadow: Boolean = true,
    receiveShadow: Boolean = true,
    name: String = ""
) {
    Mesh(
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
}

/**
 * Convenience function for creating a plane mesh.
 */
@Composable
fun Plane(
    width: Float = 1f,
    height: Float = 1f,
    position: Vector3 = Vector3.ZERO,
    rotation: Vector3 = Vector3.ZERO,
    scale: Vector3 = Vector3.ONE,
    color: Int = 0xFFFFFFFF.toInt(),
    metalness: Float = 0f,
    roughness: Float = 1f,
    visible: Boolean = true,
    receiveShadow: Boolean = true,
    name: String = ""
) {
    Mesh(
        geometryType = GeometryType.PLANE,
        geometryParams = GeometryParams(width = width, height = height),
        position = position,
        rotation = rotation,
        scale = scale,
        color = color,
        metalness = metalness,
        roughness = roughness,
        visible = visible,
        castShadow = false, // Planes typically don't cast shadows
        receiveShadow = receiveShadow,
        name = name
    )
}

/**
 * Group node for organizing child nodes in the scene hierarchy.
 * All transforms applied to the group affect its children.
 */
@Composable
fun Group(
    position: Vector3 = Vector3.ZERO,
    rotation: Vector3 = Vector3.ZERO,
    scale: Vector3 = Vector3.ONE,
    visible: Boolean = true,
    name: String = "",
    content: @Composable () -> Unit
) {
    MateriaNodeWithContent(
        factory = { Group() },
        update = { group ->
            group.position.copy(position)
            group.rotation.set(rotation.x, rotation.y, rotation.z)
            group.scale.copy(scale)
            group.visible = visible
            group.name = name
        },
        content = content
    )
}
