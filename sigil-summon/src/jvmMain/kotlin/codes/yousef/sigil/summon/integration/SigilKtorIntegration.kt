package codes.yousef.sigil.summon.integration

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPOutputStream

/**
 * Ktor integration for Sigil static assets.
 * Provides functions to serve Sigil's hydration JavaScript from the library JAR.
 */
object SigilKtorIntegration {

    // Cache for compressed assets to avoid re-compressing on each request
    private val compressedAssetCache = ConcurrentHashMap<String, ByteArray>()
    private val rawAssetCache = ConcurrentHashMap<String, ByteArray>()

    /**
     * Serves Sigil hydration assets directly from the library JAR.
     * This removes the need for users to manually extract and serve static files.
     *
     * Assets are served at:
     * - `/sigil-hydration.js` - JavaScript hydration client
     *
     * Usage:
     * ```kotlin
     * routing {
     *     sigilStaticAssets()
     *     // ... other routes
     * }
     * ```
     */
    fun Route.sigilStaticAssets() {
        get("/sigil-hydration.js") {
            call.respondSigilAsset("sigil-hydration.js", ContentType.Application.JavaScript)
        }
    }

    /**
     * Loads and responds with a Sigil asset from the library JAR resources.
     * Supports gzip compression and caching for optimal performance.
     */
    suspend fun ApplicationCall.respondSigilAsset(name: String, contentType: ContentType) {
        val payload = loadSigilAssetCached(name)
        if (payload == null) {
            respondText(
                """{"status":"not-found","asset":"$name"}""",
                ContentType.Application.Json,
                HttpStatusCode.NotFound
            )
            return
        }

        // Add cache headers - assets are immutable, cache for 1 year
        response.headers.append(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
        response.headers.append(HttpHeaders.Vary, "Accept-Encoding")

        // Check if client accepts gzip
        val acceptEncoding = request.headers[HttpHeaders.AcceptEncoding] ?: ""
        if (acceptEncoding.contains("gzip")) {
            // Serve gzip-compressed version
            val compressed = getCompressedAsset(name, payload)
            response.headers.append(HttpHeaders.ContentEncoding, "gzip")
            respondBytes(compressed, contentType)
        } else {
            respondBytes(payload, contentType)
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
    private fun loadSigilAssetCached(name: String): ByteArray? {
        return rawAssetCache.getOrPut(name) {
            loadSigilAsset(name) ?: return null
        }
    }

    /**
     * Loads a Sigil asset from the library JAR resources.
     * Searches in multiple locations for compatibility.
     */
    private fun loadSigilAsset(name: String): ByteArray? {
        val locations = listOf(
            "static/$name",
            "META-INF/resources/static/$name",
            "codes/yousef/sigil/static/$name"
        )
        for (path in locations) {
            val resource = Thread.currentThread().contextClassLoader.getResourceAsStream(path)
            if (resource != null) {
                return resource.use { it.readBytes() }
            }
        }
        return null
    }
}

/**
 * Extension function to easily add Sigil static assets to a route.
 * 
 * Usage:
 * ```kotlin
 * routing {
 *     sigilStaticAssets()
 * }
 * ```
 */
fun Route.sigilStaticAssets() = SigilKtorIntegration.run { sigilStaticAssets() }
