package codes.yousef.sigil.summon.canvas

import codes.yousef.summon.annotation.Composable
import codes.yousef.sigil.schema.SigilScene
import codes.yousef.sigil.summon.context.SigilSummonContext
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLScriptElement

private val scope = MainScope()

/**
 * JS (Client-side) implementation of MateriaCanvas for Summon.
 *
 * This implementation handles hydration:
 * 1. Locates the pre-rendered container and scene data
 * 2. Parses the serialized scene JSON
 * 3. Creates actual Materia objects from the schema data
 * 4. Initializes the WebGPU renderer
 * 5. Starts the render loop
 */
@Composable
actual fun MateriaCanvas(
    id: String,
    width: String,
    height: String,
    backgroundColor: Int,
    content: @Composable () -> String
): String {
    // On the client, we might be called during initial render
    // In that case, we also execute content to collect nodes
    val context = SigilSummonContext.createClientContext()

    SigilSummonContext.withContext(context) {
        content()
    }

    // Schedule hydration for after render
    scheduleHydration(id, context.buildScene())

    // Return container HTML (same as server for consistency)
    return """<div id="$id" style="width: $width; height: $height;"></div>"""
}

/**
 * Schedule hydration to run after the current execution context.
 */
private fun scheduleHydration(canvasId: String, fallbackScene: SigilScene) {
    window.setTimeout({
        performHydration(canvasId, fallbackScene)
    }, 0)
}

/**
 * Perform the actual hydration process.
 */
private fun performHydration(canvasId: String, fallbackScene: SigilScene) {
    // Try to get pre-rendered scene data
    val dataElement = document.getElementById("$canvasId-data") as? HTMLScriptElement
    val scene = if (dataElement != null) {
        val jsonData = dataElement.textContent ?: "{}"
        SigilScene.fromJson(jsonData)
    } else {
        // Use fallback scene from client-side composition
        fallbackScene
    }

    // Get or create the container element
    val container = document.getElementById(canvasId) as? HTMLDivElement
        ?: run {
            console.error("Sigil: Container element '$canvasId' not found")
            return
        }

    // Create canvas element
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.style.width = "100%"
    canvas.style.height = "100%"

    // Clear container and add canvas
    container.innerHTML = ""
    container.appendChild(canvas)

    // Set canvas size
    val rect = container.getBoundingClientRect()
    canvas.width = rect.width.toInt()
    canvas.height = rect.height.toInt()

    // Initialize Materia (async)
    scope.launch {
        try {
            val hydrator = SigilHydrator(canvas, scene)
            hydrator.initialize()
            hydrator.startRenderLoop()

            // Store reference for cleanup
            js("window")["sigilHydrators"] = js("window.sigilHydrators || {}")
            js("window.sigilHydrators")["canvasId"] = hydrator

        } catch (e: Exception) {
            console.error("Sigil: Failed to hydrate scene: ${e.message}")
        }
    }
}
