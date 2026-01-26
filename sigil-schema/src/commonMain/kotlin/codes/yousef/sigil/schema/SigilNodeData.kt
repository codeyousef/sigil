package codes.yousef.sigil.schema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI

/**
 * Default JSON configuration for Sigil serialization.
 * Uses lenient parsing and encodes defaults for complete scene reconstruction.
 */
val SigilJson = Json {
    prettyPrint = false
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = true
    classDiscriminator = "type"
}

/**
 * Base sealed class for all scene graph nodes in Sigil.
 * All node types are serializable and can be transmitted between JVM and JS.
 * 
 * Vectors are represented as List<Float> for cross-platform portability,
 * avoiding platform-specific Vec3 types.
 */
@Serializable
sealed class SigilNodeData {
    /**
     * Unique identifier for this node, used for reconciliation and hydration.
     */
    abstract val id: String

    /**
     * Local position relative to parent node.
     * Format: [x, y, z]
     */
    abstract val position: List<Float>

    /**
     * Local rotation in euler angles (radians).
     * Format: [x, y, z]
     */
    abstract val rotation: List<Float>

    /**
     * Local scale multiplier.
     * Format: [x, y, z]
     */
    abstract val scale: List<Float>

    /**
     * Whether this node is visible in the scene.
     */
    abstract val visible: Boolean

    /**
     * Optional name for debugging and scene traversal.
     */
    abstract val name: String?
}

/**
 * Represents a renderable mesh in the scene graph.
 * Combines geometry and material properties for a complete renderable object.
 */
@Serializable
@SerialName("mesh")
data class MeshData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,

    /**
     * Type of geometry primitive.
     * Supported: "BOX", "SPHERE", "PLANE", "CYLINDER", "CONE", "TORUS"
     */
    val geometryType: GeometryType = GeometryType.BOX,

    /**
     * Geometry-specific parameters (dimensions, segments, etc.)
     */
    val geometryParams: GeometryParams = GeometryParams(),

    /**
     * Material color in ARGB hex format (e.g., 0xFFFF0000 for red)
     */
    val materialColor: Int = 0xFFFFFFFF.toInt(),

    /**
     * Material metalness value (0.0 - 1.0)
     */
    val metalness: Float = 0f,

    /**
     * Material roughness value (0.0 - 1.0)
     */
    val roughness: Float = 1f,

    /**
     * Whether this mesh casts shadows
     */
    val castShadow: Boolean = true,

    /**
     * Whether this mesh receives shadows
     */
    val receiveShadow: Boolean = true
) : SigilNodeData()

/**
 * Material overrides for glTF model meshes.
 *
 * If [target] is null, the override applies to all mesh materials.
 * If [target] is set, it is matched against material or mesh names.
 */
@Serializable
data class ModelMaterialOverride(
    val target: String? = null,
    val color: Int? = null,
    val metalness: Float? = null,
    val roughness: Float? = null
)

/**
 * Represents a glTF model in the scene graph.
 */
@Serializable
@SerialName("model")
data class ModelData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,

    /**
     * URL or file path to a .gltf/.glb asset.
     */
    val url: String,

    /**
     * Whether meshes in the model cast shadows.
     */
    val castShadow: Boolean = true,

    /**
     * Whether meshes in the model receive shadows.
     */
    val receiveShadow: Boolean = true,

    /**
     * Optional material overrides applied after loading.
     */
    val materialOverrides: List<ModelMaterialOverride> = emptyList()
) : SigilNodeData()

/**
 * Container node for grouping child nodes together.
 * Provides hierarchical scene organization.
 */
@Serializable
@SerialName("group")
data class GroupData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,

    /**
     * Child nodes within this group
     */
    val children: List<SigilNodeData> = emptyList()
) : SigilNodeData()

/**
 * Light source node for scene illumination.
 */
@Serializable
@SerialName("light")
data class LightData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,

    /**
     * Type of light source
     */
    val lightType: LightType = LightType.POINT,

    /**
     * Light color in ARGB hex format
     */
    val color: Int = 0xFFFFFFFF.toInt(),

    /**
     * Light intensity multiplier
     */
    val intensity: Float = 1f,

    /**
     * Maximum distance for point/spot lights (0 = infinite)
     */
    val distance: Float = 0f,

    /**
     * Decay rate for point/spot lights (physically correct = 2)
     */
    val decay: Float = 2f,

    /**
     * Cone angle for spot lights (radians)
     */
    val angle: Float = 0.523599f, // PI/6

    /**
     * Soft edge for spot lights (0-1)
     */
    val penumbra: Float = 0f,

    /**
     * Whether this light casts shadows
     */
    val castShadow: Boolean = false,

    /**
     * Target position for directional/spot lights
     * Format: [x, y, z]
     */
    val target: List<Float> = listOf(0f, 0f, 0f)
) : SigilNodeData()

