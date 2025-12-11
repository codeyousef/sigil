package codes.yousef.sigil.summon.integration

import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPOutputStream

/**
 * Core asset loading functionality shared across all framework integrations.
 * Provides caching, compression, and resource loading from the library JAR.
 */
object SigilAssets {

    // Cache for compressed assets to avoid re-compressing on each request
    private val compressedAssetCache = ConcurrentHashMap<String, ByteArray>()
    private val rawAssetCache = ConcurrentHashMap<String, ByteArray>()

    /**
     * Available Sigil assets.
     */
    object Assets {
        const val HYDRATION_JS = "sigil-hydration.js"
        const val HYDRATION_JS_MAP = "sigil-hydration.js.map"
    }

    /**
     * Content types for assets.
     */
    object ContentTypes {
        const val JAVASCRIPT = "application/javascript"
        const val JSON = "application/json"
        const val SOURCE_MAP = "application/json"
    }

    /**
     * Result of loading an asset.
     */
    data class AssetResult(
        val bytes: ByteArray,
        val contentType: String,
        val isCompressed: Boolean = false
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AssetResult) return false
            return bytes.contentEquals(other.bytes) && contentType == other.contentType && isCompressed == other.isCompressed
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + isCompressed.hashCode()
            return result
        }
    }

    /**
     * Loads a Sigil asset, optionally gzip-compressed.
     *
     * @param name The asset name (e.g., "sigil-hydration.js")
     * @param acceptsGzip Whether the client accepts gzip encoding
     * @return The asset result, or null if not found
     */
    fun loadAsset(name: String, acceptsGzip: Boolean = false): AssetResult? {
        val raw = loadAssetCached(name) ?: return null
        val contentType = getContentType(name)

        return if (acceptsGzip) {
            AssetResult(
                bytes = getCompressedAsset(name, raw),
                contentType = contentType,
                isCompressed = true
            )
        } else {
            AssetResult(
                bytes = raw,
                contentType = contentType,
                isCompressed = false
            )
        }
    }

    /**
     * Gets the content type for an asset.
     */
    fun getContentType(name: String): String {
        return when {
            name.endsWith(".js") -> ContentTypes.JAVASCRIPT
            name.endsWith(".map") -> ContentTypes.SOURCE_MAP
            name.endsWith(".json") -> ContentTypes.JSON
            else -> "application/octet-stream"
        }
    }

    /**
     * Gets or creates a gzip-compressed version of an asset.
     */
    private fun getCompressedAsset(name: String, original: ByteArray): ByteArray {
        return compressedAssetCache.getOrPut(name) {
            ByteArrayOutputStream().use { baos ->
                GZIPOutputStream(baos).use { gzos ->
                    gzos.write(original)
                }
                baos.toByteArray()
            }
        }
    }

    /**
     * Loads a Sigil asset with caching.
     */
    private fun loadAssetCached(name: String): ByteArray? {
        return rawAssetCache.getOrPut(name) {
            loadAssetFromResources(name) ?: return null
        }
    }

    /**
     * Loads a Sigil asset from the library JAR resources.
     * Searches in multiple locations for compatibility with different classloaders.
     */
    private fun loadAssetFromResources(name: String): ByteArray? {
        val locations = listOf(
            "static/$name",
            "META-INF/resources/static/$name",
            "codes/yousef/sigil/static/$name"
        )
        
        // Try thread context classloader first
        for (path in locations) {
            Thread.currentThread().contextClassLoader?.getResourceAsStream(path)?.use {
                return it.readBytes()
            }
        }
        
        // Fallback to class classloader
        for (path in locations) {
            SigilAssets::class.java.classLoader?.getResourceAsStream(path)?.use {
                return it.readBytes()
            }
        }
        
        return null
    }

    /**
     * Clears all cached assets. Useful for testing or hot-reload scenarios.
     */
    fun clearCache() {
        compressedAssetCache.clear()
        rawAssetCache.clear()
    }
}
