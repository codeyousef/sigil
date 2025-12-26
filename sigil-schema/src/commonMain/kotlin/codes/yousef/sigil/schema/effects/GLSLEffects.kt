package codes.yousef.sigil.schema.effects

/**
 * Post-processing effects for GLSL shaders.
 */
object GLSLEffects {
    /**
     * Vignette effect.
     */
    const val VIGNETTE = """
// Vignette effect
float vignette(vec2 uv, float intensity, float smoothness) {
    vec2 centered = uv * 2.0 - 1.0;
    float dist = length(centered);
    return 1.0 - smoothstep(1.0 - smoothness - intensity, 1.0 - smoothness, dist);
}
"""

    /**
     * Film grain effect.
     */
    const val FILM_GRAIN = """
// Film grain effect
float filmGrain(vec2 uv, float time, float intensity) {
    float noise = fract(sin(dot(uv + fract(time), vec2(12.9898, 78.233))) * 43758.5453);
    return (noise - 0.5) * intensity;
}
"""

    /**
     * Chromatic aberration effect.
     */
    const val CHROMATIC_ABERRATION = """
// Chromatic aberration (use with texture sampling)
vec3 chromaticAberration(sampler2D tex, vec2 uv, float amount) {
    vec2 offset = (uv - 0.5) * amount;
    float r = texture2D(tex, uv + offset).r;
    float g = texture2D(tex, uv).g;
    float b = texture2D(tex, uv - offset).b;
    return vec3(r, g, b);
}
"""

    /**
     * Scanlines effect.
     */
    const val SCANLINES = """
// Scanlines effect
float scanlines(vec2 uv, float density, float opacity) {
    return 1.0 - opacity * (1.0 - abs(sin(uv.y * density * 3.14159)));
}
"""

    /**
     * CRT curvature distortion.
     */
    const val CRT_CURVATURE = """
// CRT screen curvature
vec2 crtCurvature(vec2 uv, float curvature) {
    vec2 centered = uv * 2.0 - 1.0;
    vec2 offset = centered.yx / curvature;
    centered += centered * offset * offset;
    return centered * 0.5 + 0.5;
}
"""

    /**
     * Barrel distortion.
     */
    const val BARREL_DISTORTION = """
// Barrel distortion
vec2 barrelDistortion(vec2 uv, float amount) {
    vec2 centered = uv - 0.5;
    float r2 = dot(centered, centered);
    return uv + centered * r2 * amount;
}
"""
}
