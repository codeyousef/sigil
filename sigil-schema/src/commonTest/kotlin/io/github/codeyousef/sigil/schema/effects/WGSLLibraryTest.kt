package io.github.codeyousef.sigil.schema.effects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import io.github.codeyousef.sigil.schema.SigilJson

/**
 * TDD tests for WGSL shader library.
 */
class WGSLLibraryTest {

    @Test
    fun wgslLib_noise_simplex2D_shouldContainValidWGSL() {
        val shader = WGSLLib.Noise.SIMPLEX_2D
        
        assertTrue(shader.contains("fn simplex2D"))
        assertTrue(shader.contains("vec2<f32>"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun wgslLib_noise_fbm_shouldContainValidWGSL() {
        val shader = WGSLLib.Noise.FBM
        
        assertTrue(shader.contains("fn fbm"))
        assertTrue(shader.contains("octaves"))
    }

    @Test
    fun wgslLib_color_hslToRgb_shouldContainValidWGSL() {
        val shader = WGSLLib.Color.HSL_TO_RGB
        
        assertTrue(shader.contains("fn hslToRgb"))
        assertTrue(shader.contains("vec3<f32>"))
    }

    @Test
    fun wgslLib_color_cosinePalette_shouldContainValidWGSL() {
        val shader = WGSLLib.Color.COSINE_PALETTE
        
        assertTrue(shader.contains("fn cosinePalette"))
        assertTrue(shader.contains("cos"))
    }

    @Test
    fun wgslLib_sdf_circle_shouldContainValidWGSL() {
        val shader = WGSLLib.SDF.CIRCLE
        
        assertTrue(shader.contains("fn sdCircle"))
    }

    @Test
    fun wgslLib_canCombineShaderFragments() {
        val combined = buildString {
            append(WGSLLib.Noise.SIMPLEX_2D)
            append("\n")
            append(WGSLLib.Color.HSL_TO_RGB)
        }
        
        assertTrue(combined.contains("simplex2D"))
        assertTrue(combined.contains("hslToRgb"))
    }
}
