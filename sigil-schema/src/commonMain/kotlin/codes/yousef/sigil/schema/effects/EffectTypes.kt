package codes.yousef.sigil.schema.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 2D vector for screen-space coordinates and uniforms.
 */
@Serializable
data class Vec2(
    val x: Float = 0f,
    val y: Float = 0f
) {
    constructor() : this(0f, 0f)
    
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun div(scalar: Float) = Vec2(x / scalar, y / scalar)
    
    fun toList() = listOf(x, y)
    
    companion object {
        val ZERO = Vec2(0f, 0f)
        val ONE = Vec2(1f, 1f)
    }
}

/**
 * 3D vector for color values and spatial data.
 */
@Serializable
data class Vec3(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    constructor() : this(0f, 0f, 0f)
    
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3(x / scalar, y / scalar, z / scalar)
    
    fun toList() = listOf(x, y, z)
    
    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val ONE = Vec3(1f, 1f, 1f)
    }
}

/**
 * 4D vector for RGBA colors and quaternions.
 */
@Serializable
data class Vec4(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val w: Float = 0f
) {
    constructor() : this(0f, 0f, 0f, 0f)
    
    operator fun plus(other: Vec4) = Vec4(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Vec4) = Vec4(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(scalar: Float) = Vec4(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun div(scalar: Float) = Vec4(x / scalar, y / scalar, z / scalar, w / scalar)
    
    fun toList() = listOf(x, y, z, w)
    
    companion object {
        val ZERO = Vec4(0f, 0f, 0f, 0f)
        val ONE = Vec4(1f, 1f, 1f, 1f)
    }
}

/**
 * Context provided to effects each frame during update.
 * Contains all time-varying and user-input data needed for effect rendering.
 */
@Serializable
data class EffectContext(
    /** Current time in seconds since effect started */
    val time: Float,
    /** Time elapsed since last frame in seconds */
    val deltaTime: Float,
    /** Canvas resolution in pixels [width, height] */
    val resolution: Vec2,
    /** Normalized mouse position [0-1, 0-1] */
    val mouse: Vec2 = Vec2(0.5f, 0.5f),
    /** Current scroll position in pixels */
    val scroll: Float = 0f
)

/**
 * Blend modes for compositing multiple effects.
 */
@Serializable
enum class BlendMode {
    @SerialName("normal") NORMAL,
    @SerialName("additive") ADDITIVE,
    @SerialName("multiply") MULTIPLY,
    @SerialName("screen") SCREEN,
    @SerialName("overlay") OVERLAY
}

/**
 * Power preference for GPU selection.
 */
@Serializable
enum class PowerPreference {
    @SerialName("low-power") LOW_POWER,
    @SerialName("high-performance") HIGH_PERFORMANCE
}

/**
 * Mouse button identifiers for interaction events.
 */
@Serializable
enum class MouseButton {
    @SerialName("left") LEFT,
    @SerialName("middle") MIDDLE,
    @SerialName("right") RIGHT
}
