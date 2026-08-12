package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.DragMetadata
import io.materia.core.math.Ray
import io.materia.core.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SigilDragCancellationTest {
    @Test
    fun cancellationRestoresExactSourceStartAndDoesNotDispatchDrop() {
        val session = requireNotNull(
            SigilDragController.begin(
                ray = Ray(Vector3(2f, 3f, 8f), Vector3(0f, 0f, -1f)),
                nodePosition = Vector3(2f, 3f, 0f),
                hitPoint = Vector3(2f, 3f, 0f),
                metadata = DragMetadata()
            )
        )

        val cancelled = SigilDragCancellation.stateFor(session)

        assertClose(2f, cancelled.sourcePosition.x)
        assertClose(3f, cancelled.sourcePosition.y)
        assertClose(0f, cancelled.sourcePosition.z)
        assertNull(cancelled.targetState)
        assertFalse(cancelled.accepted)
        assertEquals("cancelled", cancelled.result)
        assertFalse(cancelled.dispatchDrop)
    }

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(kotlin.math.abs(expected - actual) < 0.001f, "Expected $expected, got $actual")
    }
}
