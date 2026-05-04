package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.DragConstraintMode
import codes.yousef.sigil.schema.DragMetadata
import io.materia.core.math.Ray
import io.materia.core.math.Vector3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SigilDragControllerTest {

    @Test
    fun horizontalDragTracksRayOnXzPlaneAndPreservesHeight() {
        val metadata = DragMetadata(mode = DragConstraintMode.HORIZONTAL)
        val startRay = rayToward(Vector3(0f, 5f, 5f), Vector3(0f, 2f, 0f))
        val session = assertNotNull(
            SigilDragController.begin(
                ray = startRay,
                nodePosition = Vector3(0f, 2f, 0f),
                hitPoint = Vector3(0f, 2f, 0f),
                metadata = metadata
            )
        )

        val position = assertNotNull(
            SigilDragController.positionFor(
                session,
                rayToward(Vector3(2f, 5f, 5f), Vector3(2f, 2f, -1f))
            )
        )

        assertClose(2f, position.x)
        assertClose(2f, position.y)
        assertClose(-1f, position.z)
    }

    @Test
    fun verticalDragTracksRayOnVerticalPlaneAndPreservesDepth() {
        val metadata = DragMetadata(mode = DragConstraintMode.VERTICAL)
        val session = assertNotNull(
            SigilDragController.begin(
                ray = rayToward(Vector3(0f, 0f, 5f), Vector3(0f, 1f, 0f)),
                nodePosition = Vector3(0f, 1f, 0f),
                hitPoint = Vector3(0f, 1f, 0f),
                metadata = metadata
            )
        )

        val position = assertNotNull(
            SigilDragController.positionFor(
                session,
                rayToward(Vector3(1f, 3f, 5f), Vector3(1f, 3f, 0f))
            )
        )

        assertClose(1f, position.x)
        assertClose(3f, position.y)
        assertClose(0f, position.z)
    }

    @Test
    fun laneDragProjectsMovementOntoAxisAndClampsToBounds() {
        val metadata = DragMetadata(
            mode = DragConstraintMode.LANE,
            planeNormal = listOf(0f, 1f, 0f),
            laneAxis = listOf(1f, 0f, 0f),
            min = 0f,
            max = 3f
        )
        val session = assertNotNull(
            SigilDragController.begin(
                ray = rayToward(Vector3(1f, 5f, 0f), Vector3(1f, 0f, 0f)),
                nodePosition = Vector3(1f, 0f, 0f),
                hitPoint = Vector3(1f, 0f, 0f),
                metadata = metadata
            )
        )

        val position = assertNotNull(
            SigilDragController.positionFor(
                session,
                rayToward(Vector3(10f, 5f, 0f), Vector3(10f, 0f, 0f))
            )
        )

        assertClose(3f, position.x)
        assertClose(0f, position.y)
        assertClose(0f, position.z)
    }

    @Test
    fun laneDragSupportsSingleSidedClampBounds() {
        val maxOnly = assertNotNull(
            SigilDragController.begin(
                ray = rayToward(Vector3(1f, 5f, 0f), Vector3(1f, 0f, 0f)),
                nodePosition = Vector3(1f, 0f, 0f),
                hitPoint = Vector3(1f, 0f, 0f),
                metadata = DragMetadata(
                    mode = DragConstraintMode.LANE,
                    planeNormal = listOf(0f, 1f, 0f),
                    laneAxis = listOf(1f, 0f, 0f),
                    max = 3f
                )
            )
        )

        val maxPosition = assertNotNull(
            SigilDragController.positionFor(
                maxOnly,
                rayToward(Vector3(10f, 5f, 0f), Vector3(10f, 0f, 0f))
            )
        )

        val minOnly = assertNotNull(
            SigilDragController.begin(
                ray = rayToward(Vector3(1f, 5f, 0f), Vector3(1f, 0f, 0f)),
                nodePosition = Vector3(1f, 0f, 0f),
                hitPoint = Vector3(1f, 0f, 0f),
                metadata = DragMetadata(
                    mode = DragConstraintMode.LANE,
                    planeNormal = listOf(0f, 1f, 0f),
                    laneAxis = listOf(1f, 0f, 0f),
                    min = -1f
                )
            )
        )

        val minPosition = assertNotNull(
            SigilDragController.positionFor(
                minOnly,
                rayToward(Vector3(-10f, 5f, 0f), Vector3(-10f, 0f, 0f))
            )
        )

        assertClose(3f, maxPosition.x)
        assertClose(-1f, minPosition.x)
    }

    @Test
    fun customPlaneReturnsNullWhenPointerRayIsParallel() {
        val session = assertNotNull(
            SigilDragController.begin(
                ray = Ray(Vector3(0f, 1f, 0f), Vector3(0f, -1f, 0f)),
                nodePosition = Vector3(0f, 0f, 0f),
                hitPoint = Vector3(0f, 0f, 0f),
                metadata = DragMetadata(
                    mode = DragConstraintMode.CAMERA_PLANE,
                    planeNormal = listOf(0f, 1f, 0f),
                    planePoint = listOf(0f, 0f, 0f)
                )
            )
        )

        assertNull(
            SigilDragController.positionFor(
                session,
                Ray(Vector3(0f, 1f, 0f), Vector3(1f, 0f, 0f))
            )
        )
    }

    private fun rayToward(origin: Vector3, target: Vector3): Ray =
        Ray(origin, Vector3(target.x - origin.x, target.y - origin.y, target.z - origin.z).normalize())

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.001f, "Expected $expected, got $actual")
    }
}
