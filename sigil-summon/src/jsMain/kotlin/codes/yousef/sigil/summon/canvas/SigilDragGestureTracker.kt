package codes.yousef.sigil.summon.canvas

internal data class SigilPointerPosition(
    val x: Float,
    val y: Float
)

internal class SigilDragGestureTracker(
    private val thresholdPx: Float = DEFAULT_DRAG_THRESHOLD_PX
) {
    private var pointerDown: SigilPointerPosition? = null
    private var suppressNextClick = false

    fun beginPointer(position: SigilPointerPosition) {
        pointerDown = position
        suppressNextClick = false
    }

    fun movedBeyondThreshold(position: SigilPointerPosition): Boolean {
        val start = pointerDown ?: return false
        val dx = position.x - start.x
        val dy = position.y - start.y
        return dx * dx + dy * dy >= thresholdPx * thresholdPx
    }

    fun completeDrag() {
        pointerDown = null
        suppressNextClick = true
    }

    fun endWithoutDrag() {
        pointerDown = null
    }

    fun consumeClickSuppression(): Boolean {
        val shouldSuppress = suppressNextClick
        suppressNextClick = false
        return shouldSuppress
    }

    fun reset() {
        pointerDown = null
        suppressNextClick = false
    }

    private companion object {
        const val DEFAULT_DRAG_THRESHOLD_PX = 4f
    }
}