/**
 * Camera node for scene viewing.
 */
@Serializable
@SerialName("camera")
data class CameraData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 5f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,

    /**
     * Type of camera projection
     */
    val cameraType: CameraType = CameraType.PERSPECTIVE,

    /**
     * Field of view in degrees (perspective camera)
     */
    val fov: Float = 75f,

    /**
     * Aspect ratio (perspective camera)
     */
    val aspect: Float = 1.777778f, // 16:9

    /**
     * Near clipping plane
     */
    val near: Float = 0.1f,

    /**
     * Far clipping plane
     */
    val far: Float = 1000f,

    /**
     * Orthographic camera bounds (left, right, top, bottom)
     */
    val orthoBounds: List<Float> = listOf(-1f, 1f, 1f, -1f),

    /**
     * Look-at target position
     * Format: [x, y, z]
     */
    val lookAt: List<Float>? = null
) : SigilNodeData()

/**
 * Camera controls configuration node.
 */
@Serializable
@SerialName("controls")
data class ControlsData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,

    val controlsType: ControlsType = ControlsType.ORBIT,
    val target: List<Float> = listOf(0f, 0f, 0f),
    val enableDamping: Boolean = true,
    val dampingFactor: Float = 0.05f,
    val minDistance: Float = 1f,
    val maxDistance: Float = 1000f,
    val minPolarAngle: Float = 0f,
    val maxPolarAngle: Float = PI.toFloat(),
    val minAzimuthAngle: Float = -Float.MAX_VALUE,
    val maxAzimuthAngle: Float = Float.MAX_VALUE,
    val rotateSpeed: Float = 1f,
    val zoomSpeed: Float = 1f,
    val panSpeed: Float = 1f,
    val keyboardSpeed: Float = 1f,
    val enableRotate: Boolean = true,
    val enableZoom: Boolean = true,
    val enablePan: Boolean = true,
    val enableKeys: Boolean = true,
    val autoRotate: Boolean = false,
    val autoRotateSpeed: Float = 2f
) : SigilNodeData()

/**
 * Supported geometry primitive types.
 */
@Serializable
enum class GeometryType {
    @SerialName("BOX") BOX,
    @SerialName("SPHERE") SPHERE,
    @SerialName("PLANE") PLANE,
    @SerialName("CYLINDER") CYLINDER,
    @SerialName("CONE") CONE,
    @SerialName("TORUS") TORUS,
    @SerialName("CIRCLE") CIRCLE,
    @SerialName("RING") RING,
    @SerialName("ICOSAHEDRON") ICOSAHEDRON,
    @SerialName("OCTAHEDRON") OCTAHEDRON,
    @SerialName("TETRAHEDRON") TETRAHEDRON,
    @SerialName("DODECAHEDRON") DODECAHEDRON
}

/**
 * Geometry-specific parameters for primitive creation.
 * Default values are used for unspecified parameters based on geometry type.
 */
@Serializable
data class GeometryParams(
    // Box parameters
    val width: Float = 1f,
    val height: Float = 1f,
    val depth: Float = 1f,

    // Sphere/Circle/Icosahedron parameters
    val radius: Float = 1f,

    // Segment counts
    val widthSegments: Int = 1,
    val heightSegments: Int = 1,
    val radialSegments: Int = 32,

    // Cylinder/Cone parameters
    val radiusTop: Float = 1f,
    val radiusBottom: Float = 1f,
    val openEnded: Boolean = false,

    // Torus parameters
    val tube: Float = 0.4f,
    val tubularSegments: Int = 48,

    // Ring parameters
    val innerRadius: Float = 0.5f,
    val outerRadius: Float = 1f,

    // Polyhedron detail level
    val detail: Int = 0
)

/**
 * Supported light types.
 */
@Serializable
enum class LightType {
    @SerialName("AMBIENT") AMBIENT,
    @SerialName("DIRECTIONAL") DIRECTIONAL,
    @SerialName("POINT") POINT,
    @SerialName("SPOT") SPOT,
    @SerialName("HEMISPHERE") HEMISPHERE
}

/**
 * Supported camera types.
 */
@Serializable
enum class CameraType {
    @SerialName("PERSPECTIVE") PERSPECTIVE,
    @SerialName("ORTHOGRAPHIC") ORTHOGRAPHIC
}

/**
 * Supported camera control types.
 */
@Serializable
enum class ControlsType {
    @SerialName("ORBIT") ORBIT
}
