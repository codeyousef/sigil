package codes.yousef.sigil.schema.effects

/**
 * Color manipulation functions for GLSL shaders.
 */
object GLSLColor {
    /**
     * Cosine color palette (Inigo Quilez technique).
     */
    const val COSINE_PALETTE = """
// Cosine color palette (Inigo Quilez)
vec3 cosinePalette(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
    return a + b * cos(6.28318 * (c * t + d));
}
"""

    /**
     * HSV to RGB conversion.
     */
    const val HSV_TO_RGB = """
// HSV to RGB conversion
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
"""

    /**
     * RGB to HSV conversion.
     */
    const val RGB_TO_HSV = """
// RGB to HSV conversion
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}
"""

    /**
     * HSL to RGB conversion.
     */
    const val HSL_TO_RGB = """
// HSL to RGB conversion
vec3 hsl2rgb(vec3 hsl) {
    float h = hsl.x;
    float s = hsl.y;
    float l = hsl.z;
    
    float c = (1.0 - abs(2.0 * l - 1.0)) * s;
    float x = c * (1.0 - abs(mod(h * 6.0, 2.0) - 1.0));
    float m = l - c / 2.0;
    
    vec3 rgb;
    float h6 = h * 6.0;
    
    if (h6 < 1.0) rgb = vec3(c, x, 0.0);
    else if (h6 < 2.0) rgb = vec3(x, c, 0.0);
    else if (h6 < 3.0) rgb = vec3(0.0, c, x);
    else if (h6 < 4.0) rgb = vec3(0.0, x, c);
    else if (h6 < 5.0) rgb = vec3(x, 0.0, c);
    else rgb = vec3(c, 0.0, x);
    
    return rgb + m;
}
"""

    /**
     * sRGB to linear color conversion.
     */
    const val SRGB_TO_LINEAR = """
// sRGB to linear conversion
vec3 srgbToLinear(vec3 srgb) {
    return pow(srgb, vec3(2.2));
}
"""

    /**
     * Linear to sRGB color conversion.
     */
    const val LINEAR_TO_SRGB = """
// Linear to sRGB conversion
vec3 linearToSrgb(vec3 linear) {
    return pow(linear, vec3(1.0 / 2.2));
}
"""

    /**
     * RGB to grayscale (luminance).
     */
    const val GRAYSCALE = """
// RGB to grayscale (luminance)
float grayscale(vec3 rgb) {
    return dot(rgb, vec3(0.299, 0.587, 0.114));
}
"""

    /**
     * Linear color interpolation.
     */
    const val LERP_COLOR = """
// Linear color interpolation
vec3 lerpColor(vec3 a, vec3 b, float t) {
    return mix(a, b, clamp(t, 0.0, 1.0));
}
"""
}
