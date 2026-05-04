package codes.yousef.sigil.summon.canvas

import io.materia.core.scene.Mesh
import io.materia.geometry.BufferAttribute
import io.materia.geometry.BufferGeometry
import io.materia.material.MeshStandardMaterial
import io.materia.texture.Texture2D
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilWebGlTextureBakerTest {

    @Test
    fun bakeBaseColorTexture_multipliesTextureSamplesIntoVertexColors() {
        val geometry = BufferGeometry().apply {
            setAttribute(
                "position",
                BufferAttribute(
                    floatArrayOf(
                        0f, 0f, 0f,
                        1f, 0f, 0f,
                        0f, 1f, 0f
                    ),
                    3
                )
            )
            setAttribute(
                "uv",
                BufferAttribute(
                    floatArrayOf(
                        0f, 0f,
                        1f, 0f,
                        0f, 1f
                    ),
                    2
                )
            )
            setAttribute(
                "color",
                BufferAttribute(
                    floatArrayOf(
                        0.5f, 1f, 1f,
                        1f, 1f, 1f,
                        1f, 1f, 1f
                    ),
                    3
                )
            )
        }
        val texture = Texture2D.fromImageData(
            width = 2,
            height = 2,
            data = byteArrayOf(
                255.toByte(), 0, 0, 255.toByte(),
                0, 255.toByte(), 0, 255.toByte(),
                0, 0, 255.toByte(), 255.toByte(),
                255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()
            )
        )
        val material = MeshStandardMaterial(map = texture)
        val mesh = Mesh(geometry, material)

        assertTrue(SigilWebGlTextureBaker.bakeBaseColorTexture(mesh))

        val baked = geometry.getAttribute("color") ?: error("Missing baked color attribute")
        assertClose(0.5f, baked.getX(0))
        assertClose(0f, baked.getY(0))
        assertClose(0f, baked.getZ(0))
        assertClose(0f, baked.getX(1))
        assertClose(1f, baked.getY(1))
        assertClose(0f, baked.getZ(1))
        assertClose(0f, baked.getX(2))
        assertClose(0f, baked.getY(2))
        assertClose(1f, baked.getZ(2))
    }

    @Test
    fun bakeBaseColorTexture_skipsRepeatedBakeForSameTextureVersion() {
        val geometry = BufferGeometry().apply {
            setAttribute("position", BufferAttribute(floatArrayOf(0f, 0f, 0f), 3))
            setAttribute("uv", BufferAttribute(floatArrayOf(0f, 0f), 2))
        }
        val texture = Texture2D.fromImageData(
            width = 1,
            height = 1,
            data = byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
        )
        val mesh = Mesh(geometry, MeshStandardMaterial(map = texture))

        assertTrue(SigilWebGlTextureBaker.bakeBaseColorTexture(mesh))
        assertFalse(SigilWebGlTextureBaker.bakeBaseColorTexture(mesh))
    }

    @Test
    fun bakeBaseColorTexture_doesNotRebakeSharedGeometryMaterialInstances() {
        val geometry = BufferGeometry().apply {
            setAttribute("position", BufferAttribute(floatArrayOf(0f, 0f, 0f), 3))
            setAttribute("uv", BufferAttribute(floatArrayOf(0f, 0f), 2))
            setAttribute("color", BufferAttribute(floatArrayOf(0.5f, 1f, 1f), 3))
        }
        val texture = Texture2D.fromImageData(
            width = 1,
            height = 1,
            data = byteArrayOf(128.toByte(), 255.toByte(), 255.toByte(), 255.toByte())
        )
        val material = MeshStandardMaterial(map = texture)
        val firstMesh = Mesh(geometry, material)
        val secondMesh = Mesh(geometry, material)

        assertTrue(SigilWebGlTextureBaker.bakeBaseColorTexture(firstMesh))
        assertFalse(SigilWebGlTextureBaker.bakeBaseColorTexture(secondMesh))

        val baked = geometry.getAttribute("color") ?: error("Missing baked color attribute")
        assertClose(0.5f * (128f / 255f), baked.getX(0))
        assertClose(1f, baked.getY(0))
        assertClose(1f, baked.getZ(0))
    }

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(abs(expected - actual) < 0.001f, "Expected $expected, got $actual")
    }
}
