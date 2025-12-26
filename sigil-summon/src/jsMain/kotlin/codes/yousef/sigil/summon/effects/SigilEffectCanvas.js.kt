package codes.yousef.sigil.summon.effects

import codes.yousef.summon.annotation.Composable
import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.effects.SigilCanvasConfig
import codes.yousef.sigil.schema.effects.InteractionConfig
import codes.yousef.sigil.schema.effects.EffectComposerData
import kotlinx.serialization.encodeToString

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

    // Build the effect composer data
    val composerData = context.buildComposerData(id)

    // Serialize to JSON
    val effectJson = SigilJson.encodeToString(composerData)
    val configJson = SigilJson.encodeToString(config)
    val interactionsJson = SigilJson.encodeToString(interactions)

    // Escape JSON for embedding in HTML attributes
    val escapedEffectJson = escapeJsonForAttribute(effectJson)
    val escapedConfigJson = escapeJsonForAttribute(configJson)
    val escapedInteractionsJson = escapeJsonForAttribute(interactionsJson)

    // For CSR, we need to trigger hydration after the canvas is rendered
    // Return a canvas element that will be hydrated
    return buildString {
        append("""<div id="$id-container" style="width: $width; height: $height; position: relative;">""")
        append("""<canvas """)
        append("""id="$id" """)
        append("""data-sigil-effects='$escapedEffectJson' """)
        append("""data-sigil-config='$escapedConfigJson' """)
        append("""data-sigil-interactions='$escapedInteractionsJson' """)
        append("""style="width: 100%; height: 100%; display: block;">""")
        append("""</canvas>""")
        append("</div>")
    }
}

/**
 * Escape JSON for embedding in a single-quoted HTML attribute.
 * Browser auto-decodes HTML entities when reading dataset properties.
 */
private fun escapeJsonForAttribute(json: String): String {
    return json
        .replace("&", "&amp;")   // Must be first
        .replace("'", "&#39;")   // Escape single quotes for single-quoted attribute
        .replace("<", "&lt;")    // Safety
        .replace(">", "&gt;")    // Safety
}
