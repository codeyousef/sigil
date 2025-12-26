package codes.yousef.sigil.summon.effects

import kotlinx.browser.window

/**
 * Small helper that owns a requestAnimationFrame loop and delivers time data to a callback.
 */
class RenderLoop {
    private var running = false
    private var animationFrameId: Int = 0
    private var startTimeMs: Double = 0.0
    private var lastTotalTime = 0.0

    fun start(onFrame: (totalTimeSeconds: Double, deltaTimeSeconds: Double) -> Unit) {
        if (running) return
        running = true
        startTimeMs = 0.0
        lastTotalTime = 0.0

        fun step(currentTimeMs: Double) {
            if (!running) return
            if (startTimeMs == 0.0) {
                startTimeMs = currentTimeMs
            }
            val totalTimeSeconds = (currentTimeMs - startTimeMs) / 1000.0
            val deltaTimeSeconds = (totalTimeSeconds - lastTotalTime).coerceAtLeast(0.0)

            onFrame(totalTimeSeconds, deltaTimeSeconds)
            lastTotalTime = totalTimeSeconds

            animationFrameId = window.requestAnimationFrame(::step)
        }

        animationFrameId = window.requestAnimationFrame(::step)
    }

    fun stop() {
        running = false
        if (animationFrameId != 0) {
            window.cancelAnimationFrame(animationFrameId)
            animationFrameId = 0
        }
    }
}
