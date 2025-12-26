package codes.yousef.sigil.schema.effects

/**
 * Noise and Hash functions for GLSL shaders.
 */
object GLSLNoise {
    
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
}
