package codes.yousef.sigil.schema.effects

/**
 * Library of reusable GLSL shader functions for screen-space effects.
 * 
 * This is the WebGL equivalent of [WGSLLib], providing the same functionality
 * using GLSL syntax for browsers that don't support WebGPU.
 * 
 * These can be composed together to create complex effects by concatenating
 * the string constants before your main shader code.
 */
object GLSLLib {
    
    /**
     * Mathematical constants.
     */
    object Math {
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
    
    /**
     * Hash functions for pseudo-random number generation.
     */
    object Hash {
        /**
         * 2D to 1D hash function.
         */
        const val HASH_21 = """
// Hash 2D -> 1D
float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.x, p.y, p.x) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
"""

        /**
         * 2D to 2D hash function.
         */
        const val HASH_22 = """
// Hash 2D -> 2D
vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.x, p.y, p.x) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}
"""

        /**
         * 3D to 1D hash function.
         */
        const val HASH_31 = """
// Hash 3D -> 1D
float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
"""

        /**
         * 3D to 3D hash function.
         */
        const val HASH_33 = """
// Hash 3D -> 3D
vec3 hash33(vec3 p) {
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.xxy + p.yxx) * p.zyx);
}
"""
    }
    
    /**
     * Noise functions for procedural generation.
     */
    object Noise {
        /**
         * 2D Value noise.
         * Returns values in range [0, 1].
         */
        const val VALUE_2D = """
// 2D Value Noise
float valueNoise2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}
"""

        /**
         * 2D Perlin noise.
         * Returns values in range [-1, 1].
         */
        const val PERLIN_2D = """
// 2D Perlin Noise helper
vec2 perlinGrad(vec2 p) {
    float angle = hash21(p) * 6.28318;
    return vec2(cos(angle), sin(angle));
}

float perlin2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    
    vec2 g00 = perlinGrad(i);
    vec2 g10 = perlinGrad(i + vec2(1.0, 0.0));
    vec2 g01 = perlinGrad(i + vec2(0.0, 1.0));
    vec2 g11 = perlinGrad(i + vec2(1.0, 1.0));
    
    float n00 = dot(g00, f);
    float n10 = dot(g10, f - vec2(1.0, 0.0));
    float n01 = dot(g01, f - vec2(0.0, 1.0));
    float n11 = dot(g11, f - vec2(1.0, 1.0));
    
    return mix(mix(n00, n10, u.x), mix(n01, n11, u.x), u.y);
}
"""

        /**
         * 2D Simplex noise.
         * Returns values in range [-1, 1].
         */
        const val SIMPLEX_2D = """
// 2D Simplex Noise
vec3 mod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec2 mod289_2(vec2 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec3 permute(vec3 x) {
    return mod289(((x * 34.0) + 1.0) * x);
}

float simplex2D(vec2 v) {
    const vec4 C = vec4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
    
    vec2 i = floor(v + dot(v, C.yy));
    vec2 x0 = v - i + dot(i, C.xx);
    
    vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
    
    vec4 x12 = x0.xyxy + C.xxzz;
    x12.xy -= i1;
    
    i = mod289_2(i);
    vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));
    
    vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);
    m = m * m;
    m = m * m;
    
    vec3 x = 2.0 * fract(p * C.www) - 1.0;
    vec3 h = abs(x) - 0.5;
    vec3 ox = floor(x + 0.5);
    vec3 a0 = x - ox;
    
    m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);
    
    vec3 g;
    g.x = a0.x * x0.x + h.x * x0.y;
    g.yz = a0.yz * x12.xz + h.yz * x12.yw;
    
    return 130.0 * dot(m, g);
}
"""

        /**
         * 2D Worley (cellular) noise.
         * Returns distance to nearest point.
         */
        const val WORLEY_2D = """
// 2D Worley (Cellular) Noise
float worley2D(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    
    float minDist = 1.0;
    
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighbor = vec2(float(x), float(y));
            vec2 point = hash22(i + neighbor);
            vec2 diff = neighbor + point - f;
            float dist = length(diff);
            minDist = min(minDist, dist);
        }
    }
    
    return minDist;
}
"""
    }
    
    /**
     * Fractal noise functions for multi-octave noise.
     */
    object Fractal {
        /**
         * Fractional Brownian Motion (FBM).
         * Requires a noise function to be included first.
         */
        const val FBM = """
// Fractional Brownian Motion
float fbm(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    
    for (int i = 0; i < octaves; i++) {
        value += amplitude * simplex2D(p * frequency);
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value;
}
"""

        /**
         * Turbulence function (absolute value FBM).
         */
        const val TURBULENCE = """
// Turbulence (absolute value FBM)
float turbulence(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    
    for (int i = 0; i < octaves; i++) {
        value += amplitude * abs(simplex2D(p * frequency));
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value;
}
"""

        /**
         * Ridged multi-fractal noise.
         */
        const val RIDGED = """
// Ridged multi-fractal noise
float ridgedNoise(vec2 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    float prev = 1.0;
    
    for (int i = 0; i < octaves; i++) {
        float n = 1.0 - abs(simplex2D(p * frequency));
        n = n * n;
        value += n * amplitude * prev;
        prev = n;
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value;
}
"""
    }
    
    /**
     * Color manipulation functions.
     */
    object Color {
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
    
    /**
     * Signed Distance Functions for 2D shapes.
     */
    object SDF {
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
    
    /**
     * UV and coordinate manipulation.
     */
    object UV {
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
    
    /**
     * Post-processing effects.
     */
    object Effects {
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
    
    /**
     * Standard struct and uniform declarations.
     */
    object Uniforms {
        /**
         * Standard varying declaration for UV coordinates.
         */
        const val VARYING_UV = """
varying vec2 vUv;
"""

        /**
         * Standard uniform declarations for effects.
         */
        const val STANDARD_UNIFORMS = """
uniform float time;
uniform float deltaTime;
uniform vec2 resolution;
uniform vec2 mouse;
uniform float mouseDown;
"""

        /**
         * Standard sampler for input texture (for multi-pass effects).
         */
        const val INPUT_TEXTURE = """
uniform sampler2D inputTexture;
"""
    }
    
    /**
     * Preset shader fragments combining common functionality.
     */
    object Presets {
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
}
