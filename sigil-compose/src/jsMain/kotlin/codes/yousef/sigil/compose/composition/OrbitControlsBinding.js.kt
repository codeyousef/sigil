package codes.yousef.sigil.compose.composition

import codes.yousef.sigil.compose.canvas.MateriaCanvasState
import io.materia.controls.Key
import io.materia.controls.OrbitControls
import io.materia.controls.PointerButton
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.WheelEvent

internal actual fun bindOrbitControls(
    controls: OrbitControls,
    canvasState: MateriaCanvasState
): OrbitControlsBinding? {
    val canvasElement = canvasState.canvas as? HTMLCanvasElement ?: return null
    canvasElement.style.touchAction = "none"

    var activeButton: PointerButton? = null

    fun pointerPosition(event: MouseEvent): Pair<Float, Float> {
        val rect = canvasElement.getBoundingClientRect()
        val x = event.clientX - rect.left
        val y = event.clientY - rect.top
        return Pair(x.toFloat(), y.toFloat())
    }

    val mouseDown: (MouseEvent) -> Unit = { event ->
        val button = toPointerButton(event.button)
        val (x, y) = pointerPosition(event)
        activeButton = button
        controls.onPointerDown(x, y, button)
        event.preventDefault()
    }

    val mouseMove: (MouseEvent) -> Unit = mouseMove@{ event ->
        val button = activeButton ?: return@mouseMove
        val (x, y) = pointerPosition(event)
        controls.onPointerMove(x, y, button)
        event.preventDefault()
    }

    val mouseUp: (MouseEvent) -> Unit = { event ->
        val button = activeButton ?: toPointerButton(event.button)
        val (x, y) = pointerPosition(event)
        controls.onPointerUp(x, y, button)
        activeButton = null
        event.preventDefault()
    }

    val wheelHandler: (WheelEvent) -> Unit = { event ->
        controls.onWheel(event.deltaX.toFloat(), event.deltaY.toFloat())
        event.preventDefault()
    }

    val contextMenu: (MouseEvent) -> Unit = { event ->
        event.preventDefault()
    }

    val keyDown: (KeyboardEvent) -> Unit = { event ->
        toControlsKey(event.key)?.let { key ->
            controls.onKeyDown(key)
            event.preventDefault()
        }
    }

    val keyUp: (KeyboardEvent) -> Unit = { event ->
        toControlsKey(event.key)?.let { key ->
            controls.onKeyUp(key)
            event.preventDefault()
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
