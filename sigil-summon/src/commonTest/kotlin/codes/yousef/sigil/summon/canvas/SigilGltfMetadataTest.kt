package codes.yousef.sigil.summon.canvas

import kotlin.test.Test
import kotlin.test.assertEquals

class SigilGltfMetadataTest {

    @Test
    fun resolveAssetPath_resolvesTextureRelativeToLocalGltf() {
        val resolved = SigilGltfMetadata.resolveAssetPath(
            requested = "textures/albedo.png",
            modelUrl = "models/warehouse/scene.gltf"
        )

        assertEquals("models/warehouse/textures/albedo.png", resolved)
    }

    @Test
    fun resolveAssetPath_normalizesParentSegmentsAndKeepsQueryHash() {
        val resolved = SigilGltfMetadata.resolveAssetPath(
            requested = "../textures/albedo.png?rev=1#atlas",
            modelUrl = "models/warehouse/scene.gltf"
        )

        assertEquals("models/textures/albedo.png?rev=1#atlas", resolved)
    }

    @Test
    fun resolveAssetPath_preservesAbsoluteRemoteUrls() {
        val resolved = SigilGltfMetadata.resolveAssetPath(
            requested = "textures/albedo.png",
            modelUrl = "https://cdn.example.com/assets/scene.gltf"
        )

        assertEquals("https://cdn.example.com/assets/textures/albedo.png", resolved)
    }

    @Test
    fun extractBaseColorTextures_readsMaterialTextureAndFactor() {
        val metadata = SigilGltfMetadata.extractBaseColorTextures(
            """
            {
              "materials": [
                {
                  "pbrMetallicRoughness": {
                    "baseColorTexture": { "index": 0 },
                    "baseColorFactor": [0.25, 0.5, 0.75, 0.8]
                  }
                }
              ],
              "textures": [{ "source": 0 }],
              "images": [{ "uri": "textures/atlas.png" }]
            }
            """.trimIndent()
        )

        assertEquals(1, metadata.size)
        assertEquals(0, metadata.single().materialIndex)
        assertEquals(0, metadata.single().textureIndex)
        assertEquals(0, metadata.single().imageIndex)
        assertEquals("textures/atlas.png", metadata.single().uri)
        assertEquals(listOf(0.25f, 0.5f, 0.75f, 0.8f), metadata.single().baseColorFactor)
    }
}
