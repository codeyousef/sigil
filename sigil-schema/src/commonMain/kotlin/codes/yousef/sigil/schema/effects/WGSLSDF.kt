package codes.yousef.sigil.schema.effects

/**
 * Signed Distance Functions for shapes in WGSL.
 */
object WGSLSDF {
    /**
     * SDF for a circle.
     */
    const val CIRCLE = """
// Circle SDF
fn sdCircle(p: vec2<f32>, r: f32) -> f32 {
    return length(p) - r;
}
"""

    /**
     * SDF for a box.
     */
    const val BOX = """
// Box SDF
fn sdBox(p: vec2<f32>, b: vec2<f32>) -> f32 {
    let d = abs(p) - b;
    return length(max(d, vec2<f32>(0.0))) + min(max(d.x, d.y), 0.0);
}
"""

    /**
     * SDF for a rounded box.
     */
    const val ROUNDED_BOX = """
// Rounded Box SDF
fn sdRoundedBox(p: vec2<f32>, b: vec2<f32>, r: f32) -> f32 {
    let q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2<f32>(0.0))) - r;
}
"""

    /**
     * Smooth minimum for combining SDFs.
     */
    const val SMOOTH_MIN = """
// Smooth minimum
fn smin(a: f32, b: f32, k: f32) -> f32 {
    let h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}
"""
}
