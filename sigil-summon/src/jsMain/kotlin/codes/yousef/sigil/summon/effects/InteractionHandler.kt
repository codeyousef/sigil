package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.effects.InteractionConfig
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

/**
 * External declaration for ResizeObserver.
 */
external class ResizeObserver(callback: (Array<dynamic>) -> Unit) {
    fun observe(target: Element)
    fun unobserve(target: Element)
    fun disconnect()
}

/**
 * Handles user interactions (mouse, resize) for the effect hydrator.
 */
class InteractionHandler(
    private val canvas: HTMLCanvasElement,
    private val config: InteractionConfig
) {
    var mouseX = 0f
        private set
    var mouseY = 0f
        private set
    var isMouseDown = false
        private set
    
    private var resizeObserver: ResizeObserver? = null

    /**
     * Setup mouse listeners on the canvas.
     */
    fun setupMouseListeners() {
        if (!config.enableMouseMove && !config.enableMouseClick) return

        canvas.addEventListener("mousemove", { event ->
            val mouseEvent = event as MouseEvent
            val rect = canvas.getBoundingClientRect()
            mouseX = (mouseEvent.clientX - rect.left).toFloat()
            mouseY = (mouseEvent.clientY - rect.top).toFloat()
        })

        canvas.addEventListener("mousedown", {
            isMouseDown = true
        })

        canvas.addEventListener("mouseup", {
            isMouseDown = false
        })

        canvas.addEventListener("mouseleave", {
            isMouseDown = false
        })
        
        // Also listen on window for mouseup to catch drags outside canvas
        window.addEventListener("mouseup", {
            isMouseDown = false
        })
    }

    /**
     * Setup a ResizeObserver on an element with a callback.
     */
    fun setupResizeObserver(element: Element, onResize: (Double, Double) -> Unit) {
        resizeObserver = ResizeObserver { entries ->
            entries.forEach { entry ->
                val rect = entry.contentRect
                val width = rect.width as Double
                val height = rect.height as Double
                onResize(width, height)
            }
        }
        resizeObserver?.observe(element)
    }
    
    /**
     * Clean up listeners and observers.
     */
    fun dispose() {
        resizeObserver?.disconnect()
        resizeObserver = null
    }
}
