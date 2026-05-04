package codes.yousef.sigil.summon.integration

import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.summon.canvas.SigilSceneCallbackRegistry
import codes.yousef.sigil.summon.canvas.SigilSceneEventCallbackResponse
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Ktor integration for Sigil static assets.
 * Provides functions to serve Sigil's hydration JavaScript from the library JAR.
 * 
 * Compatible with Ktor 3.x.
 */
object SigilKtorIntegration {

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
            respondSigilAsset(SigilAssets.Assets.HYDRATION_JS)
        }
        get("/sigil-hydration.js.map") {
            respondSigilAsset(SigilAssets.Assets.HYDRATION_JS_MAP)
        }
        sigilSceneCallbackHandler()
    }

    /**
     * Handles Sigil scene callbacks that return no-reload scene or DOM patches.
     */
    fun Route.sigilSceneCallbackHandler() {
        post("/sigil/callback/{callbackId}") {
            val callbackId = call.parameters["callbackId"].orEmpty()
            val result = SigilSceneCallbackRegistry.executeCallback(callbackId)
            val body = SigilJson.encodeToString(
                SigilSceneEventCallbackResponse.serializer(),
                result.response
            )

            call.respondText(
                body,
                ContentType.Application.Json,
                HttpStatusCode.fromValue(result.statusCode)
            )
        }
    }

    /**
     * Loads and responds with a Sigil asset from the library JAR resources.
     * Supports gzip compression and caching for optimal performance.
     */
    suspend fun RoutingContext.respondSigilAsset(name: String) {
        val acceptsGzip = call.request.headers[HttpHeaders.AcceptEncoding]?.contains("gzip") == true
        val result = SigilAssets.loadAsset(name, acceptsGzip)

        if (result == null) {
            call.respondText(
                """{"status":"not-found","asset":"$name"}""",
                ContentType.Application.Json,
                HttpStatusCode.NotFound
            )
            return
        }

        // Add cache headers - assets are immutable, cache for 1 year
        call.response.headers.append(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
        call.response.headers.append(HttpHeaders.Vary, "Accept-Encoding")

        if (result.isCompressed) {
            call.response.headers.append(HttpHeaders.ContentEncoding, "gzip")
        }

        call.respondBytes(result.bytes, ContentType.parse(result.contentType))
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

/**
 * Extension function to add only the Sigil scene callback endpoint.
 */
fun Route.sigilSceneCallbackHandler() = SigilKtorIntegration.run { sigilSceneCallbackHandler() }
