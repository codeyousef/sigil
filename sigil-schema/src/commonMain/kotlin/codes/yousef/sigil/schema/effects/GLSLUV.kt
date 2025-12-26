package codes.yousef.sigil.schema.effects

/**
 * UV and coordinate manipulation functions for GLSL shaders.
 */
object GLSLUV {
    /**
     * Center UV coordinates (0,0 at center, aspect corrected).
     */
    const val CENTER = """
// Center UV coordinates with aspect ratio correction
vec2 centerUV(vec2 uv, vec2 resolution) {
    return (uv * 2.0 - 1.0) * vec2(resolution.x / resolution.y, 1.0);
}
"""

    /**
     * Rotate UV coordinates.
     */
    const val ROTATE = """
// Rotate UV coordinates around origin
vec2 rotateUV(vec2 uv, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return vec2(uv.x * c - uv.y * s, uv.x * s + uv.y * c);
}
"""

    /**
     * Scale UV coordinates from center.
     */
    const val SCALE = """
// Scale UV coordinates from center
vec2 scaleUV(vec2 uv, float scale) {
    return (uv - 0.5) * scale + 0.5;
}
"""

    /**
     * Tile UV coordinates.
     */
    const val TILE = """
// Tile UV coordinates
vec2 tileUV(vec2 uv, vec2 tiles) {
    return fract(uv * tiles);
}
"""
}
