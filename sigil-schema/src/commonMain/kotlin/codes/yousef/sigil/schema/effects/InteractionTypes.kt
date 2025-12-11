package codes.yousef.sigil.schema.effects

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Types of user interactions that effects can respond to.
 */
@Serializable
enum class InteractionType {
    @SerialName("mouseMove") MOUSE_MOVE,
    @SerialName("mouseClick") MOUSE_CLICK,
    @SerialName("keyboard") KEYBOARD,
    @SerialName("scroll") SCROLL,
    @SerialName("touch") TOUCH
}

/**
 * Configuration for which interactions an effect responds to.
 */
@Serializable
data class InteractionConfig(
    val enableMouseMove: Boolean = true,
    val enableMouseClick: Boolean = true,
    val enableKeyboard: Boolean = true,
    val enableScroll: Boolean = true,
    val enableTouch: Boolean = true
) {
    /**
     * Returns a set of enabled interaction types.
     */
    fun enabledTypes(): Set<InteractionType> = buildSet {
        if (enableMouseMove) add(InteractionType.MOUSE_MOVE)
        if (enableMouseClick) add(InteractionType.MOUSE_CLICK)
        if (enableKeyboard) add(InteractionType.KEYBOARD)
        if (enableScroll) add(InteractionType.SCROLL)
        if (enableTouch) add(InteractionType.TOUCH)
    }
    
    companion object {
        /** No interactions enabled */
        val NONE = InteractionConfig(
            enableMouseMove = false,
            enableMouseClick = false,
            enableKeyboard = false,
            enableScroll = false,
            enableTouch = false
        )
        
        /** All interactions enabled (default) */
        val ALL = InteractionConfig()
        
        /** Only mouse interactions */
        val MOUSE_ONLY = InteractionConfig(
            enableKeyboard = false,
            enableScroll = false,
            enableTouch = false
        )
    }
}

/**
 * Data for mouse move/click events.
 */
@Serializable
data class MouseEventData(
    /** Position in pixels from top-left */
    val position: Vec2,
    /** Position normalized to [0-1, 0-1] */
    val normalizedPosition: Vec2,
    /** Which button was pressed (for click events) */
    val button: MouseButton = MouseButton.LEFT
)

/**
 * Data for keyboard events.
 */
@Serializable
data class KeyboardEventData(
    /** The key value (e.g., "a", "1", "Enter") */
    val key: String,
    /** The physical key code (e.g., "KeyA", "Digit1") */
    val code: String,
    /** Whether Alt was held */
    val altKey: Boolean = false,
    /** Whether Ctrl was held */
    val ctrlKey: Boolean = false,
    /** Whether Shift was held */
    val shiftKey: Boolean = false
)

/**
 * Data for scroll events.
 */
@Serializable
data class ScrollEventData(
    /** Scroll delta (positive = down) */
    val delta: Float,
    /** Position where scroll occurred */
    val position: Vec2
)

/**
 * Single touch point data.
 */
@Serializable
data class TouchPoint(
    /** Unique identifier for this touch */
    val id: Int,
    /** Position in pixels */
    val position: Vec2
)

/**
 * Data for touch events.
 */
@Serializable
data class TouchEventData(
    /** All active touch points */
    val touches: List<TouchPoint>
)

/**
 * Interface for effects that respond to user input.
 * Effects can implement this to receive interaction events.
 */
interface InteractiveEffect {
    /** Called when mouse moves */
    fun onMouseMove(position: Vec2, normalized: Vec2) {}
    
    /** Called when mouse button is clicked */
    fun onMouseClick(position: Vec2, button: MouseButton) {}
    
    /** Called when a key is pressed */
    fun onKeyPress(key: String, code: String) {}
    
    /** Called when scroll occurs */
    fun onScroll(delta: Float, position: Vec2) {}
    
    /** Called when touch state changes */
    fun onTouch(touches: List<TouchPoint>) {}
}
