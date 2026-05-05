package codes.yousef.sigil.summon.canvas

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilDragGestureTrackerTest {
    @Test
    fun pointerMovementMustCrossThresholdBeforeDragStarts() {
        val tracker = SigilDragGestureTracker(thresholdPx = 4f)
        tracker.beginPointer(SigilPointerPosition(10f, 10f))

        assertFalse(tracker.movedBeyondThreshold(SigilPointerPosition(13f, 10f)))
        assertTrue(tracker.movedBeyondThreshold(SigilPointerPosition(14f, 10f)))
    }

    @Test
    fun completedDragSuppressesExactlyOneTrailingClick() {
        val tracker = SigilDragGestureTracker()
        tracker.beginPointer(SigilPointerPosition(0f, 0f))
        tracker.completeDrag()

        assertTrue(tracker.consumeClickSuppression())
        assertFalse(tracker.consumeClickSuppression())
    }

    @Test
    fun simpleClickDoesNotSuppressClick() {
        val tracker = SigilDragGestureTracker()
        tracker.beginPointer(SigilPointerPosition(0f, 0f))
        tracker.endWithoutDrag()

        assertFalse(tracker.consumeClickSuppression())
    }

    @Test
    fun newPointerGestureClearsStaleSuppression() {
        val tracker = SigilDragGestureTracker()
        tracker.beginPointer(SigilPointerPosition(0f, 0f))
        tracker.completeDrag()

        tracker.beginPointer(SigilPointerPosition(20f, 20f))

        assertFalse(tracker.consumeClickSuppression())
    }
}
