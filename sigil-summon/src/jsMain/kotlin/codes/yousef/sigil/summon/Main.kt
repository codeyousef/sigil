package codes.yousef.sigil.summon

import codes.yousef.sigil.summon.effects.SigilEffectHydratorJs
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * Main entry point for the Sigil hydration bundle.
 * 
 * This function is called when the compiled JavaScript bundle loads in the browser.
 * It exports the SigilEffectHydrator to the global window object and sets up
 * automatic hydration on DOMContentLoaded.
 */
fun main() {
    // Export SigilEffectHydrator to window for manual hydration
    js("window.SigilEffectHydrator = {}")
    val hydrator = js("window.SigilEffectHydrator")
    
    hydrator.hydrate = { canvasId: String ->
        SigilEffectHydratorJs.hydrate(canvasId)
    }
    
    hydrator.isWebGPUAvailable = {
        SigilEffectHydratorJs.isWebGPUAvailable()
    }
    
    hydrator.isWebGLAvailable = {
        SigilEffectHydratorJs.isWebGLAvailable()
    }
    
    hydrator.getAvailableRenderer = {
        SigilEffectHydratorJs.getAvailableRenderer()
    }
    
    // Also export under Sigil namespace for convenience
    js("window.Sigil = window.Sigil || {}")
    js("window.Sigil.EffectHydrator = window.SigilEffectHydrator")
    
    console.log("[Sigil] Hydration bundle loaded, renderer: ${SigilEffectHydratorJs.getAvailableRenderer()}")
    
    // Auto-hydrate on DOMContentLoaded if not already loaded
    val isLoading = js("document.readyState === 'loading'") as Boolean
    if (isLoading) {
        document.addEventListener("DOMContentLoaded", { autoHydrate() })
    } else {
        // DOM already loaded, hydrate now
        window.setTimeout({ autoHydrate() }, 0)
    }
}

/**
 * Automatically hydrate all canvases with data-sigil-effects attribute.
 */
private fun autoHydrate() {
    val canvases = document.querySelectorAll("canvas[data-sigil-effects]")
    console.log("[Sigil] Auto-hydrating ${canvases.length} effect canvas(es)")
    
    for (i in 0 until canvases.length) {
        val canvas = canvases.item(i)
        val canvasId = canvas?.asDynamic()?.id as? String
        if (canvasId != null && canvasId.isNotBlank()) {
            console.log("[Sigil] Hydrating canvas: $canvasId")
            SigilEffectHydratorJs.hydrate(canvasId)
        }
    }
}
