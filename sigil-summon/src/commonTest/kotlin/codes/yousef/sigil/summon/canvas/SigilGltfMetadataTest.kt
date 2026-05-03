package codes.yousef.sigil.summon.canvas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun isGlbUrl_ignoresQueryAndHash() {
        assertTrue(SigilGltfMetadata.isGlbUrl("/models/package.glb?v=1#mesh"))
    }

    @Test
    fun glbToGltfJson_embedsBinaryBufferAndBufferViewImageUris() {
        val gltfJson = SigilGltfMetadata.glbToGltfJson(
            minimalGlb(
                """
                {
                  "buffers": [{ "byteLength": 8 }],
                  "bufferViews": [
                    { "buffer": 0, "byteOffset": 0, "byteLength": 4 },
                    { "buffer": 0, "byteOffset": 4, "byteLength": 4 }
                  ],
                  "images": [{ "bufferView": 1, "mimeType": "image/png" }],
                  "textures": [{ "source": 0 }],
                  "materials": [
                    {
                      "pbrMetallicRoughness": {
                        "baseColorTexture": { "index": 0 },
                        "baseColorFactor": [1, 0.5, 0.25, 1]
                      }
                    }
                  ]
                }
                """.trimIndent(),
                byteArrayOf(10, 11, 12, 13, 1, 2, 3, 4)
            )
        )

        assertTrue(gltfJson.contains("\"uri\":\"data:application/octet-stream;base64,CgsMDQECAwQ=\""))
        val textures = SigilGltfMetadata.extractBaseColorTextures(gltfJson)
        assertEquals("data:image/png;base64,AQIDBA==", textures.single().uri)
        assertEquals(listOf(1f, 0.5f, 0.25f, 1f), textures.single().baseColorFactor)
    }

    private fun minimalGlb(json: String, binaryChunk: ByteArray): ByteArray {
        val jsonBytes = json.encodeToByteArray().paddedToFour(0x20)
        val binBytes = binaryChunk.paddedToFour(0)
        val totalLength = 12 + 8 + jsonBytes.size + 8 + binBytes.size

        return buildList<Byte> {
            addUInt32LE(0x46546C67)
            addUInt32LE(2)
            addUInt32LE(totalLength)
            addUInt32LE(jsonBytes.size)
            addUInt32LE(0x4E4F534A)
            addAll(jsonBytes.toList())
            addUInt32LE(binBytes.size)
            addUInt32LE(0x004E4942)
            addAll(binBytes.toList())
        }.toByteArray()
    }

    private fun ByteArray.paddedToFour(padByte: Int): ByteArray {
        val padding = (4 - (size % 4)) % 4
        return this + ByteArray(padding) { padByte.toByte() }
    }

    private fun MutableList<Byte>.addUInt32LE(value: Int) {
        add((value and 0xFF).toByte())
        add(((value ushr 8) and 0xFF).toByte())
        add(((value ushr 16) and 0xFF).toByte())
        add(((value ushr 24) and 0xFF).toByte())
    }
}
