package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.HitVolumeData
import codes.yousef.sigil.schema.HitVolumeShape
import codes.yousef.sigil.schema.InteractionMetadata
import io.materia.core.math.Ray
import io.materia.core.math.Vector3
import io.materia.core.scene.Group
import io.materia.core.scene.Mesh
import io.materia.geometry.primitives.BoxGeometry
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SigilInteractionPickerTest {

    @Test
    fun intersectHitVolume_hitsBoxAfterApplyingNodeTransform() {
        val node = Group().apply {
            position.set(3f, 0f, 0f)
        }
        val interaction = InteractionMetadata(
            hitVolume = HitVolumeData(
                shape = HitVolumeShape.BOX,
                center = listOf(0f, 0f, 0f),
                size = listOf(2f, 2f, 2f)
            )
        )

        val hit = assertNotNull(
            SigilInteractionPicker.intersectHitVolume(
                ray = Ray(Vector3(3f, 0f, 5f), Vector3(0f, 0f, -1f)),
                node = node,
                interaction = interaction
            )
        )

        assertSame(node, hit.node)
        assertClose(4f, hit.intersection.distance)
        assertClose(3f, hit.intersection.point.x)
        assertClose(0f, hit.intersection.point.y)
        assertClose(1f, hit.intersection.point.z)
    }

    @Test
    fun intersectHitVolume_hitsSphere() {
        val node = Group()
        val interaction = InteractionMetadata(
            hitVolume = HitVolumeData(
                shape = HitVolumeShape.SPHERE,
                center = listOf(0f, 1f, 0f),
                radius = 1f
            )
        )

        val hit = assertNotNull(
            SigilInteractionPicker.intersectHitVolume(
                ray = Ray(Vector3(0f, 1f, 5f), Vector3(0f, 0f, -1f)),
                node = node,
                interaction = interaction
            )
        )

        assertClose(4f, hit.intersection.distance)
        assertClose(0f, hit.intersection.point.x)
        assertClose(1f, hit.intersection.point.y)
        assertClose(1f, hit.intersection.point.z)
    }

    @Test
    fun intersectHitVolume_infersBoundingBoxFromChildGeometryWhenSizeIsOmitted() {
        val node = Group().apply {
            add(Mesh(BoxGeometry(width = 2f, height = 2f, depth = 2f)))
        }
        val interaction = InteractionMetadata(
            hitVolume = HitVolumeData(shape = HitVolumeShape.BOUNDING_BOX)
        )

        val hit = assertNotNull(
            SigilInteractionPicker.intersectHitVolume(
                ray = Ray(Vector3(0f, 0f, 5f), Vector3(0f, 0f, -1f)),
                node = node,
                interaction = interaction
            )
        )

        assertClose(4f, hit.intersection.distance)
        assertClose(1f, hit.intersection.point.z)
    }

    @Test
    fun intersectHitVolume_ignoresMeshShapeForMateriaRaycastFallback() {
        val hit = SigilInteractionPicker.intersectHitVolume(
            ray = Ray(Vector3(0f, 0f, 5f), Vector3(0f, 0f, -1f)),
            node = Group(),
            interaction = InteractionMetadata(
                hitVolume = HitVolumeData(shape = HitVolumeShape.MESH)
            )
        )

        assertNull(hit)
    }

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.001f, "Expected $expected, got $actual")
    }
}
