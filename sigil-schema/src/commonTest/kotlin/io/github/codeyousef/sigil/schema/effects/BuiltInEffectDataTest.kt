package io.github.codeyousef.sigil.schema.effects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import io.github.codeyousef.sigil.schema.SigilJson

/**
 * TDD tests for built-in effect data types.
 */
class BuiltInEffectDataTest {

    private val json = SigilJson

    @Test
    fun gradientNoiseEffectData_shouldHaveDefaults() {
        val effect = GradientNoiseEffectData(
            id = "gradient-1",
            colors = listOf(0xFF1A1A2E.toInt(), 0xFF16213E.toInt())
        )

        assertEquals("gradient-1", effect.id)
        assertEquals(2, effect.colors.size)
        assertEquals(1.0f, effect.noiseScale)
        assertEquals(1.0f, effect.animationSpeed)
        assertEquals(BlendMode.NORMAL, effect.blendMode)
        assertTrue(effect.enabled)
    }

    @Test
    fun gradientNoiseEffectData_shouldSerialize() {
        val effect = GradientNoiseEffectData(
            id = "gradient-1",
            colors = listOf(0xFF1A1A2E.toInt(), 0xFF16213E.toInt()),
            noiseScale = 2.0f,
            animationSpeed = 0.5f
        )

        val serialized = json.encodeToString(effect)
        val deserialized = json.decodeFromString<GradientNoiseEffectData>(serialized)

        assertEquals(effect.id, deserialized.id)
        assertEquals(effect.colors, deserialized.colors)
        assertEquals(effect.noiseScale, deserialized.noiseScale)
        assertEquals(effect.animationSpeed, deserialized.animationSpeed)
    }

    @Test
    fun filmGrainEffectData_shouldHaveDefaults() {
        val effect = FilmGrainEffectData(id = "grain-1")

        assertEquals(0.05f, effect.intensity)
        assertTrue(effect.animated)
    }

    @Test
    fun vignetteEffectData_shouldHaveDefaults() {
        val effect = VignetteEffectData(id = "vignette-1")

        assertEquals(0.3f, effect.intensity)
        assertEquals(0.5f, effect.smoothness)
        assertEquals(0xFF000000.toInt(), effect.color)
    }

    @Test
    fun chromaticAberrationEffectData_shouldHaveDefaults() {
        val effect = ChromaticAberrationEffectData(id = "chromatic-1")

        assertEquals(0.01f, effect.intensity)
        assertTrue(effect.radial)
    }

    @Test
    fun bloomEffectData_shouldHaveDefaults() {
        val effect = BloomEffectData(id = "bloom-1")

        assertEquals(0.8f, effect.threshold)
        assertEquals(1.0f, effect.intensity)
        assertEquals(1.0f, effect.radius)
    }

    @Test
    fun scanlinesEffectData_shouldHaveDefaults() {
        val effect = ScanlinesEffectData(id = "scanlines-1")

        assertEquals(1.0f, effect.density)
        assertEquals(0.1f, effect.lineOpacity)
    }

    @Test
    fun colorGradingEffectData_shouldHaveDefaults() {
        val effect = ColorGradingEffectData(id = "color-grading-1")

        assertEquals(1.0f, effect.saturation)
        assertEquals(1.0f, effect.contrast)
        assertEquals(1.0f, effect.brightness)
        assertEquals(0.0f, effect.temperature)
    }
}
