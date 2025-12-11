package codes.yousef.sigil.summon.integration

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Spring Boot / Spring MVC integration for Sigil static assets.
 * 
 * This class provides utilities for serving Sigil's hydration JavaScript
 * in Spring-based applications. It can be used with Spring MVC controllers
 * or Spring WebFlux handlers.
 * 
 * ## Usage with Spring MVC Controller
 * 
 * ```kotlin
 * @RestController
 * class SigilController {
 *     @GetMapping("/sigil-hydration.js")
 *     fun sigilHydrationJs(request: HttpServletRequest, response: HttpServletResponse) {
 *         SigilSpringIntegration.serveAsset(request, response, "sigil-hydration.js")
 *     }
 * }
 * ```
 * 
 * ## Usage with Spring Boot Auto-Configuration
 * 
 * Register the [SigilWebMvcConfigurer] bean to automatically serve assets:
 * 
 * ```kotlin
 * @Configuration
 * class SigilConfig {
 *     @Bean
 *     fun sigilWebMvcConfigurer() = SigilWebMvcConfigurer()
 * }
 * ```
 */
object SigilSpringIntegration {

    /**
     * Serves a Sigil asset to the HTTP response.
     * Handles gzip compression and cache headers automatically.
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
}

/**
 * Spring WebMVC configurer that registers Sigil asset handlers.
 * 
 * Add this as a bean in your Spring configuration:
 * 
 * ```kotlin
 * @Configuration
 * class SigilConfig {
 *     @Bean
 *     fun sigilWebMvcConfigurer() = SigilWebMvcConfigurer()
 * }
 * ```
 * 
 * This will automatically serve:
 * - `/sigil-hydration.js` - The hydration JavaScript bundle
 * - `/sigil-hydration.js.map` - Source map for debugging
 */
class SigilWebMvcConfigurer {
    
    /**
     * Creates a controller that handles Sigil asset requests.
     * Register this bean in your Spring context.
     */
    fun createController(): Any {
        return object {
            fun handleHydrationJs(request: HttpServletRequest, response: HttpServletResponse) {
                if (!SigilSpringIntegration.serveHydrationJs(request, response)) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND)
                }
            }

            fun handleHydrationJsMap(request: HttpServletRequest, response: HttpServletResponse) {
                if (!SigilSpringIntegration.serveHydrationJsMap(request, response)) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND)
                }
            }
        }
    }
}

/**
 * Example Spring MVC Controller for Sigil assets.
 * 
 * Copy this into your Spring application and annotate with @RestController:
 * 
 * ```kotlin
 * @RestController
 * class SigilAssetController {
 *     @GetMapping("/sigil-hydration.js")
 *     fun hydrationJs(request: HttpServletRequest, response: HttpServletResponse) {
 *         SigilSpringIntegration.serveHydrationJs(request, response)
 *     }
 *     
 *     @GetMapping("/sigil-hydration.js.map")
 *     fun hydrationJsMap(request: HttpServletRequest, response: HttpServletResponse) {
 *         SigilSpringIntegration.serveHydrationJsMap(request, response)
 *     }
 * }
 * ```
 */
// Note: This is documentation only - actual controller must be in user's application
