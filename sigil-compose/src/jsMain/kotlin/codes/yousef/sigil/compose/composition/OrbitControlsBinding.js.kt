package codes.yousef.sigil.compose.composition

import codes.yousef.sigil.compose.canvas.MateriaCanvasState
import io.materia.controls.Key
import io.materia.controls.OrbitControls
import io.materia.controls.PointerButton
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

internal actual fun bindOrbitControls(
    controls: OrbitControls,
    canvasState: MateriaCanvasState
): OrbitControlsBinding? {
    val canvasElement = canvasState.canvas as? HTMLCanvasElement ?: return null
    canvasElement.style.setProperty("touch-action", "none")

    var activeButton: PointerButton? = null

    fun pointerPosition(event: MouseEvent): Pair<Float, Float> {
        val rect = canvasElement.getBoundingClientRect()
        val x = event.clientX - rect.left
        val y = event.clientY - rect.top
        return Pair(x.toFloat(), y.toFloat())
    }

    val mouseDown: (Event) -> Unit = mouseDown@{ event ->
        val mouseEvent = event as? MouseEvent ?: return@mouseDown
        val button = toPointerButton(mouseEvent.button)
        val (x, y) = pointerPosition(mouseEvent)
        activeButton = button
        controls.onPointerDown(x, y, button)
        mouseEvent.preventDefault()
    }

    val mouseMove: (Event) -> Unit = mouseMove@{ event ->
        val mouseEvent = event as? MouseEvent ?: return@mouseMove
        val button = activeButton ?: return@mouseMove
        val (x, y) = pointerPosition(mouseEvent)
        controls.onPointerMove(x, y, button)
        mouseEvent.preventDefault()
    }

    val mouseUp: (Event) -> Unit = mouseUp@{ event ->
        val mouseEvent = event as? MouseEvent ?: return@mouseUp
        val button = activeButton ?: toPointerButton(mouseEvent.button)
        val (x, y) = pointerPosition(mouseEvent)
        controls.onPointerUp(x, y, button)
        activeButton = null
        mouseEvent.preventDefault()
    }

    val wheelHandler: (Event) -> Unit = wheelHandler@{ event ->
        val wheelEvent = event as? WheelEvent ?: return@wheelHandler
        controls.onWheel(wheelEvent.deltaX.toFloat(), wheelEvent.deltaY.toFloat())
        wheelEvent.preventDefault()
    }

    val contextMenu: (Event) -> Unit = contextMenu@{ event ->
        val mouseEvent = event as? MouseEvent ?: return@contextMenu
        mouseEvent.preventDefault()
    }

    val keyDown: (Event) -> Unit = keyDown@{ event ->
        val keyEvent = event as? KeyboardEvent ?: return@keyDown
        toControlsKey(keyEvent.key)?.let { key ->
            controls.onKeyDown(key)
            keyEvent.preventDefault()
        }
    }

    val keyUp: (Event) -> Unit = keyUp@{ event ->
        val keyEvent = event as? KeyboardEvent ?: return@keyUp
        toControlsKey(keyEvent.key)?.let { key ->
            controls.onKeyUp(key)
            keyEvent.preventDefault()
        }
    }

    canvasElement.addEventListener("mousedown", mouseDown)
    canvasElement.addEventListener("mousemove", mouseMove)
    canvasElement.addEventListener("mouseup", mouseUp)
    canvasElement.addEventListener("wheel", wheelHandler)
    canvasElement.addEventListener("contextmenu", contextMenu)
    window.addEventListener("keydown", keyDown)
    window.addEventListener("keyup", keyUp)

    return object : OrbitControlsBinding {
        override fun dispose() {
            canvasElement.removeEventListener("mousedown", mouseDown)
            canvasElement.removeEventListener("mousemove", mouseMove)
            canvasElement.removeEventListener("mouseup", mouseUp)
            canvasElement.removeEventListener("wheel", wheelHandler)
            canvasElement.removeEventListener("contextmenu", contextMenu)
            window.removeEventListener("keydown", keyDown)
            window.removeEventListener("keyup", keyUp)
        }
    }
}

private fun toPointerButton(button: Short): PointerButton {
    return when (button.toInt()) {
        1 -> PointerButton.AUXILIARY
        2 -> PointerButton.SECONDARY
        else -> PointerButton.PRIMARY
    }
}

private fun toControlsKey(key: String): Key? {
    return when (key.lowercase()) {
        "w" -> Key.W
        "a" -> Key.A
        "s" -> Key.S
        "d" -> Key.D
        "q" -> Key.Q
        "e" -> Key.E
        "shift" -> Key.SHIFT
        " " -> Key.SPACE
        "space" -> Key.SPACE
        "arrowup" -> Key.ARROW_UP
        "arrowdown" -> Key.ARROW_DOWN
        "arrowleft" -> Key.ARROW_LEFT
        "arrowright" -> Key.ARROW_RIGHT
        "escape" -> Key.ESCAPE
        "enter" -> Key.ENTER
        else -> null
    }
}
