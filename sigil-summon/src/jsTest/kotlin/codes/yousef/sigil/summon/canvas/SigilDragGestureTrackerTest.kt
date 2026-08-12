package codes.yousef.sigil.summon.canvas

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilDragGestureTrackerTest {
    @Test
    fun mouseAndPenMovementMustCrossFourPixelThresholdBeforeDragStarts() {
        val tracker = SigilDragGestureTracker()
        assertTrue(tracker.beginPointer(1, SigilPointerPosition(10f, 10f), "mouse", isPrimary = true))

        assertFalse(tracker.movedBeyondThreshold(1, SigilPointerPosition(13f, 10f)))
        assertTrue(tracker.movedBeyondThreshold(1, SigilPointerPosition(14f, 10f)))

        tracker.endWithoutDrag(1)
        assertTrue(tracker.beginPointer(2, SigilPointerPosition(10f, 10f), "pen", isPrimary = true))
        assertFalse(tracker.movedBeyondThreshold(2, SigilPointerPosition(13f, 10f)))
        assertTrue(tracker.movedBeyondThreshold(2, SigilPointerPosition(14f, 10f)))
    }

    @Test
    fun touchMovementMustCrossEightPixelThresholdBeforeDragStarts() {
        val tracker = SigilDragGestureTracker()
        assertTrue(tracker.beginPointer(7, SigilPointerPosition(10f, 10f), "touch", isPrimary = true))

        assertFalse(tracker.movedBeyondThreshold(7, SigilPointerPosition(17f, 10f)))
        assertTrue(tracker.movedBeyondThreshold(7, SigilPointerPosition(18f, 10f)))
    }

    @Test
    fun secondaryAndConcurrentPointersAreIgnored() {
        val tracker = SigilDragGestureTracker()

        assertFalse(tracker.beginPointer(2, SigilPointerPosition(0f, 0f), "touch", isPrimary = false))
        assertFalse(tracker.hasActivePointer())
        assertTrue(tracker.beginPointer(1, SigilPointerPosition(0f, 0f), "touch", isPrimary = true))
        assertFalse(tracker.beginPointer(3, SigilPointerPosition(20f, 20f), "pen", isPrimary = true))
        assertTrue(tracker.ownsPointer(1))
        assertFalse(tracker.ownsPointer(3))
        assertFalse(tracker.movedBeyondThreshold(3, SigilPointerPosition(100f, 100f)))
    }

    @Test
    fun completedDragSuppressesExactlyOneTrailingClick() {
        val tracker = SigilDragGestureTracker()
        tracker.beginPointer(1, SigilPointerPosition(0f, 0f), "mouse", isPrimary = true)
        tracker.completeDrag(1)

        assertTrue(tracker.consumeClickSuppression())
        assertFalse(tracker.consumeClickSuppression())
    }

    @Test
    fun simpleClickDoesNotSuppressClick() {
        val tracker = SigilDragGestureTracker()
        tracker.beginPointer(1, SigilPointerPosition(0f, 0f), "touch", isPrimary = true)
        tracker.endWithoutDrag(1)

        assertFalse(tracker.consumeClickSuppression())
    }

    @Test
    fun newPointerGestureClearsStaleSuppression() {
        val tracker = SigilDragGestureTracker()
        tracker.beginPointer(1, SigilPointerPosition(0f, 0f), "mouse", isPrimary = true)
        tracker.completeDrag(1)

        tracker.beginPointer(2, SigilPointerPosition(20f, 20f), "mouse", isPrimary = true)

        assertFalse(tracker.consumeClickSuppression())
    }
}
