package codes.yousef.sigil.sample.summon

import codes.yousef.sigil.schema.SigilScene
import codes.yousef.sigil.summon.canvas.SigilHydrator
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement

private val scope = MainScope()

fun main() {
    window.onload = { hydrateScenes() }
}

private fun hydrateScenes() {
    val canvases = document.querySelectorAll("canvas[data-sigil-scene]")
    for (index in 0 until canvases.length) {
        val canvas = canvases.item(index) as? HTMLCanvasElement ?: continue
        val sceneJson = canvas.getAttribute("data-sigil-scene") ?: continue
        val scene = try {
            SigilScene.fromJson(sceneJson)
        } catch (error: Throwable) {
            console.error("Failed to parse Sigil sample scene: ${error.message}")
            continue
        }

        scope.launch {
            try {
                val hydrator = SigilHydrator(canvas, scene)
                hydrator.initialize()
                hydrator.startRenderLoop()
                window.asDynamic().sampleSigilHydrator = hydrator
            } catch (error: Throwable) {
                console.error("Failed to hydrate Sigil sample scene: ${error.message}")
            }
        }
    }
}

private external object console {
    fun error(message: String)
}
