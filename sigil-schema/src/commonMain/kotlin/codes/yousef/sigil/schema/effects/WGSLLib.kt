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
        const val SIMPLEX_2D = WGSLNoise.SIMPLEX_2D

        /**
         * Fractional Brownian Motion (FBM) for layered noise.
         * Requires SIMPLEX_2D to be included first.
         */
        const val FBM = WGSLNoise.FBM

        /**
         * Hash function for random values.
         */
        const val HASH = WGSLNoise.HASH
    }
    
    /**
     * Color manipulation functions.
     */
    object Color {
        /**
         * Convert HSL to RGB color space.
         */
        const val HSL_TO_RGB = WGSLColor.HSL_TO_RGB

        /**
         * Cosine palette for smooth color gradients.
         * Based on Inigo Quilez's technique.
         * 
         * Usage: cosinePalette(t, a, b, c, d)
         * where a, b, c, d are vec3 color parameters
         */
        const val COSINE_PALETTE = WGSLColor.COSINE_PALETTE

        /**
         * Linear interpolation between colors.
         */
        const val LERP_COLOR = WGSLColor.LERP_COLOR

        /**
         * RGB to grayscale conversion.
         */
        const val GRAYSCALE = WGSLColor.GRAYSCALE
    }
    
    /**
     * Signed Distance Functions for shapes.
     */
    object SDF {
        /**
         * SDF for a circle.
         */
        const val CIRCLE = WGSLSDF.CIRCLE

        /**
         * SDF for a box.
         */
        const val BOX = WGSLSDF.BOX

        /**
         * SDF for a rounded box.
         */
        const val ROUNDED_BOX = WGSLSDF.ROUNDED_BOX

        /**
         * Smooth minimum for combining SDFs.
         */
        const val SMOOTH_MIN = WGSLSDF.SMOOTH_MIN
    }
    
    /**
     * UV and coordinate manipulation.
     */
    object UV {
        /**
         * Center UV coordinates (0,0 at center).
         */
        const val CENTER = WGSLUV.CENTER

        /**
         * Rotate UV coordinates.
         */
        const val ROTATE = WGSLUV.ROTATE

        /**
         * Scale UV coordinates from center.
         */
        const val SCALE = WGSLUV.SCALE
    }
    
    /**
     * Common effect utilities.
     */
    object Effects {
        /**
         * Vignette effect function.
         */
        const val VIGNETTE = WGSLEffects.VIGNETTE

        /**
         * Film grain effect function.
         */
        const val FILM_GRAIN = WGSLEffects.FILM_GRAIN

        /**
         * Scanlines effect function.
         */
        const val SCANLINES = WGSLEffects.SCANLINES
    }
    
    /**
     * Standard struct definitions for effects.
     */
    object Structs {
        /**
         * Standard uniforms struct for effects.
         */
        const val EFFECT_UNIFORMS = WGSLStructs.EFFECT_UNIFORMS

        /**
         * Standard vertex output struct.
         */
        const val VERTEX_OUTPUT = WGSLStructs.VERTEX_OUTPUT
    }
}
