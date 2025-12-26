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
        const val PI = GLSLMath.PI
        
        /** Tau (2*PI) constant */
        const val TAU = GLSLMath.TAU
        
        /**
         * Remap a value from one range to another.
         */
        const val REMAP = GLSLMath.REMAP
        
        /**
         * Cubic smoothstep (standard GLSL smoothstep).
         */
        const val SMOOTHSTEP_CUBIC = GLSLMath.SMOOTHSTEP_CUBIC
        
        /**
         * Quintic smoothstep for smoother interpolation.
         */
        const val SMOOTHSTEP_QUINTIC = GLSLMath.SMOOTHSTEP_QUINTIC
        
        /**
         * 2D rotation matrix.
         */
        const val ROTATION_2D = GLSLMath.ROTATION_2D
    }
    
    /**
     * Hash functions for pseudo-random number generation.
     */
    object Hash {
        /**
         * 2D to 1D hash function.
         */
        const val HASH_21 = GLSLNoise.Hash.HASH_21

        /**
         * 2D to 2D hash function.
         */
        const val HASH_22 = GLSLNoise.Hash.HASH_22

        /**
         * 3D to 1D hash function.
         */
        const val HASH_31 = GLSLNoise.Hash.HASH_31

        /**
         * 3D to 3D hash function.
         */
        const val HASH_33 = GLSLNoise.Hash.HASH_33
    }
    
    /**
     * Noise functions for procedural generation.
     */
    object Noise {
        /**
         * 2D Value noise.
         * Returns values in range [0, 1].
         */
        const val VALUE_2D = GLSLNoise.VALUE_2D

        /**
         * 2D Perlin noise.
         * Returns values in range [-1, 1].
         */
        const val PERLIN_2D = GLSLNoise.PERLIN_2D

        /**
         * 2D Simplex noise.
         * Returns values in range [-1, 1].
         */
        const val SIMPLEX_2D = GLSLNoise.SIMPLEX_2D

        /**
         * 2D Worley (cellular) noise.
         * Returns distance to nearest point.
         */
        const val WORLEY_2D = GLSLNoise.WORLEY_2D
    }
    
    /**
     * Fractal noise functions for multi-octave noise.
     */
    object Fractal {
        /**
         * Fractional Brownian Motion (FBM).
         * Requires a noise function to be included first.
         */
        const val FBM = GLSLNoise.Fractal.FBM

        /**
         * Turbulence function (absolute value FBM).
         */
        const val TURBULENCE = GLSLNoise.Fractal.TURBULENCE

        /**
         * Ridged multi-fractal noise.
         */
        const val RIDGED = GLSLNoise.Fractal.RIDGED
    }
    
    /**
     * Color manipulation functions.
     */
    object Color {
        /**
         * Cosine color palette (Inigo Quilez technique).
         */
        const val COSINE_PALETTE = GLSLColor.COSINE_PALETTE

        /**
         * HSV to RGB conversion.
         */
        const val HSV_TO_RGB = GLSLColor.HSV_TO_RGB

        /**
         * RGB to HSV conversion.
         */
        const val RGB_TO_HSV = GLSLColor.RGB_TO_HSV

        /**
         * HSL to RGB conversion.
         */
        const val HSL_TO_RGB = GLSLColor.HSL_TO_RGB

        /**
         * sRGB to linear color conversion.
         */
        const val SRGB_TO_LINEAR = GLSLColor.SRGB_TO_LINEAR

        /**
         * Linear to sRGB color conversion.
         */
        const val LINEAR_TO_SRGB = GLSLColor.LINEAR_TO_SRGB

        /**
         * RGB to grayscale (luminance).
         */
        const val GRAYSCALE = GLSLColor.GRAYSCALE

        /**
         * Linear color interpolation.
         */
        const val LERP_COLOR = GLSLColor.LERP_COLOR
    }
    
    /**
     * Signed Distance Functions for 2D shapes.
     */
    object SDF {
        /**
         * Circle SDF.
         */
        const val CIRCLE = GLSLSDF.CIRCLE

        /**
         * Box SDF.
         */
        const val BOX = GLSLSDF.BOX

        /**
         * Rounded box SDF.
         */
        const val ROUNDED_BOX = GLSLSDF.ROUNDED_BOX

        /**
         * Line segment SDF.
         */
        const val LINE = GLSLSDF.LINE

        /**
         * Triangle SDF.
         */
        const val TRIANGLE = GLSLSDF.TRIANGLE

        /**
         * Ring (annulus) SDF.
         */
        const val RING = GLSLSDF.RING

        /**
         * Smooth minimum for blending SDFs.
         */
        const val SMOOTH_MIN = GLSLSDF.SMOOTH_MIN
    }
    
    /**
     * UV and coordinate manipulation.
     */
    object UV {
        /**
         * Center UV coordinates (0,0 at center, aspect corrected).
         */
        const val CENTER = GLSLUV.CENTER

        /**
         * Rotate UV coordinates.
         */
        const val ROTATE = GLSLUV.ROTATE

        /**
         * Scale UV coordinates from center.
         */
        const val SCALE = GLSLUV.SCALE

        /**
         * Tile UV coordinates.
         */
        const val TILE = GLSLUV.TILE
    }
    
    /**
     * Post-processing effects.
     */
    object Effects {
        /**
         * Vignette effect.
         */
        const val VIGNETTE = GLSLEffects.VIGNETTE

        /**
         * Film grain effect.
         */
        const val FILM_GRAIN = GLSLEffects.FILM_GRAIN

        /**
         * Chromatic aberration effect.
         */
        const val CHROMATIC_ABERRATION = GLSLEffects.CHROMATIC_ABERRATION

        /**
         * Scanlines effect.
         */
        const val SCANLINES = GLSLEffects.SCANLINES

        /**
         * CRT curvature distortion.
         */
        const val CRT_CURVATURE = GLSLEffects.CRT_CURVATURE

        /**
         * Barrel distortion.
         */
        const val BARREL_DISTORTION = GLSLEffects.BARREL_DISTORTION
    }
    
    /**
     * Standard struct and uniform declarations.
     */
    object Uniforms {
        /**
         * Standard varying declaration for UV coordinates.
         */
        const val VARYING_UV = GLSLUniforms.VARYING_UV

        /**
         * Standard uniform declarations for effects.
         */
        const val STANDARD_UNIFORMS = GLSLUniforms.STANDARD_UNIFORMS

        /**
         * Standard sampler for input texture (for multi-pass effects).
         */
        const val INPUT_TEXTURE = GLSLUniforms.INPUT_TEXTURE
    }
    
    /**
     * Preset shader fragments combining common functionality.
     */
    object Presets {
        /**
         * Standard fragment shader header with uniforms and varying.
         */
        const val FRAGMENT_HEADER = GLSLPresets.FRAGMENT_HEADER

        /**
         * Standard fragment shader header with all standard uniforms.
         */
        const val FRAGMENT_HEADER_WITH_UNIFORMS = GLSLPresets.FRAGMENT_HEADER_WITH_UNIFORMS

        /**
         * Simple gradient shader preset.
         */
        const val SIMPLE_GRADIENT = GLSLPresets.SIMPLE_GRADIENT

        /**
         * Animated noise shader preset.
         */
        const val ANIMATED_NOISE = GLSLPresets.ANIMATED_NOISE
    }
}
