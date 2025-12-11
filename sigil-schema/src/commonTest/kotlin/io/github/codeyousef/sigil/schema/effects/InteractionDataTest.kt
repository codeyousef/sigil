package io.github.codeyousef.sigil.schema.effects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import io.github.codeyousef.sigil.schema.SigilJson

/**
 * TDD tests for interaction data types.
 */
class InteractionDataTest {

    private val json = SigilJson

    @Test
    fun interactionType_shouldHaveAllExpectedValues() {
        val types = InteractionType.entries
        assertTrue(types.contains(InteractionType.MOUSE_MOVE))
        assertTrue(types.contains(InteractionType.MOUSE_CLICK))
        assertTrue(types.contains(InteractionType.KEYBOARD))
        assertTrue(types.contains(InteractionType.SCROLL))
        assertTrue(types.contains(InteractionType.TOUCH))
    }

    @Test
    fun interactionConfig_shouldHaveDefaults() {
        val config = InteractionConfig()
        
        assertTrue(config.enableMouseMove)
        assertTrue(config.enableMouseClick)
        assertTrue(config.enableKeyboard)
        assertTrue(config.enableScroll)
        assertTrue(config.enableTouch)
    }

    @Test
    fun interactionConfig_shouldSerialize() {
        val config = InteractionConfig(
            enableMouseMove = true,
            enableMouseClick = false,
            enableKeyboard = true,
            enableScroll = false,
            enableTouch = false
        )

        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<InteractionConfig>(serialized)

        assertEquals(config.enableMouseMove, deserialized.enableMouseMove)
        assertEquals(config.enableMouseClick, deserialized.enableMouseClick)
        assertEquals(config.enableKeyboard, deserialized.enableKeyboard)
        assertEquals(config.enableScroll, deserialized.enableScroll)
        assertEquals(config.enableTouch, deserialized.enableTouch)
    }

    @Test
    fun mouseEvent_shouldStoreData() {
        val event = MouseEventData(
            position = Vec2(100f, 200f),
            normalizedPosition = Vec2(0.5f, 0.5f),
            button = MouseButton.LEFT
        )

        assertEquals(100f, event.position.x)
        assertEquals(200f, event.position.y)
        assertEquals(0.5f, event.normalizedPosition.x)
        assertEquals(MouseButton.LEFT, event.button)
    }

    @Test
    fun keyboardEvent_shouldStoreData() {
        val event = KeyboardEventData(
            key = "1",
            code = "Digit1",
            altKey = false,
            ctrlKey = false,
            shiftKey = false
        )

        assertEquals("1", event.key)
        assertEquals("Digit1", event.code)
    }

    @Test
    fun scrollEvent_shouldStoreData() {
        val event = ScrollEventData(
            delta = 120f,
            position = Vec2(500f, 300f)
        )

        assertEquals(120f, event.delta)
        assertEquals(500f, event.position.x)
    }

    @Test
    fun touchEventData_shouldStoreMultipleTouches() {
        val event = TouchEventData(
            touches = listOf(
                TouchPoint(id = 0, position = Vec2(100f, 100f)),
                TouchPoint(id = 1, position = Vec2(200f, 200f))
            )
        )

        assertEquals(2, event.touches.size)
        assertEquals(0, event.touches[0].id)
        assertEquals(100f, event.touches[0].position.x)
    }
}
