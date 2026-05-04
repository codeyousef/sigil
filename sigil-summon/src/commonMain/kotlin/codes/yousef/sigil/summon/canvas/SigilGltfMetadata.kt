package codes.yousef.sigil.summon.canvas

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal data class GltfBaseColorTexture(
    val materialIndex: Int,
    val textureIndex: Int,
    val imageIndex: Int,
    val uri: String,
    val mimeType: String?,
    val baseColorFactor: List<Float>
)

internal object SigilGltfMetadata {
    private const val GLB_MAGIC = 0x46546C67
    private const val GLB_VERSION_2 = 2
    private const val GLB_CHUNK_JSON = 0x4E4F534A
    private const val GLB_CHUNK_BIN = 0x004E4942

    private data class ParsedGlb(
        val json: String,
        val binaryChunk: ByteArray?
    )

    fun isGltfUrl(url: String): Boolean =
        cleanAssetUrl(url).endsWith(".gltf", ignoreCase = true)

    fun isGlbUrl(url: String): Boolean =
        cleanAssetUrl(url).endsWith(".glb", ignoreCase = true)

    fun resolveAssetPath(requested: String, basePath: String? = null, modelUrl: String? = null): String {
        if (requested.isBlank() || requested.isAbsoluteAssetPath()) return requested

        val base = when {
            !basePath.isNullOrBlank() -> basePath
            !modelUrl.isNullOrBlank() -> modelUrl.substringBeforeLast("/", missingDelimiterValue = "")
            else -> ""
        }.trimEnd('/')

        if (base.isBlank()) return normalizeRelativePath(requested)
        if (base.startsWith("http://") || base.startsWith("https://")) {
            return normalizeUrlPath(base, requested)
        }
        return normalizeRelativePath("$base/$requested")
    }

    fun glbToGltfJson(glbBytes: ByteArray): String {
        val glb = parseGlb(glbBytes)
        return embedGlbResources(glb.json, glb.binaryChunk)
    }

