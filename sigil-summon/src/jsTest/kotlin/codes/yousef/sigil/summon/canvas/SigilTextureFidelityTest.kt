package codes.yousef.sigil.summon.canvas

import io.materia.geometry.BufferAttribute
import io.materia.geometry.BufferGeometry
import io.materia.material.MeshStandardMaterial
import io.materia.renderer.TextureFilter
import io.materia.texture.Texture2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SigilTextureFidelityTest {

    @Test
    fun configureMaterial_preservesBaseColorTextureWithoutBakingVertexColors() {
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
        }
        val texture = Texture2D.fromImageData(
            width = 4,
            height = 4,
            data = checkerTextureBytes()
        )
        val material = MeshStandardMaterial(map = texture)

        SigilTextureFidelity.configureMaterial(material)

        assertSame(texture, material.map)
        assertFalse(geometry.hasAttribute("color"))
        assertFalse(material.vertexColors)
        assertTrue(material.needsUpdate)
        assertTrue(texture.generateMipmaps)
        assertEquals(TextureFilter.LINEAR, texture.magFilter)
        assertEquals(TextureFilter.LINEAR_MIPMAP_LINEAR, texture.minFilter)
        assertEquals(4f, texture.anisotropy)
    }

    private fun checkerTextureBytes(): ByteArray {
        val bytes = ByteArray(4 * 4 * 4)
        for (pixel in 0 until 16) {
            val offset = pixel * 4
            val bright = if (pixel % 2 == 0) 255.toByte() else 0.toByte()
            bytes[offset] = bright
            bytes[offset + 1] = bright
            bytes[offset + 2] = bright
            bytes[offset + 3] = 255.toByte()
        }
        return bytes
    }
}
