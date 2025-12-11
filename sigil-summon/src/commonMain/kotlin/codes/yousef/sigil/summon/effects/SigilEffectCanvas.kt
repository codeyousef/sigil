package codes.yousef.sigil.summon.effects

import codes.yousef.summon.annotation.Composable
import codes.yousef.sigil.schema.effects.SigilCanvasConfig
import codes.yousef.sigil.schema.effects.InteractionConfig
import codes.yousef.sigil.schema.effects.ShaderEffectData

/**
 * Summon composable that renders a canvas for screen-space effects.
 *
 * Platform implementations:
 * - JVM (Server): Serializes effect data to JSON embedded in HTML
 * - JS (Client): Hydrates and creates actual WebGPU shader passes
 *
 * @param id Unique ID for the canvas element
 * @param width CSS width of the canvas
 * @param height CSS height of the canvas
 * @param config Canvas configuration options
 * @param interactions Interaction configuration
 * @param fallback Composable to render if WebGPU is unavailable
 * @param content Composable lambda containing effect definitions
 */
@Composable
expect fun SigilEffectCanvas(
    id: String = "sigil-effect-canvas",
    width: String = "100%",
    height: String = "100%",
    config: SigilCanvasConfig = SigilCanvasConfig(),
    interactions: InteractionConfig = InteractionConfig(),
    fallback: @Composable () -> String = { "" },
    content: @Composable () -> String
): String

/**
 * Register a shader effect within a SigilEffectCanvas.
 *
 * @param effect The effect data to register
 */
@Composable
fun SigilEffect(effect: ShaderEffectData): String {
    val context = EffectSummonContext.current()
    context.registerEffect(effect)
    return ""
}

/**
 * Convenience function to create and register a custom shader effect.
 *
 * @param id Unique identifier for the effect
 * @param fragmentShader WGSL fragment shader source code
 * @param name Optional human-readable name
 * @param timeScale Time multiplier for animations
 * @param enableMouseInteraction Whether to track mouse position
 */
@Composable
fun CustomShaderEffect(
    id: String,
    fragmentShader: String,
    name: String? = null,
    timeScale: Float = 1f,
    enableMouseInteraction: Boolean = false,
    uniforms: Map<String, codes.yousef.sigil.schema.effects.UniformValue> = emptyMap()
): String {
    return SigilEffect(
        ShaderEffectData(
            id = id,
            name = name,
            fragmentShader = fragmentShader,
            timeScale = timeScale,
            enableMouseInteraction = enableMouseInteraction,
            uniforms = uniforms
        )
    )
}
