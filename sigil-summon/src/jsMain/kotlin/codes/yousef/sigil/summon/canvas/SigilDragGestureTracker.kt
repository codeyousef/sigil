package codes.yousef.sigil.summon.canvas

internal data class SigilPointerPosition(
    val x: Float,
    val y: Float
)

internal class SigilDragGestureTracker(
    private val mouseAndPenThresholdPx: Float = DEFAULT_MOUSE_AND_PEN_DRAG_THRESHOLD_PX,
    private val touchThresholdPx: Float = DEFAULT_TOUCH_DRAG_THRESHOLD_PX
) {
    private data class PointerDown(
        val pointerId: Int,
        val position: SigilPointerPosition,
        val thresholdPx: Float
    )

    private var pointerDown: PointerDown? = null
    private var suppressNextClick = false

    fun beginPointer(
        pointerId: Int,
        position: SigilPointerPosition,
        pointerType: String,
        isPrimary: Boolean
    ): Boolean {
        if (!isPrimary || pointerDown != null) return false

        pointerDown = PointerDown(
            pointerId = pointerId,
            position = position,
            thresholdPx = if (pointerType.equals("touch", ignoreCase = true)) {
                touchThresholdPx
            } else {
                mouseAndPenThresholdPx
            }
        )
        suppressNextClick = false
        return true
    }

    fun hasActivePointer(): Boolean = pointerDown != null

    fun ownsPointer(pointerId: Int): Boolean = pointerDown?.pointerId == pointerId

    fun movedBeyondThreshold(pointerId: Int, position: SigilPointerPosition): Boolean {
        val start = pointerDown ?: return false
        if (start.pointerId != pointerId) return false
        val dx = position.x - start.position.x
        val dy = position.y - start.position.y
        return dx * dx + dy * dy >= start.thresholdPx * start.thresholdPx
    }

    fun completeDrag(pointerId: Int) {
        if (!ownsPointer(pointerId)) return
        pointerDown = null
        suppressNextClick = true
    }

    fun endWithoutDrag(pointerId: Int) {
        if (!ownsPointer(pointerId)) return
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
        const val DEFAULT_MOUSE_AND_PEN_DRAG_THRESHOLD_PX = 4f
        const val DEFAULT_TOUCH_DRAG_THRESHOLD_PX = 8f
    }
}
