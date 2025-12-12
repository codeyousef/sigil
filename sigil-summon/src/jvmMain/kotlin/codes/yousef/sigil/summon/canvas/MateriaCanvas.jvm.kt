package codes.yousef.sigil.summon.canvas

import codes.yousef.summon.annotation.Composable
import codes.yousef.summon.components.foundation.RawHtml
import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.SigilScene
import codes.yousef.sigil.schema.SceneSettings
import codes.yousef.sigil.summon.context.SigilSummonContext

/**
 * JVM (Server-side) implementation of MateriaCanvas for Summon SSR.
 *
 * This implementation:
 * 1. Creates a SigilSummonContext for the composition
 * 2. Executes the content lambda (which populates the context with scene nodes)
 * 3. Serializes the collected scene data to JSON
 * 4. Renders a container div with embedded JSON data for client hydration
 *
 * The rendered HTML includes:
 * - A container div for the WebGPU canvas
 * - A script tag containing serialized scene data
 * - A loader script that triggers hydration on the client
 */
@Composable
actual fun MateriaCanvas(
    id: String,
    width: String,
    height: String,
    backgroundColor: Int,
    content: @Composable () -> String
): String {
    // Create server-side context
    val context = SigilSummonContext.createServerContext()

    // Configure default scene settings from backgroundColor
    context.configureSettings {
        SceneSettings(backgroundColor = backgroundColor)
    }

    // Execute content within context - this populates the node registry
    SigilSummonContext.withContext(context) {
        content()
    }

    // Build the complete scene
    val scene = context.buildScene()

    // Serialize scene to JSON
    val sceneJson = scene.toJson()

    // Escape JSON for embedding in HTML script tag
    val escapedJson = escapeJsonForHtml(sceneJson)

    // Build the HTML output
    val html = buildString {
        // Container div for the canvas
        append("""<div id="$id-container" style="width: $width; height: $height; position: relative;">""")

        // Initial div that the hydration script will replace with a WebGPU canvas
        append("""<div id="$id" style="width: 100%; height: 100%; background-color: ${intToHexColor(backgroundColor)};"></div>""")

        // Embedded scene data for hydration
        append("""<script type="application/json" id="$id-data">$escapedJson</script>""")

        // Hydration loader script
        append(buildHydrationScript(id))

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
 * Escape JSON string for safe embedding in HTML script tags.
 */
private fun escapeJsonForHtml(json: String): String {
    return json
        .replace("</", "<\\/") // Escape closing tags
        .replace("<!--", "<\\!--") // Escape HTML comments
}

/**
 * Convert ARGB int to CSS hex color string.
 */
private fun intToHexColor(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}"
}

/**
 * Build the hydration script that runs on the client.
 * Automatically loads the Sigil hydration bundle if not already present.
 */
private fun buildHydrationScript(canvasId: String): String {
    return """
        <script type="module">
        (function() {
            function loadSigilBundle() {
                return new Promise((resolve, reject) => {
                    if (typeof window.SigilHydrator !== 'undefined') {
                        resolve();
                        return;
                    }
                    if (document.querySelector('script[src*="sigil-hydration"]')) {
                        // Script tag exists, wait for it to load
                        const checkLoaded = () => {
                            if (typeof window.SigilHydrator !== 'undefined') {
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
                            if (typeof window.SigilHydrator !== 'undefined') {
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
            
            async function hydrateSigil() {
                try {
                    await loadSigilBundle();
                    const dataElement = document.getElementById('$canvasId-data');
                    if (dataElement) {
                        const sceneData = JSON.parse(dataElement.textContent);
                        window.SigilHydrator.hydrate('$canvasId', sceneData);
                    }
                } catch (e) {
                    console.error('Sigil hydration failed:', e);
                }
            }
            
            // Start hydration when DOM is ready
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', hydrateSigil);
            } else {
                hydrateSigil();
            }
        })();
        </script>
    """.trimIndent()
}
