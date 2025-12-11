package codes.yousef.sigil.schema.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class for animated gradient background with noise effect.
 */
@Serializable
@SerialName("gradientNoiseEffect")
data class GradientNoiseEffectData(
    val id: String,
    val name: String? = "Gradient Noise",
    val blendMode: BlendMode = BlendMode.NORMAL,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    
    /** Colors for the gradient (as ARGB ints) */
    val colors: List<Int>,
    /** Scale of the noise pattern */
    val noiseScale: Float = 1.0f,
    /** Speed of animation */
    val animationSpeed: Float = 1.0f
)

/**
 * Data class for film grain overlay effect.
 */
@Serializable
@SerialName("filmGrainEffect")
data class FilmGrainEffectData(
    val id: String,
    val name: String? = "Film Grain",
    val blendMode: BlendMode = BlendMode.OVERLAY,
    val opacity: Float = 0.05f,
    val enabled: Boolean = true,
    
    /** Intensity of the grain (0-1) */
    val intensity: Float = 0.05f,
    /** Whether the grain animates over time */
    val animated: Boolean = true
)

/**
 * Data class for vignette darkening at edges effect.
 */
@Serializable
@SerialName("vignetteEffect")
data class VignetteEffectData(
    val id: String,
    val name: String? = "Vignette",
    val blendMode: BlendMode = BlendMode.MULTIPLY,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    
    /** Intensity of the vignette darkening (0-1) */
    val intensity: Float = 0.3f,
    /** Smoothness of the vignette edge (0-1) */
    val smoothness: Float = 0.5f,
    /** Color of the vignette (ARGB) */
    val color: Int = 0xFF000000.toInt()
)

/**
 * Data class for chromatic aberration effect.
 */
@Serializable
@SerialName("chromaticAberrationEffect")
data class ChromaticAberrationEffectData(
    val id: String,
    val name: String? = "Chromatic Aberration",
    val blendMode: BlendMode = BlendMode.NORMAL,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    
    /** Intensity of the color separation */
    val intensity: Float = 0.01f,
    /** Whether aberration radiates from center */
    val radial: Boolean = true
)

/**
 * Data class for bloom/glow effect.
 */
@Serializable
@SerialName("bloomEffect")
data class BloomEffectData(
    val id: String,
    val name: String? = "Bloom",
    val blendMode: BlendMode = BlendMode.ADDITIVE,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    
    /** Brightness threshold for bloom (0-1) */
    val threshold: Float = 0.8f,
    /** Intensity of the bloom glow */
    val intensity: Float = 1.0f,
    /** Radius of the bloom blur */
    val radius: Float = 1.0f
)

/**
 * Data class for CRT scanlines effect.
 */
@Serializable
@SerialName("scanlinesEffect")
data class ScanlinesEffectData(
    val id: String,
    val name: String? = "Scanlines",
    val blendMode: BlendMode = BlendMode.MULTIPLY,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    
    /** Density of scanlines */
    val density: Float = 1.0f,
    /** Opacity of individual scanlines (0-1) */
    val lineOpacity: Float = 0.1f
)

/**
 * Data class for color grading/LUT effect.
 */
@Serializable
@SerialName("colorGradingEffect")
data class ColorGradingEffectData(
    val id: String,
    val name: String? = "Color Grading",
    val blendMode: BlendMode = BlendMode.NORMAL,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    
    /** Saturation multiplier (1 = normal) */
    val saturation: Float = 1.0f,
    /** Contrast multiplier (1 = normal) */
    val contrast: Float = 1.0f,
    /** Brightness multiplier (1 = normal) */
    val brightness: Float = 1.0f,
    /** Temperature shift (-1 = cool, +1 = warm) */
    val temperature: Float = 0.0f
)
