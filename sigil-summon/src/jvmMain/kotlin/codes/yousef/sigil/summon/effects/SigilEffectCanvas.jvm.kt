package codes.yousef.sigil.summon.effects

import codes.yousef.summon.annotation.Composable
import codes.yousef.summon.components.foundation.RawHtml
import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.effects.SigilCanvasConfig
import codes.yousef.sigil.schema.effects.InteractionConfig
import codes.yousef.sigil.schema.effects.EffectComposerData
import kotlinx.serialization.encodeToString

/**
 * JVM (Server-side) implementation of SigilEffectCanvas for Summon SSR.
 *
 * This implementation:
 * 1. Creates an EffectSummonContext for the composition
 * 2. Executes the content lambda (which populates the context with effects)
 * 3. Serializes the collected effect data to JSON
 * 4. Renders a canvas element with embedded JSON data for client hydration
 * 5. Includes a noscript fallback for non-WebGPU browsers
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
    // Create server-side context
    val context = EffectSummonContext.createServerContext()
    context.configureCanvas(config)
    context.configureInteractions(interactions)

    // Execute content within context - this populates the effect registry
    EffectSummonContext.withContext(context) {
        content()
    }

    // Build the effect composer data
    val composerData = context.buildComposerData(id)

    // Serialize to JSON
    val effectJson = SigilJson.encodeToString(composerData)
    val configJson = SigilJson.encodeToString(config)
    val interactionsJson = SigilJson.encodeToString(interactions)

    // Escape JSON for embedding in HTML
    val escapedEffectJson = escapeJsonForHtml(effectJson)
    val escapedConfigJson = escapeJsonForHtml(configJson)
    val escapedInteractionsJson = escapeJsonForHtml(interactionsJson)

    // Render the fallback content (for noscript)
    val fallbackHtml = fallback()

    // Build the HTML output
    val html = buildString {
        // Container div
        append("""<div id="$id-container" style="width: $width; height: $height; position: relative;">""")

        // Canvas element with data attributes
        append("""<canvas """)
        append("""id="$id" """)
        append("""data-sigil-effect="true" """)
        append("""data-sigil-config='$escapedConfigJson' """)
        append("""data-sigil-interactions='$escapedInteractionsJson' """)
        append("""style="width: 100%; height: 100%; display: block;">""")
        append("""</canvas>""")

        // Embedded effect data for hydration
        append("""<script type="application/json" id="$id-effects">$escapedEffectJson</script>""")

        // Hydration loader script
        append(buildEffectHydrationScript(id))

        // Noscript fallback
        if (fallbackHtml.isNotEmpty()) {
            append("""<noscript>$fallbackHtml</noscript>""")
        }

        append("</div>")
    }

    // Try to inject via Summon's renderer if available
    // Works when called within Summon's SSR rendering pipeline
    try {
        RawHtml(html)
    } catch (_: Exception) {
        // No active renderer - caller will use returned string
    }

    return html
}

/**
 * Escape JSON string for safe embedding in HTML.
 */
private fun escapeJsonForHtml(json: String): String {
    return json
        .replace("</", "<\\/")
        .replace("<!--", "<\\!--")
        .replace("'", "\\'")
}

/**
 * Build the hydration script for effects.
 */
private fun buildEffectHydrationScript(canvasId: String): String {
    return """
        <script type="module">
        (function() {
            function hydrateSigilEffect() {
                if (typeof window.SigilEffectHydrator !== 'undefined') {
                    const canvas = document.getElementById('$canvasId');
                    const dataElement = document.getElementById('$canvasId-effects');
                    if (canvas && dataElement) {
                        const effectData = JSON.parse(dataElement.textContent);
                        const config = JSON.parse(canvas.dataset.sigilConfig.replace(/\\'/g, "'"));
                        const interactions = JSON.parse(canvas.dataset.sigilInteractions.replace(/\\'/g, "'"));
                        window.SigilEffectHydrator.hydrate('$canvasId', effectData, config, interactions);
                    }
                } else {
                    setTimeout(hydrateSigilEffect, 50);
                }
            }
            
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', hydrateSigilEffect);
            } else {
                hydrateSigilEffect();
            }
        })();
        </script>
    """.trimIndent()
}
