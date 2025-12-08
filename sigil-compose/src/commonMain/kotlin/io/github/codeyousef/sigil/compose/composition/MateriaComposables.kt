package io.github.codeyousef.sigil.compose.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.codeyousef.sigil.compose.applier.MateriaApplier
import io.github.codeyousef.sigil.compose.node.MateriaNodeWrapper
import io.materia.core.scene.Object3D
import io.materia.core.scene.Scene
import io.materia.core.scene.Mesh
import io.materia.core.scene.Group
import io.materia.material.MeshBasicMaterial
import io.materia.material.MeshStandardMaterial
import io.materia.core.math.Color
import io.materia.core.math.Vector3
import io.materia.geometry.primitives.BoxGeometry
import io.materia.geometry.primitives.SphereGeometry
import io.materia.geometry.primitives.PlaneGeometry
import io.materia.geometry.primitives.CylinderGeometry
import io.materia.geometry.ConeGeometry
import io.materia.geometry.primitives.TorusGeometry
import io.materia.geometry.CircleGeometry
import io.materia.geometry.primitives.RingGeometry
import io.materia.geometry.IcosahedronGeometry
import io.materia.geometry.OctahedronGeometry
import io.materia.geometry.TetrahedronGeometry
import io.materia.geometry.DodecahedronGeometry
import io.materia.geometry.BufferGeometry
import io.materia.lighting.AmbientLightImpl
import io.materia.lighting.DirectionalLightImpl
import io.materia.lighting.PointLightImpl
import io.materia.lighting.SpotLightImpl
import io.materia.lighting.HemisphereLightImpl
import io.materia.camera.PerspectiveCamera
import io.materia.camera.OrthographicCamera
import io.github.codeyousef.sigil.schema.GeometryType
import io.github.codeyousef.sigil.schema.GeometryParams
import io.github.codeyousef.sigil.compose.context.LocalMateriaLightingContext

/**
 * Generic Composable node for creating Materia scene graph objects.
 *
 * This is the core building block that bridges Compose's ComposeNode API
 * with Materia's Object3D hierarchy.
 *
 * @param T The type of Materia Object3D being created
 * @param factory Lambda to create the Materia node
 * @param update Lambda to update the node's properties
 */
@Composable
inline fun <reified T : Object3D> MateriaNode(
    crossinline factory: () -> T,
    crossinline update: (T) -> Unit = {}
) {
    ComposeNode<MateriaNodeWrapper, MateriaApplier>(
        factory = {
            MateriaNodeWrapper(factory())
        },
        update = {
            set(Unit) {
                @Suppress("UNCHECKED_CAST")
                update(this.internalNode as T)
            }
        }
    )
}

/**
 * Composable node that can have children.
 */
@Composable
inline fun <reified T : Object3D> MateriaNodeWithContent(
    crossinline factory: () -> T,
    crossinline update: (T) -> Unit = {},
    content: @Composable () -> Unit
) {
    ComposeNode<MateriaNodeWrapper, MateriaApplier>(
        factory = {
            MateriaNodeWrapper(factory())
        },
        update = {
            set(Unit) {
                @Suppress("UNCHECKED_CAST")
                update(this.internalNode as T)
            }
        },
        content = content
    )
}

// =============================================================================
// Mesh Composables
// =============================================================================

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

// =============================================================================
// Group Composable
// =============================================================================

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

// =============================================================================
// Light Composables
// =============================================================================

/**
 * Ambient light that illuminates all objects equally.
 * 
 * Note: Lights in Materia are managed through a LightingSystem rather than
 * the scene graph. Ensure a MateriaLightingContext is provided.
 */
