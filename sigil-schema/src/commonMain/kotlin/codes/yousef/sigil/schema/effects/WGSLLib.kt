package codes.yousef.sigil.schema.effects

/**
 * Library of reusable WGSL shader functions for screen-space effects.
 * 
 * These can be composed together to create complex effects by concatenating
 * the string constants before your main shader code.
 */
object WGSLLib {
    
    /**
     * Noise functions for procedural generation.
     */
    object Noise {
        /**
         * 2D Simplex noise function.
         * Returns values in range [-1, 1].
         */
        const val SIMPLEX_2D = """
// 2D Simplex Noise
fn mod289_2(x: vec2<f32>) -> vec2<f32> {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

fn mod289_3(x: vec3<f32>) -> vec3<f32> {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

fn permute(x: vec3<f32>) -> vec3<f32> {
    return mod289_3(((x * 34.0) + 1.0) * x);
}

fn simplex2D(v: vec2<f32>) -> f32 {
    let C = vec4<f32>(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
    var i = floor(v + dot(v, C.yy));
    let x0 = v - i + dot(i, C.xx);
    var i1: vec2<f32>;
    if (x0.x > x0.y) {
        i1 = vec2<f32>(1.0, 0.0);
    } else {
        i1 = vec2<f32>(0.0, 1.0);
    }
    var x12 = x0.xyxy + C.xxzz;
    x12 = vec4<f32>(x12.xy - i1, x12.zw);
    i = mod289_2(i);
    let p = permute(permute(i.y + vec3<f32>(0.0, i1.y, 1.0)) + i.x + vec3<f32>(0.0, i1.x, 1.0));
    var m = max(vec3<f32>(0.5) - vec3<f32>(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), vec3<f32>(0.0));
    m = m * m;
    m = m * m;
    let x = 2.0 * fract(p * C.www) - 1.0;
    let h = abs(x) - 0.5;
    let ox = floor(x + 0.5);
    let a0 = x - ox;
    m = m * (1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h));
    let g = vec3<f32>(a0.x * x0.x + h.x * x0.y, a0.yz * x12.xz + h.yz * x12.yw);
    return 130.0 * dot(m, g);
}
"""

        /**
         * Fractional Brownian Motion (FBM) for layered noise.
         * Requires SIMPLEX_2D to be included first.
         */
        const val FBM = """
// Fractional Brownian Motion
fn fbm(p: vec2<f32>, octaves: i32) -> f32 {
    var value = 0.0;
    var amplitude = 0.5;
    var frequency = 1.0;
    var pos = p;
    
    for (var i = 0; i < octaves; i = i + 1) {
        value = value + amplitude * simplex2D(pos * frequency);
        amplitude = amplitude * 0.5;
        frequency = frequency * 2.0;
    }
    
    return value;
}
"""

        /**
         * Hash function for random values.
         */
        const val HASH = """
// Hash functions
fn hash21(p: vec2<f32>) -> f32 {
    var p3 = fract(vec3<f32>(p.x, p.y, p.x) * 0.1031);
    p3 = p3 + dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

fn hash22(p: vec2<f32>) -> vec2<f32> {
    var p3 = fract(vec3<f32>(p.x, p.y, p.x) * vec3<f32>(0.1031, 0.1030, 0.0973));
    p3 = p3 + dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}
"""
    }
    
    /**
     * Color manipulation functions.
     */
    object Color {
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
    
    /**
     * Signed Distance Functions for shapes.
     */
    object SDF {
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
    
    /**
     * UV and coordinate manipulation.
     */
    object UV {
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
    
    /**
     * Common effect utilities.
     */
    object Effects {
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
    
    /**
     * Standard struct definitions for effects.
     */
    object Structs {
        /**
         * Standard uniforms struct for effects.
         */
        const val EFFECT_UNIFORMS = """
struct EffectUniforms {
    time: f32,
    deltaTime: f32,
    resolution: vec2<f32>,
    mouse: vec2<f32>,
    scroll: f32,
    _padding: f32,
}

@group(0) @binding(0)
var<uniform> uniforms: EffectUniforms;
"""

        /**
         * Standard vertex output struct.
         */
        const val VERTEX_OUTPUT = """
struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
}
"""
    }
}
