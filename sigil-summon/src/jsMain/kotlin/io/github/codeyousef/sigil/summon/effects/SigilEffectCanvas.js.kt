package io.github.codeyousef.sigil.summon.effects

import codes.yousef.summon.annotation.Composable
import io.github.codeyousef.sigil.schema.effects.SigilCanvasConfig
import io.github.codeyousef.sigil.schema.effects.InteractionConfig

/**
 * JS (Client-side) implementation of SigilEffectCanvas.
 *
 * On the client, this component is used for client-side rendering scenarios.
 * For SSR hydration, the SigilEffectHydrator is used instead.
 */
@Composable
actual fun SigilEffectCanvas(
    id: String,
    width: String,
    height: String,
    config: SigilCanvasConfig,
    interactions: InteractionConfig,
    fallback: @Composable () -> String,
    content: @Composable () -> String
): String {
    // Create client-side context
    val context = EffectSummonContext.createClientContext()
    context.configureCanvas(config)
    context.configureInteractions(interactions)

    // Execute content within context
    EffectSummonContext.withContext(context) {
        content()
    }

    // For CSR, we need to trigger hydration after the canvas is rendered
    // Return a canvas element that will be hydrated
    return buildString {
        append("""<div id="$id-container" style="width: $width; height: $height; position: relative;">""")
        append("""<canvas id="$id" style="width: 100%; height: 100%; display: block;"></canvas>""")
        append("</div>")
    }
}
