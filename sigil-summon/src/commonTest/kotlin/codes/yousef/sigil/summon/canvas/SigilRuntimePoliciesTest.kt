package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.ScreenAnchor
import codes.yousef.sigil.schema.ScreenLayerData
import codes.yousef.sigil.schema.ScreenLayoutData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilRuntimePoliciesTest {
    @Test
    fun screenLayoutUsesDesktopAndMobileAnchors() {
        val layer = ScreenLayerData(
            id = "hud",
            desktop = ScreenLayoutData(ScreenAnchor.TOP_RIGHT, offsetX = 20f, offsetY = 30f),
            mobile = ScreenLayoutData(ScreenAnchor.BOTTOM_CENTER, offsetX = 0f, offsetY = 16f, scale = 0.8f),
            mobileBreakpoint = 600
        )

        val desktop = SigilScreenLayoutResolver.resolve(layer, 1000, 800)
        val mobile = SigilScreenLayoutResolver.resolve(layer, 390, 844)

        assertEquals(480f, desktop.x)
        assertEquals(370f, desktop.y)
        assertEquals(0f, mobile.x)
        assertEquals(-406f, mobile.y)
        assertEquals(0.8f, mobile.scale)
    }

    @Test
    fun duplicateRequestsAreSuppressedUntilReleased() {
        val gate = SigilRequestGate()

        assertTrue(gate.tryAcquire("focus:1", suppressWhilePending = true))
        assertFalse(gate.tryAcquire("focus:1", suppressWhilePending = true))
        assertTrue(gate.isPending("focus:1"))

        gate.release("focus:1")

        assertTrue(gate.tryAcquire("focus:1", suppressWhilePending = true))
        assertTrue(gate.tryAcquire("focus:1", suppressWhilePending = false))
    }

    @Test
    fun modelSwapTrackerRejectsStaleReplacement() {
        val tracker = SigilModelSwapTracker()
        val first = tracker.begin("slot:1")
        val second = tracker.begin("slot:1")

        assertFalse(tracker.isCurrent("slot:1", first))
        assertTrue(tracker.isCurrent("slot:1", second))
    }
}
