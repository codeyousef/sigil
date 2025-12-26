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
    private val config: InteractionConfig,
    private val normalizeCoordinates: Boolean = false
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
        if (!config.enableMouseMove && !config.enableMouseClick && !config.enableTouch) return

        canvas.addEventListener("mousemove", { event ->
            val mouseEvent = event as MouseEvent
            val rect = canvas.getBoundingClientRect()
            updatePointer(
                x = (mouseEvent.clientX - rect.left).toFloat(),
                y = (mouseEvent.clientY - rect.top).toFloat(),
                rectWidth = rect.width,
                rectHeight = rect.height
            )
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

        if (config.enableTouch) {
            canvas.addEventListener("touchmove", { event ->
                val touches = event.asDynamic().touches
                if (touches != undefined && touches != null && touches.length > 0) {
                    val touch = touches[0]
                    val rect = canvas.getBoundingClientRect()
                    updatePointer(
                        x = (touch.clientX - rect.left).toFloat(),
                        y = (touch.clientY - rect.top).toFloat(),
                        rectWidth = rect.width,
                        rectHeight = rect.height
                    )
                }
            })

            canvas.addEventListener("touchstart", {
                isMouseDown = true
            })

            canvas.addEventListener("touchend", {
                isMouseDown = false
            })

            canvas.addEventListener("touchcancel", {
                isMouseDown = false
            })
        }
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

    private fun updatePointer(x: Float, y: Float, rectWidth: Double, rectHeight: Double) {
        if (normalizeCoordinates && rectWidth != 0.0 && rectHeight != 0.0) {
            mouseX = (x / rectWidth).toFloat()
            mouseY = (y / rectHeight).toFloat()
        } else {
            mouseX = x
            mouseY = y
        }
    }
}
