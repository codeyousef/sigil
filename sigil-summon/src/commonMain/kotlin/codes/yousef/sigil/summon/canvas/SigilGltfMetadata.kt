package codes.yousef.sigil.summon.canvas

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class GltfBaseColorTexture(
    val materialIndex: Int,
    val textureIndex: Int,
    val imageIndex: Int,
    val uri: String,
    val baseColorFactor: List<Float>
)

internal object SigilGltfMetadata {
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
                baseColorFactor = pbr.floatArrayOrDefault("baseColorFactor", listOf(1f, 1f, 1f, 1f))
            )
        }
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
}
