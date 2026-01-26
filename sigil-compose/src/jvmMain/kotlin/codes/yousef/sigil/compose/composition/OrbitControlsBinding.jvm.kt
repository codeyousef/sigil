package codes.yousef.sigil.compose.composition

import codes.yousef.sigil.compose.canvas.MateriaCanvasState
import io.materia.controls.Key
import io.materia.controls.OrbitControls
import io.materia.controls.PointerButton
import java.awt.Component
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener

internal actual fun bindOrbitControls(
    controls: OrbitControls,
    canvasState: MateriaCanvasState
): OrbitControlsBinding? {
    val component = canvasState.canvas as? Component ?: return null
    component.isFocusable = true
    component.requestFocus()

    var activeButton: PointerButton? = null

    val mouseListener = object : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            val button = toPointerButton(event.button)
            activeButton = button
            controls.onPointerDown(event.x.toFloat(), event.y.toFloat(), button)
        }

        override fun mouseReleased(event: MouseEvent) {
            val button = activeButton ?: toPointerButton(event.button)
            controls.onPointerUp(event.x.toFloat(), event.y.toFloat(), button)
            activeButton = null
        }
    }

    val motionListener = object : MouseMotionAdapter() {
        override fun mouseDragged(event: MouseEvent) {
            val button = activeButton ?: return
            controls.onPointerMove(event.x.toFloat(), event.y.toFloat(), button)
        }
    }

    val wheelListener = MouseWheelListener { event: MouseWheelEvent ->
        controls.onWheel(0f, event.preciseWheelRotation.toFloat())
    }

    val keyListener = object : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            toControlsKey(event.keyCode)?.let { key ->
                controls.onKeyDown(key)
            }
        }

        override fun keyReleased(event: KeyEvent) {
            toControlsKey(event.keyCode)?.let { key ->
                controls.onKeyUp(key)
            }
        }
    }

    component.addMouseListener(mouseListener)
    component.addMouseMotionListener(motionListener)
    component.addMouseWheelListener(wheelListener)
    component.addKeyListener(keyListener)

    return object : OrbitControlsBinding {
        override fun dispose() {
            component.removeMouseListener(mouseListener)
            component.removeMouseMotionListener(motionListener)
            component.removeMouseWheelListener(wheelListener)
            component.removeKeyListener(keyListener)
        }
    }
}

private fun toPointerButton(button: Int): PointerButton {
    return when (button) {
        MouseEvent.BUTTON2 -> PointerButton.AUXILIARY
        MouseEvent.BUTTON3 -> PointerButton.SECONDARY
        else -> PointerButton.PRIMARY
    }
}

private fun toControlsKey(keyCode: Int): Key? {
    return when (keyCode) {
        KeyEvent.VK_W -> Key.W
        KeyEvent.VK_A -> Key.A
        KeyEvent.VK_S -> Key.S
        KeyEvent.VK_D -> Key.D
        KeyEvent.VK_Q -> Key.Q
        KeyEvent.VK_E -> Key.E
        KeyEvent.VK_SHIFT -> Key.SHIFT
        KeyEvent.VK_SPACE -> Key.SPACE
        KeyEvent.VK_UP -> Key.ARROW_UP
        KeyEvent.VK_DOWN -> Key.ARROW_DOWN
        KeyEvent.VK_LEFT -> Key.ARROW_LEFT
        KeyEvent.VK_RIGHT -> Key.ARROW_RIGHT
        KeyEvent.VK_ESCAPE -> Key.ESCAPE
        KeyEvent.VK_ENTER -> Key.ENTER
        else -> null
    }
}
