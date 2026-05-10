package codes.yousef.sigil.summon.canvas

internal class SigilModelLoadState {
    var started: Boolean = false
        private set

    var completed: Boolean = false
        private set

    fun shouldStartForVisibility(visible: Boolean): Boolean =
        visible && !started

    fun tryStartForVisibility(visible: Boolean): Boolean {
        if (!shouldStartForVisibility(visible)) return false
        started = true
        return true
    }

    fun complete() {
        completed = true
    }
}