    fun extractBaseColorTextures(gltfJson: String): List<GltfBaseColorTexture> {
        val root = Json.parseToJsonElement(gltfJson).jsonObject
        val materials = root.arrayOrEmpty("materials")
        val textures = root.arrayOrEmpty("textures")
        val images = root.arrayOrEmpty("images")

        return materials.mapIndexedNotNull { materialIndex, material ->
            val materialObject = material as? JsonObject ?: return@mapIndexedNotNull null
            val pbr = materialObject["pbrMetallicRoughness"] as? JsonObject ?: return@mapIndexedNotNull null
            val baseColorTexture = pbr["baseColorTexture"] as? JsonObject ?: return@mapIndexedNotNull null
            val textureIndex = baseColorTexture["index"]?.jsonPrimitive?.intOrNull ?: return@mapIndexedNotNull null
            val texture = textures.getOrNull(textureIndex) as? JsonObject ?: return@mapIndexedNotNull null
            val imageIndex = texture["source"]?.jsonPrimitive?.intOrNull ?: return@mapIndexedNotNull null
            val image = images.getOrNull(imageIndex) as? JsonObject ?: return@mapIndexedNotNull null
            val uri = image["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null

            GltfBaseColorTexture(
                materialIndex = materialIndex,
                textureIndex = textureIndex,
                imageIndex = imageIndex,
                uri = uri,
                mimeType = image["mimeType"]?.jsonPrimitive?.contentOrNull,
                baseColorFactor = pbr.floatArrayOrDefault("baseColorFactor", listOf(1f, 1f, 1f, 1f))
            )
        }
    }

    fun imageMimeType(uri: String, declaredMimeType: String? = null): String {
        declaredMimeType?.takeIf { it.startsWith("image/", ignoreCase = true) }?.let { return it }

        if (uri.startsWith("data:", ignoreCase = true)) {
            val metadata = uri.substringAfter("data:", missingDelimiterValue = "")
                .substringBefore(",", missingDelimiterValue = "")
            val mimeType = metadata.substringBefore(";").takeIf { it.startsWith("image/", ignoreCase = true) }
            if (mimeType != null) return mimeType
        }

        val lowerUri = cleanAssetUrl(uri).lowercase()
        return when {
            lowerUri.endsWith(".jpg") || lowerUri.endsWith(".jpeg") -> "image/jpeg"
            lowerUri.endsWith(".png") -> "image/png"
            lowerUri.endsWith(".webp") -> "image/webp"
            lowerUri.endsWith(".gif") -> "image/gif"
            lowerUri.endsWith(".bmp") -> "image/bmp"
            else -> "application/octet-stream"
        }
    }

    private fun cleanAssetUrl(url: String): String =
        url.substringBefore("?").substringBefore("#")

    private fun parseGlb(bytes: ByteArray): ParsedGlb {
        require(bytes.size >= 20) { "GLB file is too small" }
        require(bytes.readUInt32LE(0) == GLB_MAGIC) { "Invalid GLB magic" }
        require(bytes.readUInt32LE(4) == GLB_VERSION_2) { "Unsupported GLB version" }

        val declaredLength = bytes.readUInt32LE(8)
        require(declaredLength <= bytes.size) { "GLB declared length exceeds available bytes" }

        var offset = 12
        var json: String? = null
        var binaryChunk: ByteArray? = null

        while (offset + 8 <= declaredLength) {
            val chunkLength = bytes.readUInt32LE(offset)
            val chunkType = bytes.readUInt32LE(offset + 4)
            val chunkStart = offset + 8
            val chunkEnd = chunkStart + chunkLength
            require(chunkEnd <= declaredLength) { "GLB chunk exceeds declared length" }

            when (chunkType) {
                GLB_CHUNK_JSON -> {
                    json = bytes.copyOfRange(chunkStart, chunkEnd)
                        .decodeToString()
                        .trimEnd('\u0000', ' ', '\n', '\r', '\t')
                }
                GLB_CHUNK_BIN -> {
                    binaryChunk = bytes.copyOfRange(chunkStart, chunkEnd)
                }
            }

            offset = chunkEnd
        }

        return ParsedGlb(
            json = requireNotNull(json) { "GLB is missing a JSON chunk" },
            binaryChunk = binaryChunk
        )
    }

    private fun embedGlbResources(gltfJson: String, binaryChunk: ByteArray?): String {
        if (binaryChunk == null) return gltfJson

        val root = Json.parseToJsonElement(gltfJson).jsonObject
        val bufferViews = root.arrayOrEmpty("bufferViews")

        return buildJsonObject {
            for ((key, value) in root) {
                when (key) {
                    "buffers" -> put(key, embedPrimaryGlbBuffer(root.arrayOrEmpty("buffers"), binaryChunk))
                    "images" -> put(key, embedBufferViewImages(root.arrayOrEmpty("images"), bufferViews, binaryChunk))
                    else -> put(key, value)
                }
            }
        }.toString()
    }

    private fun embedPrimaryGlbBuffer(buffers: JsonArray, binaryChunk: ByteArray): JsonArray =
        buildJsonArray {
            buffers.forEachIndexed { index, buffer ->
                val bufferObject = buffer as? JsonObject
                if (index == 0 && bufferObject != null) {
                    add(buildJsonObject {
                        for ((key, value) in bufferObject) put(key, value)
                        put("uri", JsonPrimitive(binaryChunk.toDataUri("application/octet-stream")))
                    })
                } else {
                    add(buffer)
                }
            }
        }

    private fun embedBufferViewImages(
        images: JsonArray,
        bufferViews: JsonArray,
        binaryChunk: ByteArray
    ): JsonArray =
        buildJsonArray {
            images.forEach { image ->
                val imageObject = image as? JsonObject
                if (imageObject == null || imageObject["uri"] != null) {
                    add(image)
                    return@forEach
                }

                val bufferViewIndex = imageObject["bufferView"]?.jsonPrimitive?.intOrNull
                val bufferView = bufferViewIndex?.let { bufferViews.getOrNull(it) as? JsonObject }
                val imageBytes = bufferView?.sliceFromBinaryChunk(binaryChunk)
                if (imageBytes == null) {
                    add(image)
                    return@forEach
                }

                val mimeType = imageObject["mimeType"]?.jsonPrimitive?.contentOrNull
                    ?: "application/octet-stream"
                add(buildJsonObject {
                    for ((key, value) in imageObject) {
                        if (key != "bufferView") put(key, value)
                    }
                    put("uri", JsonPrimitive(imageBytes.toDataUri(mimeType)))
                })
            }
        }

    private fun JsonObject.sliceFromBinaryChunk(binaryChunk: ByteArray): ByteArray? {
        val bufferIndex = this["buffer"]?.jsonPrimitive?.intOrNull ?: 0
        if (bufferIndex != 0) return null

        val byteOffset = this["byteOffset"]?.jsonPrimitive?.intOrNull ?: 0
        val byteLength = this["byteLength"]?.jsonPrimitive?.intOrNull ?: return null
        if (byteOffset < 0 || byteLength < 0) return null

        val end = byteOffset + byteLength
        if (end > binaryChunk.size) return null
        return binaryChunk.copyOfRange(byteOffset, end)
    }

    private fun String.isAbsoluteAssetPath(): Boolean {
        val lower = lowercase()
        return startsWith("/") ||
            lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("data:") ||
            lower.startsWith("blob:")
    }

    private fun normalizeRelativePath(path: String): String {
        val prefix = if (path.startsWith("/")) "/" else ""
        val parts = mutableListOf<String>()
        val suffix = path.split("#", limit = 2)
        val pathAndQuery = suffix[0].split("?", limit = 2)

        for (part in pathAndQuery[0].split("/")) {
            when (part) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty() && parts.last() != "..") parts.removeLast() else parts.add(part)
                else -> parts.add(part)
            }
        }

