package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.ScreenAnchor
import codes.yousef.sigil.schema.ScreenLayerData

internal data class SigilScreenLayerPlacement(
    val x: Float,
    val y: Float,
    val scale: Float,
    val visible: Boolean
)

internal object SigilScreenLayoutResolver {
    fun resolve(layer: ScreenLayerData, viewportWidth: Int, viewportHeight: Int): SigilScreenLayerPlacement {
        val width = viewportWidth.coerceAtLeast(1).toFloat()
        val height = viewportHeight.coerceAtLeast(1).toFloat()
        val layout = if (viewportWidth <= layer.mobileBreakpoint) layer.mobile else layer.desktop
        val halfWidth = width / 2f
        val halfHeight = height / 2f

        val anchorX = when (layout.anchor) {
            ScreenAnchor.TOP_LEFT, ScreenAnchor.CENTER_LEFT, ScreenAnchor.BOTTOM_LEFT -> -halfWidth
            ScreenAnchor.TOP_CENTER, ScreenAnchor.CENTER, ScreenAnchor.BOTTOM_CENTER -> 0f
            ScreenAnchor.TOP_RIGHT, ScreenAnchor.CENTER_RIGHT, ScreenAnchor.BOTTOM_RIGHT -> halfWidth
        }
        val anchorY = when (layout.anchor) {
            ScreenAnchor.TOP_LEFT, ScreenAnchor.TOP_CENTER, ScreenAnchor.TOP_RIGHT -> halfHeight
            ScreenAnchor.CENTER_LEFT, ScreenAnchor.CENTER, ScreenAnchor.CENTER_RIGHT -> 0f
            ScreenAnchor.BOTTOM_LEFT, ScreenAnchor.BOTTOM_CENTER, ScreenAnchor.BOTTOM_RIGHT -> -halfHeight
        }
        val inwardX = when (layout.anchor) {
            ScreenAnchor.TOP_RIGHT, ScreenAnchor.CENTER_RIGHT, ScreenAnchor.BOTTOM_RIGHT -> -layout.offsetX
            else -> layout.offsetX
        }
        val inwardY = when (layout.anchor) {
            ScreenAnchor.TOP_LEFT, ScreenAnchor.TOP_CENTER, ScreenAnchor.TOP_RIGHT -> -layout.offsetY
            else -> layout.offsetY
        }

        return SigilScreenLayerPlacement(
            x = anchorX + inwardX + layer.position.getOrElse(0) { 0f },
            y = anchorY + inwardY + layer.position.getOrElse(1) { 0f },
            scale = layout.scale,
            visible = layer.visible && layout.visible
        )
    }
}

internal class SigilRequestGate {
    private val pending = mutableSetOf<String>()

    fun tryAcquire(key: String?, suppressWhilePending: Boolean): Boolean {
        if (key.isNullOrBlank() || !suppressWhilePending) return true
        return pending.add(key)
    }

    fun release(key: String?) {
        if (!key.isNullOrBlank()) pending.remove(key)
    }

    fun isPending(key: String): Boolean = key in pending

    fun clear() = pending.clear()
}

internal class SigilModelSwapTracker {
    private val generations = mutableMapOf<String, Long>()

    fun begin(nodeId: String): Long {
        val generation = (generations[nodeId] ?: 0L) + 1L
        generations[nodeId] = generation
        return generation
    }

    fun isCurrent(nodeId: String, generation: Long): Boolean = generations[nodeId] == generation

    fun clear() = generations.clear()
}
