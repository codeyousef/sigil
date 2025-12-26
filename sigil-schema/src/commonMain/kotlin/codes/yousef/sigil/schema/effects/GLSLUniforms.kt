package codes.yousef.sigil.schema.effects

/**
 * Standard struct and uniform declarations for GLSL shaders.
 */
object GLSLUniforms {
    /**
     * Standard varying declaration for UV coordinates.
     */
    const val VARYING_UV = """
varying vec2 vUv;
"""

    /**
     * Standard uniform declarations for effects.
     */
    const val STANDARD_UNIFORMS = """
uniform float time;
uniform float deltaTime;
uniform vec2 resolution;
uniform vec2 mouse;
uniform float mouseDown;
"""

    /**
     * Standard sampler for input texture (for multi-pass effects).
     */
    const val INPUT_TEXTURE = """
uniform sampler2D inputTexture;
"""
}