@Composable
fun AmbientLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(color, intensity, castShadow) {
        val light = AmbientLightImpl().apply {
            this.color = intToColor(color)
            this.intensity = intensity
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

/**
 * Directional light that emits parallel rays (like sunlight).
 */
@Composable
fun DirectionalLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3 = Vector3(5f, 10f, 7.5f),
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(color, intensity, position, castShadow) {
        val light = DirectionalLightImpl().apply {
            this.color = intToColor(color)
            this.intensity = intensity
            this.position = position
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

/**
 * Point light that emits from a single point in all directions.
 */
@Composable
fun PointLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3 = Vector3.ZERO,
    distance: Float = 0f,
    decay: Float = 2f,
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(color, intensity, position, distance, decay, castShadow) {
        val light = PointLightImpl().apply {
            this.color = intToColor(color)
            this.intensity = intensity
            this.position = position
            this.distance = distance
            this.decay = decay
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

/**
 * Spot light that emits a cone of light.
 */
@Composable
fun SpotLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3 = Vector3.ZERO,
    distance: Float = 0f,
    angle: Float = 0.523599f, // PI/6
    penumbra: Float = 0f,
    decay: Float = 2f,
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(color, intensity, position, distance, angle, penumbra, decay, castShadow) {
        val light = SpotLightImpl().apply {
            this.color = intToColor(color)
            this.intensity = intensity
            this.position = position
            this.distance = distance
            this.angle = angle
            this.penumbra = penumbra
            this.decay = decay
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

/**
 * Hemisphere light with sky and ground colors.
 */
@Composable
fun HemisphereLight(
    skyColor: Int = 0xFF87CEEB.toInt(),
    groundColor: Int = 0xFF362D1A.toInt(),
    intensity: Float = 1f,
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(skyColor, groundColor, intensity, castShadow) {
        val light = HemisphereLightImpl().apply {
            this.color = intToColor(skyColor)
            this.groundColor = intToColor(groundColor)
            this.intensity = intensity
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

// =============================================================================
// Camera Composables
// =============================================================================

/**
 * Perspective camera with field-of-view projection.
 */
@Composable
fun PerspectiveCamera(
    fov: Float = 75f,
    aspect: Float = 1.777778f,
    near: Float = 0.1f,
    far: Float = 1000f,
    position: Vector3 = Vector3(0f, 0f, 5f),
    lookAt: Vector3? = null,
    visible: Boolean = true,
    name: String = ""
) {
    MateriaNode(
        factory = { io.materia.camera.PerspectiveCamera(fov, aspect, near, far) },
        update = { camera ->
            camera.fov = fov
            camera.aspect = aspect
            camera.near = near
            camera.far = far
            camera.position.copy(position)
            lookAt?.let { camera.lookAt(it) }
            camera.visible = visible
            camera.name = name
            camera.updateProjectionMatrix()
        }
    )
}

/**
 * Orthographic camera with parallel projection.
 */
@Composable
fun OrthographicCamera(
    left: Float = -1f,
    right: Float = 1f,
    top: Float = 1f,
    bottom: Float = -1f,
    near: Float = 0.1f,
    far: Float = 1000f,
    position: Vector3 = Vector3(0f, 0f, 5f),
    lookAt: Vector3? = null,
    visible: Boolean = true,
    name: String = ""
) {
    MateriaNode(
        factory = { io.materia.camera.OrthographicCamera(left, right, top, bottom, near, far) },
        update = { camera ->
            camera.left = left
            camera.right = right
            camera.top = top
            camera.bottom = bottom
            camera.near = near
            camera.far = far
            camera.position.copy(position)
            lookAt?.let { camera.lookAt(it) }
            camera.visible = visible
            camera.name = name
            camera.updateProjectionMatrix()
        }
    )
}

// =============================================================================
// Helper Functions
// =============================================================================

/**
 * Convert an ARGB int to Materia Color.
 */
private fun intToColor(argb: Int): Color {
    val a = ((argb shr 24) and 0xFF) / 255f
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return Color(r, g, b)
}

/**
 * Create a BufferGeometry based on the geometry type and parameters.
 */
private fun createGeometry(type: GeometryType, params: GeometryParams): BufferGeometry {
    return when (type) {
        GeometryType.BOX -> BoxGeometry(
            width = params.width,
            height = params.height,
            depth = params.depth,
            widthSegments = params.widthSegments,
            heightSegments = params.heightSegments,
            depthSegments = params.widthSegments
        )
        GeometryType.SPHERE -> SphereGeometry(
            radius = params.radius,
            widthSegments = params.widthSegments.coerceAtLeast(8),
            heightSegments = params.heightSegments.coerceAtLeast(6)
        )
        GeometryType.PLANE -> PlaneGeometry(
            width = params.width,
            height = params.height,
            widthSegments = params.widthSegments,
            heightSegments = params.heightSegments
        )
        GeometryType.CYLINDER -> CylinderGeometry(
            radiusTop = params.radiusTop,
            radiusBottom = params.radiusBottom,
            height = params.height,
            radialSegments = params.radialSegments,
            heightSegments = params.heightSegments,
            openEnded = params.openEnded
        )
        GeometryType.CONE -> ConeGeometry(
            radius = params.radius,
            height = params.height,
            radialSegments = params.radialSegments,
            heightSegments = params.heightSegments,
            openEnded = params.openEnded
        )
        GeometryType.TORUS -> TorusGeometry(
            radius = params.radius,
            tube = params.tube,
            radialSegments = params.radialSegments,
            tubularSegments = params.tubularSegments
        )
        GeometryType.CIRCLE -> CircleGeometry(
            radius = params.radius,
            segments = params.radialSegments
        )
        GeometryType.RING -> RingGeometry(
            innerRadius = params.innerRadius,
            outerRadius = params.outerRadius,
            thetaSegments = params.radialSegments
        )
        GeometryType.ICOSAHEDRON -> IcosahedronGeometry(
            radius = params.radius,
            detail = params.detail
        )
        GeometryType.OCTAHEDRON -> OctahedronGeometry(
            radius = params.radius,
            detail = params.detail
        )
        GeometryType.TETRAHEDRON -> TetrahedronGeometry(
            radius = params.radius,
            detail = params.detail
        )
        GeometryType.DODECAHEDRON -> DodecahedronGeometry(
            radius = params.radius,
            detail = params.detail
        )
    }
}
