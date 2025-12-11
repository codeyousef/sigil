package codes.yousef.sigil.schema.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed class representing different types of uniform values.
 * Supports all common WGSL/shader uniform types.
 */
@Serializable
sealed class UniformValue {
    
    @Serializable
    @SerialName("float")
    data class FloatValue(val value: Float) : UniformValue()
    
    @Serializable
    @SerialName("int")
    data class IntValue(val value: Int) : UniformValue()
    
    @Serializable
    @SerialName("vec2")
    data class Vec2Value(val value: Vec2) : UniformValue()
    
    @Serializable
    @SerialName("vec3")
    data class Vec3Value(val value: Vec3) : UniformValue()
    
    @Serializable
    @SerialName("vec4")
    data class Vec4Value(val value: Vec4) : UniformValue()
    
    @Serializable
    @SerialName("mat3")
    data class Mat3Value(val values: List<Float>) : UniformValue() {
        init {
            require(values.size == 9) { "Mat3 requires exactly 9 values" }
        }
    }
    
    @Serializable
    @SerialName("mat4")
    data class Mat4Value(val values: List<Float>) : UniformValue() {
        init {
            require(values.size == 16) { "Mat4 requires exactly 16 values" }
        }
    }
}

/**
 * Type-safe builder for uniform blocks.
 */
class UniformBlockBuilder {
    private val uniforms = mutableMapOf<String, UniformValue>()
    
    fun float(name: String, value: Float = 0f) {
        uniforms[name] = UniformValue.FloatValue(value)
    }
    
    fun int(name: String, value: Int = 0) {
        uniforms[name] = UniformValue.IntValue(value)
    }
    
    fun vec2(name: String, value: Vec2 = Vec2()) {
        uniforms[name] = UniformValue.Vec2Value(value)
    }
    
    fun vec3(name: String, value: Vec3 = Vec3()) {
        uniforms[name] = UniformValue.Vec3Value(value)
    }
    
    fun vec4(name: String, value: Vec4 = Vec4()) {
        uniforms[name] = UniformValue.Vec4Value(value)
    }
    
    fun build(): Map<String, UniformValue> = uniforms.toMap()
}

/**
 * DSL function for building uniform blocks.
 */
fun uniformBlock(builder: UniformBlockBuilder.() -> Unit): Map<String, UniformValue> {
    return UniformBlockBuilder().apply(builder).build()
}
