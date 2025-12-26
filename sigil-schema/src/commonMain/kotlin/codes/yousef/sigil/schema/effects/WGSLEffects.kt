package codes.yousef.sigil.schema.effects

/**
 * Common effect utilities for WGSL shaders.
 */
object WGSLEffects {
    /**
     * Vignette effect function.
     */
    const val VIGNETTE = """
// Vignette effect
fn vignette(uv: vec2<f32>, intensity: f32, smoothness: f32) -> f32 {
    let centered = uv * 2.0 - 1.0;
    let dist = length(centered);
    return 1.0 - smoothstep(1.0 - smoothness - intensity, 1.0 - smoothness, dist);
}
"""

    /**
     * Film grain effect function.
     */
    const val FILM_GRAIN = """
// Film grain
fn filmGrain(uv: vec2<f32>, time: f32, intensity: f32) -> f32 {
    let noise = fract(sin(dot(uv + time, vec2<f32>(12.9898, 78.233))) * 43758.5453);
    return (noise - 0.5) * intensity;
}
"""

    /**
     * Scanlines effect function.
     */
    const val SCANLINES = """
// Scanlines effect
fn scanlines(uv: vec2<f32>, density: f32, opacity: f32) -> f32 {
    return 1.0 - opacity * (1.0 - abs(sin(uv.y * density * 3.14159)));
}
"""
}
