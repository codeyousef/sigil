package codes.yousef.sigil.summon.integration

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Quarkus / JAX-RS integration for Sigil static assets.
 * 
 * This class provides utilities for serving Sigil's hydration JavaScript
 * in Quarkus applications using JAX-RS (RESTEasy).
 * 
 * ## Usage with JAX-RS Resource
 * 
 * ```kotlin
 * @Path("/")
 * class SigilResource {
 *     @GET
 *     @Path("sigil-hydration.js")
 *     @Produces("application/javascript")
 *     fun sigilHydrationJs(@Context request: HttpServletRequest): Response {
 *         return SigilQuarkusIntegration.buildResponse(request, "sigil-hydration.js")
 *     }
 * }
 * ```
 * 
 * ## Usage with Quarkus Reactive Routes
 * 
 * ```kotlin
 * @ApplicationScoped
 * class SigilRoutes {
 *     @Route(path = "/sigil-hydration.js", methods = [Route.HttpMethod.GET])
 *     fun sigilHydrationJs(rc: RoutingContext) {
 *         SigilQuarkusIntegration.handleRoutingContext(rc, "sigil-hydration.js")
 *     }
 * }
 * ```
 */
object SigilQuarkusIntegration {

    /**
     * Builds a JAX-RS Response for a Sigil asset.
     * Handles gzip compression and cache headers automatically.
     *
     * @param request The HTTP request (for Accept-Encoding header)
     * @param assetName The asset to serve (e.g., "sigil-hydration.js")
     * @return A JAX-RS Response, or a 404 response if not found
     */
    fun buildResponseBytes(
        acceptsGzip: Boolean,
        assetName: String
    ): SigilAssets.AssetResult? {
        return SigilAssets.loadAsset(assetName, acceptsGzip)
    }

    /**
     * Serves a Sigil asset using Servlet API (works with Quarkus RESTEasy Classic).
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param assetName The asset to serve (e.g., "sigil-hydration.js")
     * @return true if the asset was served, false if not found
     */
    fun serveAsset(
        request: HttpServletRequest,
        response: HttpServletResponse,
        assetName: String
    ): Boolean {
        val acceptsGzip = request.getHeader("Accept-Encoding")?.contains("gzip") == true
        val result = SigilAssets.loadAsset(assetName, acceptsGzip) ?: return false

        // Set response headers
        response.contentType = result.contentType
        response.setHeader("Cache-Control", "public, max-age=31536000, immutable")
        response.setHeader("Vary", "Accept-Encoding")

        if (result.isCompressed) {
            response.setHeader("Content-Encoding", "gzip")
        }

        response.setContentLength(result.bytes.size)
        response.outputStream.write(result.bytes)
        response.outputStream.flush()

        return true
    }

    /**
     * Serves the hydration JavaScript asset.
     */
    fun serveHydrationJs(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        return serveAsset(request, response, SigilAssets.Assets.HYDRATION_JS)
    }

    /**
     * Serves the hydration JavaScript source map.
     */
    fun serveHydrationJsMap(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        return serveAsset(request, response, SigilAssets.Assets.HYDRATION_JS_MAP)
    }

    /**
     * Gets cache headers for Sigil assets.
     * Use these when building responses manually.
     */
    fun getCacheHeaders(): Map<String, String> {
        return mapOf(
            "Cache-Control" to "public, max-age=31536000, immutable",
            "Vary" to "Accept-Encoding"
        )
    }
}

/**
 * Example Quarkus JAX-RS Resource for Sigil assets.
 * 
 * Copy this into your Quarkus application:
 * 
 * ```kotlin
 * @Path("/")
 * class SigilAssetResource {
 *     
 *     @GET
 *     @Path("sigil-hydration.js")
 *     @Produces("application/javascript")
 *     fun hydrationJs(@Context request: HttpServletRequest, @Context response: HttpServletResponse) {
 *         if (!SigilQuarkusIntegration.serveHydrationJs(request, response)) {
 *             throw WebApplicationException(Response.Status.NOT_FOUND)
 *         }
 *     }
 *     
 *     @GET
 *     @Path("sigil-hydration.js.map")
 *     @Produces("application/json")
 *     fun hydrationJsMap(@Context request: HttpServletRequest, @Context response: HttpServletResponse) {
 *         if (!SigilQuarkusIntegration.serveHydrationJsMap(request, response)) {
 *             throw WebApplicationException(Response.Status.NOT_FOUND)
 *         }
 *     }
 * }
 * ```
 * 
 * Or using Quarkus Reactive with Mutiny:
 * 
 * ```kotlin
 * @Path("/")
 * class SigilAssetResource {
 *     
 *     @GET
 *     @Path("sigil-hydration.js")
 *     @Produces("application/javascript")
 *     fun hydrationJs(@HeaderParam("Accept-Encoding") acceptEncoding: String?): Response {
 *         val acceptsGzip = acceptEncoding?.contains("gzip") == true
 *         val result = SigilQuarkusIntegration.buildResponseBytes(acceptsGzip, "sigil-hydration.js")
 *             ?: return Response.status(Response.Status.NOT_FOUND).build()
 *         
 *         val builder = Response.ok(result.bytes, result.contentType)
 *             .header("Cache-Control", "public, max-age=31536000, immutable")
 *             .header("Vary", "Accept-Encoding")
 *         
 *         if (result.isCompressed) {
 *             builder.header("Content-Encoding", "gzip")
 *         }
 *         
 *         return builder.build()
 *     }
 * }
 * ```
 */
// Note: This is documentation only - actual resource must be in user's application
