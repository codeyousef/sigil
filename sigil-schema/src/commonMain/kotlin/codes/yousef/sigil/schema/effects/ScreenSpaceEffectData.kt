package codes.yousef.sigil.schema.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing a custom shader effect.
 * Contains all information needed to create and render a fullscreen shader pass.
 * 
 * Supports both WebGPU (WGSL) and WebGL (GLSL) shaders. If both are provided,
 * the system will automatically select the appropriate shader based on browser
 * capabilities, preferring WebGPU when available.
 */
@Serializable
@SerialName("shaderEffect")
data class ShaderEffectData(
    /** Unique identifier for the effect */
    val id: String,
    /** Human-readable name for debugging */
    val name: String? = null,
    /** Blend mode for compositing */
    val blendMode: BlendMode = BlendMode.NORMAL,
    /** Opacity for compositing (0-1) */
    val opacity: Float = 1f,
    /** Whether the effect is currently enabled */
    val enabled: Boolean = true,
    
    /**
     * The WGSL fragment shader source code for WebGPU rendering.
     * Should be a complete fragment shader or a main function body
     * that will be wrapped in the standard effect structure.
     */
    val fragmentShader: String,
    
    /**
     * Optional GLSL fragment shader for WebGL fallback.
     * If provided, this shader will be used when WebGPU is unavailable.
     * Should output to gl_FragColor and use vUv for texture coordinates.
     */
    val glslFragmentShader: String? = null,
    
    /**
     * Optional vertex shader source code (WGSL for WebGPU).
     * If not provided, a default fullscreen quad vertex shader is used.
     */
    val vertexShader: String? = null,
    
    /**
     * Optional GLSL vertex shader for WebGL fallback.
     * If not provided, a default fullscreen triangle vertex shader is used.
     */
    val glslVertexShader: String? = null,
    
    /**
     * Uniform values to pass to the shader.
     * These are automatically bound before rendering.
     */
    val uniforms: Map<String, UniformValue> = emptyMap(),
    
    /**
     * Whether this effect responds to mouse interaction.
     */
    val enableMouseInteraction: Boolean = false,
    
    /**
     * Time scale multiplier for animation speed.
     */
    val timeScale: Float = 1f
) {
    /**
     * Check if this effect has a GLSL fallback shader.
     */
    fun hasGLSLFallback(): Boolean = glslFragmentShader != null
}

/**
 * Data class for effect composer configuration.
 * Contains multiple effects to be rendered in sequence.
 */
@Serializable
@SerialName("effectComposer")
data class EffectComposerData(
    val id: String,
    /**
     * List of effects to render in order.
     * Effects are composited based on their blend modes.
     */
    val effects: List<ShaderEffectData> = emptyList()
)

/**
 * Configuration for a SigilCanvas effect rendering.
 */
@Serializable
data class SigilCanvasConfig(
    /** Unique ID for the canvas element */
    val id: String = "sigil-canvas",
    /** Whether to respect device pixel ratio for crisp rendering */
    val respectDevicePixelRatio: Boolean = true,
    /** GPU power preference */
    val powerPreference: PowerPreference = PowerPreference.HIGH_PERFORMANCE,
    /** Whether to fall back to WebGL if WebGPU is unavailable */
    val fallbackToWebGL: Boolean = true,
    /** Whether to fall back to CSS if WebGL is also unavailable */
    val fallbackToCSS: Boolean = true
)

/**
 * Annotation to mark a class as a Sigil screen-space effect.
 * Enables automatic serialization registration and hydration support.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SigilEffect(
    /** Optional custom name for serialization (defaults to class name) */
    val name: String = ""
)
