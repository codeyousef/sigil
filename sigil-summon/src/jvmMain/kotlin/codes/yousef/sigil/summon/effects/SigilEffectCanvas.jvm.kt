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

    // Escape JSON for embedding in HTML - different escaping for different contexts
    val escapedEffectJson = escapeJsonForScriptTag(effectJson)  // For <script> content
    val escapedConfigJson = escapeJsonForAttribute(configJson)   // For HTML attribute
    val escapedInteractionsJson = escapeJsonForAttribute(interactionsJson)  // For HTML attribute

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
 * Escape JSON for embedding inside a <script type="application/json"> tag.
 * Only need to escape sequences that could close the script tag.
 */
private fun escapeJsonForScriptTag(json: String): String {
    return json
        .replace("</", "<\\/")     // Prevent closing script tag
        .replace("<!--", "<\\!--") // Prevent HTML comment
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

/**
 * Build the hydration script for effects.
 * Automatically loads the Sigil hydration bundle if not already present.
 */
private fun buildEffectHydrationScript(canvasId: String): String {
    return """
        <script type="module">
        (function() {
            function loadSigilBundle() {
                return new Promise((resolve, reject) => {
                    if (typeof window.SigilEffectHydrator !== 'undefined') {
                        resolve();
                        return;
                    }
                    if (document.querySelector('script[src*="sigil-hydration"]')) {
                        // Script tag exists, wait for it to load
                        const checkLoaded = () => {
                            if (typeof window.SigilEffectHydrator !== 'undefined') {
                                resolve();
                            } else {
                                setTimeout(checkLoaded, 50);
                            }
                        };
                        checkLoaded();
                        return;
                    }
                    // Load the bundle
                    const script = document.createElement('script');
                    script.src = '/sigil-hydration.js';
                    script.onload = () => {
                        const checkLoaded = () => {
                            if (typeof window.SigilEffectHydrator !== 'undefined') {
                                resolve();
                            } else {
                                setTimeout(checkLoaded, 50);
                            }
                        };
                        checkLoaded();
                    };
                    script.onerror = () => reject(new Error('Failed to load Sigil hydration bundle'));
                    document.head.appendChild(script);
                });
            }
            
            async function hydrateSigilEffect() {
                try {
                    await loadSigilBundle();
                    const canvas = document.getElementById('$canvasId');
                    const dataElement = document.getElementById('$canvasId-effects');
                    if (canvas && dataElement) {
                        const effectData = JSON.parse(dataElement.textContent);
                        const config = JSON.parse(canvas.dataset.sigilConfig);
                        const interactions = JSON.parse(canvas.dataset.sigilInteractions);
                        window.SigilEffectHydrator.hydrate('$canvasId', effectData, config, interactions);
                    }
                } catch (e) {
                    console.error('Sigil hydration failed:', e);
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
