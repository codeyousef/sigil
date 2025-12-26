package codes.yousef.sigil.schema.effects

/**
 * Signed Distance Functions for 2D shapes in GLSL.
 */
object GLSLSDF {
    /**
     * Circle SDF.
     */
    const val CIRCLE = """
// Circle SDF
float sdCircle(vec2 p, float r) {
    return length(p) - r;
}
"""

    /**
     * Box SDF.
     */
    const val BOX = """
// Box SDF
float sdBox(vec2 p, vec2 b) {
    vec2 d = abs(p) - b;
    return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);
}
"""

    /**
     * Rounded box SDF.
     */
    const val ROUNDED_BOX = """
// Rounded Box SDF
float sdRoundedBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}
"""

    /**
     * Line segment SDF.
     */
    const val LINE = """
// Line Segment SDF
float sdLine(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a;
    vec2 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}
"""

    /**
     * Triangle SDF.
     */
    const val TRIANGLE = """
// Equilateral Triangle SDF
float sdTriangle(vec2 p, float r) {
    const float k = sqrt(3.0);
    p.x = abs(p.x) - r;
    p.y = p.y + r / k;
    if (p.x + k * p.y > 0.0) p = vec2(p.x - k * p.y, -k * p.x - p.y) / 2.0;
    p.x -= clamp(p.x, -2.0 * r, 0.0);
    return -length(p) * sign(p.y);
}
"""

    /**
     * Ring (annulus) SDF.
     */
    const val RING = """
// Ring (Annulus) SDF
float sdRing(vec2 p, float r, float thickness) {
    return abs(length(p) - r) - thickness;
}
"""

    /**
     * Smooth minimum for blending SDFs.
     */
    const val SMOOTH_MIN = """
// Smooth minimum for SDF blending
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}
"""
}
