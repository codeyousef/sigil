package codes.yousef.sigil.schema.effects

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/**
 * Tests for GLSL shader library.
 * Validates that all GLSL shader snippets contain valid GLSL syntax.
 */
class GLSLLibraryTest {

    @Test
    fun glslLib_hash21_shouldContainValidGLSL() {
        val shader = GLSLLib.Hash.HASH_21
        
        assertTrue(shader.contains("float hash21"))
        assertTrue(shader.contains("vec2 p"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_hash22_shouldContainValidGLSL() {
        val shader = GLSLLib.Hash.HASH_22
        
        assertTrue(shader.contains("vec2 hash22"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_noise_value2D_shouldContainValidGLSL() {
        val shader = GLSLLib.Noise.VALUE_2D
        
        assertTrue(shader.contains("float valueNoise2D"))
        assertTrue(shader.contains("vec2 p"))
        assertTrue(shader.contains("floor"))
        assertTrue(shader.contains("fract"))
    }

    @Test
    fun glslLib_noise_simplex2D_shouldContainValidGLSL() {
        val shader = GLSLLib.Noise.SIMPLEX_2D
        
        assertTrue(shader.contains("float simplex2D"))
        assertTrue(shader.contains("vec2 v"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_noise_perlin2D_shouldContainValidGLSL() {
        val shader = GLSLLib.Noise.PERLIN_2D
        
        assertTrue(shader.contains("float perlin2D"))
        assertTrue(shader.contains("perlinGrad"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_noise_worley2D_shouldContainValidGLSL() {
        val shader = GLSLLib.Noise.WORLEY_2D
        
        assertTrue(shader.contains("float worley2D"))
        assertTrue(shader.contains("minDist"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_fractal_fbm_shouldContainValidGLSL() {
        val shader = GLSLLib.Fractal.FBM
        
        assertTrue(shader.contains("float fbm"))
        assertTrue(shader.contains("octaves"))
        assertTrue(shader.contains("amplitude"))
        assertTrue(shader.contains("frequency"))
    }

    @Test
    fun glslLib_fractal_turbulence_shouldContainValidGLSL() {
        val shader = GLSLLib.Fractal.TURBULENCE
        
        assertTrue(shader.contains("float turbulence"))
        assertTrue(shader.contains("abs"))
    }

    @Test
    fun glslLib_fractal_ridged_shouldContainValidGLSL() {
        val shader = GLSLLib.Fractal.RIDGED
        
        assertTrue(shader.contains("float ridgedNoise"))
        assertTrue(shader.contains("prev"))
    }

    @Test
    fun glslLib_color_cosinePalette_shouldContainValidGLSL() {
        val shader = GLSLLib.Color.COSINE_PALETTE
        
        assertTrue(shader.contains("vec3 cosinePalette"))
        assertTrue(shader.contains("cos"))
    }

    @Test
    fun glslLib_color_hsvToRgb_shouldContainValidGLSL() {
        val shader = GLSLLib.Color.HSV_TO_RGB
        
        assertTrue(shader.contains("vec3 hsv2rgb"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_color_rgbToHsv_shouldContainValidGLSL() {
        val shader = GLSLLib.Color.RGB_TO_HSV
        
        assertTrue(shader.contains("vec3 rgb2hsv"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_color_hslToRgb_shouldContainValidGLSL() {
        val shader = GLSLLib.Color.HSL_TO_RGB
        
        assertTrue(shader.contains("vec3 hsl2rgb"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_color_grayscale_shouldContainValidGLSL() {
        val shader = GLSLLib.Color.GRAYSCALE
        
        assertTrue(shader.contains("float grayscale"))
        assertTrue(shader.contains("dot"))
    }

    @Test
    fun glslLib_sdf_circle_shouldContainValidGLSL() {
        val shader = GLSLLib.SDF.CIRCLE
        
        assertTrue(shader.contains("float sdCircle"))
        assertTrue(shader.contains("length"))
    }

    @Test
    fun glslLib_sdf_box_shouldContainValidGLSL() {
        val shader = GLSLLib.SDF.BOX
        
        assertTrue(shader.contains("float sdBox"))
        assertTrue(shader.contains("abs"))
    }

    @Test
    fun glslLib_sdf_roundedBox_shouldContainValidGLSL() {
        val shader = GLSLLib.SDF.ROUNDED_BOX
        
        assertTrue(shader.contains("float sdRoundedBox"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_sdf_line_shouldContainValidGLSL() {
        val shader = GLSLLib.SDF.LINE
        
        assertTrue(shader.contains("float sdLine"))
        assertTrue(shader.contains("clamp"))
    }

    @Test
    fun glslLib_sdf_smoothMin_shouldContainValidGLSL() {
        val shader = GLSLLib.SDF.SMOOTH_MIN
        
        assertTrue(shader.contains("float smin"))
        assertTrue(shader.contains("mix"))
    }

    @Test
    fun glslLib_uv_center_shouldContainValidGLSL() {
        val shader = GLSLLib.UV.CENTER
        
        assertTrue(shader.contains("vec2 centerUV"))
        assertTrue(shader.contains("resolution"))
    }

    @Test
    fun glslLib_uv_rotate_shouldContainValidGLSL() {
        val shader = GLSLLib.UV.ROTATE
        
        assertTrue(shader.contains("vec2 rotateUV"))
        assertTrue(shader.contains("cos"))
        assertTrue(shader.contains("sin"))
    }

    @Test
    fun glslLib_effects_vignette_shouldContainValidGLSL() {
        val shader = GLSLLib.Effects.VIGNETTE
        
        assertTrue(shader.contains("float vignette"))
        assertTrue(shader.contains("smoothstep"))
    }

    @Test
    fun glslLib_effects_filmGrain_shouldContainValidGLSL() {
        val shader = GLSLLib.Effects.FILM_GRAIN
        
        assertTrue(shader.contains("float filmGrain"))
        assertTrue(shader.contains("time"))
        assertTrue(shader.contains("intensity"))
    }

    @Test
    fun glslLib_effects_scanlines_shouldContainValidGLSL() {
        val shader = GLSLLib.Effects.SCANLINES
        
        assertTrue(shader.contains("float scanlines"))
        assertTrue(shader.contains("sin"))
    }

    @Test
    fun glslLib_effects_chromaticAberration_shouldContainValidGLSL() {
        val shader = GLSLLib.Effects.CHROMATIC_ABERRATION
        
        assertTrue(shader.contains("vec3 chromaticAberration"))
        assertTrue(shader.contains("texture2D"))
    }

    @Test
    fun glslLib_effects_crtCurvature_shouldContainValidGLSL() {
        val shader = GLSLLib.Effects.CRT_CURVATURE
        
        assertTrue(shader.contains("vec2 crtCurvature"))
        assertTrue(shader.contains("curvature"))
    }

    @Test
    fun glslLib_math_rotation2D_shouldContainValidGLSL() {
        val shader = GLSLLib.Math.ROTATION_2D
        
        assertTrue(shader.contains("mat2 rotation2D"))
        assertTrue(shader.contains("cos"))
        assertTrue(shader.contains("sin"))
    }

    @Test
    fun glslLib_math_remap_shouldContainValidGLSL() {
        val shader = GLSLLib.Math.REMAP
        
        assertTrue(shader.contains("float remap"))
        assertTrue(shader.contains("return"))
    }

    @Test
    fun glslLib_uniforms_standardUniforms_shouldContainValidGLSL() {
        val shader = GLSLLib.Uniforms.STANDARD_UNIFORMS
        
        assertTrue(shader.contains("uniform float time"))
        assertTrue(shader.contains("uniform vec2 resolution"))
        assertTrue(shader.contains("uniform vec2 mouse"))
    }

    @Test
    fun glslLib_presets_fragmentHeader_shouldContainValidGLSL() {
        val shader = GLSLLib.Presets.FRAGMENT_HEADER
        
        assertTrue(shader.contains("precision highp float"))
        assertTrue(shader.contains("varying vec2 vUv"))
    }

    @Test
    fun glslLib_presets_fragmentHeaderWithUniforms_shouldContainAllUniforms() {
        val shader = GLSLLib.Presets.FRAGMENT_HEADER_WITH_UNIFORMS
        
        assertTrue(shader.contains("precision highp float"))
        assertTrue(shader.contains("uniform float time"))
        assertTrue(shader.contains("uniform vec2 resolution"))
        assertTrue(shader.contains("uniform vec2 mouse"))
    }

    @Test
    fun glslLib_presets_simpleGradient_shouldBeCompleteShader() {
        val shader = GLSLLib.Presets.SIMPLE_GRADIENT
        
        assertTrue(shader.contains("void main()"))
        assertTrue(shader.contains("gl_FragColor"))
    }

    @Test
    fun glslLib_presets_animatedNoise_shouldBeCompleteShader() {
        val shader = GLSLLib.Presets.ANIMATED_NOISE
        
        assertTrue(shader.contains("void main()"))
        assertTrue(shader.contains("gl_FragColor"))
        assertTrue(shader.contains("hash21"))
    }

    @Test
    fun glslLib_composability_shouldAllowCombiningSnippets() {
        // Test that shader snippets can be composed together
        val composedShader = """
            ${GLSLLib.Presets.FRAGMENT_HEADER_WITH_UNIFORMS}
            ${GLSLLib.Hash.HASH_21}
            ${GLSLLib.Noise.VALUE_2D}
            ${GLSLLib.Fractal.FBM}
            ${GLSLLib.Color.COSINE_PALETTE}
            
            void main() {
                vec2 uv = vUv;
                float n = valueNoise2D(uv * 10.0);
                vec3 color = cosinePalette(n, vec3(0.5), vec3(0.5), vec3(1.0), vec3(0.0));
                gl_FragColor = vec4(color, 1.0);
            }
        """
        
        assertTrue(composedShader.contains("precision highp float"))
        assertTrue(composedShader.contains("float hash21"))
        assertTrue(composedShader.contains("float valueNoise2D"))
        assertTrue(composedShader.contains("vec3 cosinePalette"))
        assertTrue(composedShader.contains("void main()"))
    }
}
