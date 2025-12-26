package codes.yousef.sigil.schema.effects

/**
 * Color manipulation functions for WGSL shaders.
 */
object WGSLColor {
    /**
     * Convert HSL to RGB color space.
     */
    const val HSL_TO_RGB = """
// HSL to RGB conversion
fn hslToRgb(hsl: vec3<f32>) -> vec3<f32> {
    let h = hsl.x;
    let s = hsl.y;
    let l = hsl.z;
    
    let c = (1.0 - abs(2.0 * l - 1.0)) * s;
    let x = c * (1.0 - abs(((h * 6.0) % 2.0) - 1.0));
    let m = l - c / 2.0;
    
    var rgb: vec3<f32>;
    let h6 = h * 6.0;
    
    if (h6 < 1.0) {
        rgb = vec3<f32>(c, x, 0.0);
    } else if (h6 < 2.0) {
        rgb = vec3<f32>(x, c, 0.0);
    } else if (h6 < 3.0) {
        rgb = vec3<f32>(0.0, c, x);
    } else if (h6 < 4.0) {
        rgb = vec3<f32>(0.0, x, c);
    } else if (h6 < 5.0) {
        rgb = vec3<f32>(x, 0.0, c);
    } else {
        rgb = vec3<f32>(c, 0.0, x);
    }
    
    return rgb + m;
}
"""

    /**
     * Cosine palette for smooth color gradients.
     * Based on Inigo Quilez's technique.
     * 
     * Usage: cosinePalette(t, a, b, c, d)
     * where a, b, c, d are vec3 color parameters
     */
    const val COSINE_PALETTE = """
// Cosine color palette (Inigo Quilez)
fn cosinePalette(t: f32, a: vec3<f32>, b: vec3<f32>, c: vec3<f32>, d: vec3<f32>) -> vec3<f32> {
    return a + b * cos(6.28318 * (c * t + d));
}
"""

    /**
     * Linear interpolation between colors.
     */
    const val LERP_COLOR = """
// Linear color interpolation
fn lerpColor(a: vec3<f32>, b: vec3<f32>, t: f32) -> vec3<f32> {
    return a + (b - a) * clamp(t, 0.0, 1.0);
}
"""

    /**
     * RGB to grayscale conversion.
     */
    const val GRAYSCALE = """
// RGB to grayscale
fn grayscale(rgb: vec3<f32>) -> f32 {
    return dot(rgb, vec3<f32>(0.299, 0.587, 0.114));
}
"""
}
