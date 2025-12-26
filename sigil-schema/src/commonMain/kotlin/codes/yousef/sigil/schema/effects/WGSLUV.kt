package codes.yousef.sigil.schema.effects

/**
 * UV and coordinate manipulation functions for WGSL shaders.
 */
object WGSLUV {
    /**
     * Center UV coordinates (0,0 at center).
     */
    const val CENTER = """
// Center UV coordinates
fn centerUV(uv: vec2<f32>, resolution: vec2<f32>) -> vec2<f32> {
    return (uv * 2.0 - 1.0) * vec2<f32>(resolution.x / resolution.y, 1.0);
}
"""

    /**
     * Rotate UV coordinates.
     */
    const val ROTATE = """
// Rotate UV
fn rotateUV(uv: vec2<f32>, angle: f32) -> vec2<f32> {
    let c = cos(angle);
    let s = sin(angle);
    return vec2<f32>(uv.x * c - uv.y * s, uv.x * s + uv.y * c);
}
"""

    /**
     * Scale UV coordinates from center.
     */
    const val SCALE = """
// Scale UV from center
fn scaleUV(uv: vec2<f32>, scale: f32) -> vec2<f32> {
    return (uv - 0.5) * scale + 0.5;
}
"""
}
