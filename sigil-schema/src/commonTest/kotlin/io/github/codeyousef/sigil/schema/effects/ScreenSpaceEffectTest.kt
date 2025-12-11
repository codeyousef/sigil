package io.github.codeyousef.sigil.schema.effects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD tests for ScreenSpaceEffect system.
 */
class ScreenSpaceEffectTest {

    @Test
    fun effectContext_shouldHaveAllRequiredFields() {
        val context = EffectContext(
            time = 1.5f,
            deltaTime = 0.016f,
            resolution = Vec2(1920f, 1080f),
            mouse = Vec2(0.5f, 0.5f),
            scroll = 100f
        )

        assertEquals(1.5f, context.time)
        assertEquals(0.016f, context.deltaTime)
        assertEquals(1920f, context.resolution.x)
        assertEquals(1080f, context.resolution.y)
        assertEquals(0.5f, context.mouse.x)
        assertEquals(0.5f, context.mouse.y)
        assertEquals(100f, context.scroll)
    }

    @Test
    fun vec2_shouldStoreCoordinates() {
        val vec = Vec2(3.14f, 2.71f)
        assertEquals(3.14f, vec.x)
        assertEquals(2.71f, vec.y)
    }

    @Test
    fun vec2_shouldHaveDefaultConstructor() {
        val vec = Vec2()
        assertEquals(0f, vec.x)
        assertEquals(0f, vec.y)
    }

    @Test
    fun vec3_shouldStoreCoordinates() {
        val vec = Vec3(1f, 2f, 3f)
        assertEquals(1f, vec.x)
        assertEquals(2f, vec.y)
        assertEquals(3f, vec.z)
    }

    @Test
    fun vec4_shouldStoreCoordinates() {
        val vec = Vec4(0.5f, 0.5f, 0.5f, 1.0f)
        assertEquals(0.5f, vec.x)
        assertEquals(0.5f, vec.y)
        assertEquals(0.5f, vec.z)
        assertEquals(1.0f, vec.w)
    }

    @Test
    fun blendMode_shouldHaveAllExpectedValues() {
        val modes = BlendMode.entries
        assertTrue(modes.contains(BlendMode.NORMAL))
        assertTrue(modes.contains(BlendMode.ADDITIVE))
        assertTrue(modes.contains(BlendMode.MULTIPLY))
        assertTrue(modes.contains(BlendMode.SCREEN))
        assertTrue(modes.contains(BlendMode.OVERLAY))
    }
}
