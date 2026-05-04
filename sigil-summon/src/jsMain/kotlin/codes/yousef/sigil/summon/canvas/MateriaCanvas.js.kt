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
        waitForStableCanvasLayout(canvasId) {
            performHydration(canvasId, fallbackScene)
        }
    }, 0)
}

private fun waitForStableCanvasLayout(canvasId: String, onReady: () -> Unit) {
    var stableKey = ""
    var stableFrames = 0
    var attempts = 0

    fun checkLayout() {
        attempts += 1
        val element = document.getElementById(canvasId)
        val target = element?.parentElement ?: element
        val rect = target?.asDynamic()?.getBoundingClientRect()
        val width = if (rect != null) (rect.width as Number).toInt() else 0
        val height = if (rect != null) (rect.height as Number).toInt() else 0

        if (width > 1 && height > 1) {
            val key = "${width}x$height"
            if (key == stableKey) {
                stableFrames += 1
                if (stableFrames >= 2) {
                    onReady()
                    return
                }
            } else {
                stableKey = key
                stableFrames = 1
            }
        }

        if (attempts >= 30) {
            onReady()
        } else {
            window.requestAnimationFrame { checkLayout() }
        }
    }

    window.requestAnimationFrame { checkLayout() }
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

    // Get the element — may be a server-rendered <canvas> or a legacy <div>
    val element = document.getElementById(canvasId)
        ?: run {
            console.error("Sigil: Element '$canvasId' not found")
            return
        }

    val canvas: HTMLCanvasElement = when (element) {
        is HTMLCanvasElement -> element  // Server-rendered canvas — reuse
        is HTMLDivElement -> {           // Legacy div placeholder — create canvas
            val c = document.createElement("canvas") as HTMLCanvasElement
            c.style.width = "100%"
            c.style.height = "100%"
            c.style.display = "block"
            element.innerHTML = ""
            element.appendChild(c)
            c
        }
        else -> {
            console.error("Sigil: Element '$canvasId' is not a canvas or div")
            return
        }
    }

    // Set canvas size
    val rect = (canvas.parentElement ?: canvas).asDynamic().getBoundingClientRect()
    canvas.width = (rect.width as Number).toInt()
    canvas.height = (rect.height as Number).toInt()

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
