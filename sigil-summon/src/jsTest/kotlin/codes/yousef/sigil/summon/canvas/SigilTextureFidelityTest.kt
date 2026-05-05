package codes.yousef.sigil.summon.canvas

import io.materia.geometry.BufferAttribute
import io.materia.geometry.BufferGeometry
import io.materia.core.scene.Mesh
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
        val material = MeshStandardMaterial(map = texture).apply {
            needsUpdate = false
        }

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

    @Test
    fun configureGltfGeometryAttributes_keepsTexturedMeshesFromMultiplyingColor0() {
        val texture = Texture2D.fromImageData(
            width = 4,
            height = 4,
            data = checkerTextureBytes()
        )
        val material = MeshStandardMaterial(map = texture).apply {
            needsUpdate = false
        }
        val mesh = Mesh(createGltfColorGeometry(), material)

        SigilTextureFidelity.configureGltfGeometryAttributes(mesh)

        assertTrue(mesh.geometry.hasAttribute("uv"))
        assertTrue(mesh.geometry.hasAttribute("color"))
        assertSame(texture, material.map)
        assertFalse(material.vertexColors)
        assertFalse(material.needsUpdate)
    }

    @Test
    fun configureGltfGeometryAttributes_enablesVertexColorsForUntexturedMeshes() {
        val material = MeshStandardMaterial().apply {
            needsUpdate = false
        }
        val mesh = Mesh(createGltfColorGeometry(), material)

        SigilTextureFidelity.configureGltfGeometryAttributes(mesh)

        assertTrue(mesh.geometry.hasAttribute("uv"))
        assertTrue(mesh.geometry.hasAttribute("color"))
        assertTrue(material.vertexColors)
        assertTrue(material.needsUpdate)
    }

    private fun createGltfColorGeometry(): BufferGeometry =
        BufferGeometry().apply {
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
                "TEXCOORD_0",
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
                "COLOR_0",
                BufferAttribute(
                    floatArrayOf(
                        0.2f, 0.3f, 0.4f,
                        0.5f, 0.6f, 0.7f,
                        0.8f, 0.9f, 1f
                    ),
                    3
                )
            )
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
