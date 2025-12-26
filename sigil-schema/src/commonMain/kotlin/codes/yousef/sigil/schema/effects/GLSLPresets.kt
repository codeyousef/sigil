package codes.yousef.sigil.schema.effects

/**
 * Preset shader fragments combining common functionality for GLSL.
 */
object GLSLPresets {
    /**
     * Standard fragment shader header with uniforms and varying.
     */
    const val FRAGMENT_HEADER = """
precision highp float;
varying vec2 vUv;
"""

    /**
     * Standard fragment shader header with all standard uniforms.
     */
    const val FRAGMENT_HEADER_WITH_UNIFORMS = """
precision highp float;
varying vec2 vUv;
uniform float time;
uniform vec2 resolution;
uniform vec2 mouse;
uniform float mouseDown;
"""

    /**
     * Simple gradient shader preset.
     */
    const val SIMPLE_GRADIENT = """
precision highp float;
varying vec2 vUv;
uniform vec2 resolution;

void main() {
    vec2 uv = vUv;
    vec3 color = vec3(uv.x, uv.y, 0.5);
    gl_FragColor = vec4(color, 1.0);
}
"""

    /**
     * Animated noise shader preset.
     */
    const val ANIMATED_NOISE = """
precision highp float;
varying vec2 vUv;
uniform float time;
uniform vec2 resolution;

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.x, p.y, p.x) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

void main() {
    vec2 uv = vUv * 10.0;
    float n = hash21(floor(uv) + floor(time * 5.0));
    gl_FragColor = vec4(vec3(n), 1.0);
}
"""
}
