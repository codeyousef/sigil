package codes.yousef.sigil.schema.effects

/**
 * Standard struct definitions for WGSL effects.
 */
object WGSLStructs {
    /**
     * Standard uniforms struct for effects.
     */
    const val EFFECT_UNIFORMS = """
struct EffectUniforms {
    time: f32,
    deltaTime: f32,
    resolution: vec2<f32>,
    mouse: vec2<f32>,
    scroll: f32,
    _padding: f32,
}

@group(0) @binding(0)
var<uniform> uniforms: EffectUniforms;
"""

    /**
     * Standard vertex output struct.
     */
    const val VERTEX_OUTPUT = """
struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
}
"""
}
