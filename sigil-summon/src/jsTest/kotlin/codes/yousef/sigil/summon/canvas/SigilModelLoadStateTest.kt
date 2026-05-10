package codes.yousef.sigil.summon.canvas

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilModelLoadStateTest {

    @Test
    fun hiddenModelDoesNotStartLoading() {
        val state = SigilModelLoadState()

        assertFalse(state.tryStartForVisibility(false))
        assertFalse(state.started)
        assertFalse(state.completed)
    }

    @Test
    fun visibleModelStartsOnlyOnce() {
        val state = SigilModelLoadState()

        assertTrue(state.tryStartForVisibility(true))
        assertFalse(state.tryStartForVisibility(true))
        assertTrue(state.started)

        state.complete()

        assertTrue(state.completed)
        assertFalse(state.tryStartForVisibility(true))
    }
}