        val query = pathAndQuery.getOrNull(1)?.let { "?$it" } ?: ""
        val hash = suffix.getOrNull(1)?.let { "#$it" } ?: ""
        return prefix + parts.joinToString("/") + query + hash
    }

    private fun normalizeUrlPath(base: String, requested: String): String {
        val schemeEnd = base.indexOf("://")
        if (schemeEnd < 0) return normalizeRelativePath("$base/$requested")

        val pathStart = base.indexOf('/', schemeEnd + 3)
        val origin = if (pathStart < 0) base else base.substring(0, pathStart)
        val basePath = if (pathStart < 0) "" else base.substring(pathStart).trimEnd('/')
        return origin + normalizeRelativePath("$basePath/$requested")
    }

    private fun JsonObject.arrayOrEmpty(name: String): JsonArray =
        this[name]?.jsonArray ?: JsonArray(emptyList())

    private fun JsonObject.floatArrayOrDefault(name: String, default: List<Float>): List<Float> {
        val array = this[name]?.jsonArray ?: return default
        return array.mapNotNull { it.jsonPrimitive.floatOrNull }.takeIf { it.isNotEmpty() } ?: default
    }

    private fun ByteArray.readUInt32LE(offset: Int): Int {
        require(offset >= 0 && offset + 4 <= size) { "Offset is outside the byte array" }
        return (this[offset].toInt() and 0xFF) or
            ((this[offset + 1].toInt() and 0xFF) shl 8) or
            ((this[offset + 2].toInt() and 0xFF) shl 16) or
            ((this[offset + 3].toInt() and 0xFF) shl 24)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun ByteArray.toDataUri(mimeType: String): String =
        "data:$mimeType;base64,${Base64.encode(this)}"
}
