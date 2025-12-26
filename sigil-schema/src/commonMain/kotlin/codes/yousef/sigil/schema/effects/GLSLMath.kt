package codes.yousef.sigil.schema.effects

/**
 * Mathematical constants and functions for GLSL shaders.
 */
object GLSLMath {
    /** Pi constant */
    const val PI = """
#define PI 3.14159265359
"""
    
    /** Tau (2*PI) constant */
    const val TAU = """
#define TAU 6.28318530718
"""
    
    /**
     * Remap a value from one range to another.
     */
    const val REMAP = """
// Remap value from one range to another
float remap(float value, float inMin, float inMax, float outMin, float outMax) {
    return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
}
"""
    
    /**
     * Cubic smoothstep (standard GLSL smoothstep).
     */
    const val SMOOTHSTEP_CUBIC = """
// Cubic smoothstep (same as GLSL smoothstep)
float smoothstepCubic(float edge0, float edge1, float x) {
    return smoothstep(edge0, edge1, x);
}
"""
    
    /**
     * Quintic smoothstep for smoother interpolation.
     */
    const val SMOOTHSTEP_QUINTIC = """
// Quintic smoothstep for smoother interpolation
float smoothstepQuintic(float edge0, float edge1, float x) {
    float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
}
"""
    
    /**
     * 2D rotation matrix.
     */
    const val ROTATION_2D = """
// 2D rotation matrix
mat2 rotation2D(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, -s, s, c);
}
"""
}
