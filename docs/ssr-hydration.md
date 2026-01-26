# Server-Side Rendering & Hydration

Sigil supports server-side rendering (SSR) with client-side hydration for screen-space effects. This guide covers how to set up SSR with popular JVM web frameworks.

## Overview

When using Sigil with SSR frameworks like Summon:

1. **Server (JVM)**: Renders HTML with embedded effect data and shader code
2. **Client (Browser)**: Loads `sigil-hydration.js` which hydrates the effects
3. **Rendering**: WebGPU (preferred) or WebGL (fallback) renders the shaders

## Quick Start

### Add Dependencies

```kotlin
dependencies {
    implementation("codes.yousef.sigil:sigil-summon:0.3.0.0")
    
    // Plus your web framework of choice:
    // implementation("io.ktor:ktor-server-core:3.0.3")           // Ktor 3.x
    // implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")  // Spring Boot
    // implementation("io.quarkus:quarkus-resteasy-reactive:3.6.0") // Quarkus
}
```

---

## Ktor Integration

The simplest integration - just add `sigilStaticAssets()` to your routing:

```kotlin
import codes.yousef.sigil.summon.integration.sigilStaticAssets
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.module() {
    routing {
        sigilStaticAssets()  // Serves /sigil-hydration.js
        
        // Your other routes...
    }
}
```

**That's it!** The hydration script is now served at `/sigil-hydration.js`.

---

## Spring Boot Integration

Create a controller to serve the hydration assets:

```kotlin
import codes.yousef.sigil.summon.integration.SigilSpringIntegration
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SigilAssetController {
    
    @GetMapping("/sigil-hydration.js")
    fun hydrationJs(request: HttpServletRequest, response: HttpServletResponse) {
        SigilSpringIntegration.serveHydrationJs(request, response)
    }
    
    @GetMapping("/sigil-hydration.js.map")
    fun hydrationJsMap(request: HttpServletRequest, response: HttpServletResponse) {
        SigilSpringIntegration.serveHydrationJsMap(request, response)
    }
}
```

---

## Quarkus Integration

Create a JAX-RS resource to serve the hydration assets:

```kotlin
import codes.yousef.sigil.summon.integration.SigilQuarkusIntegration
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context

@Path("/")
class SigilAssetResource {
    
    @GET
    @Path("sigil-hydration.js")
    @Produces("application/javascript")
    fun hydrationJs(
        @Context request: HttpServletRequest,
        @Context response: HttpServletResponse
    ) {
        SigilQuarkusIntegration.serveHydrationJs(request, response)
    }
    
    @GET
    @Path("sigil-hydration.js.map")
    @Produces("application/json")
    fun hydrationJsMap(
        @Context request: HttpServletRequest,
        @Context response: HttpServletResponse
    ) {
        SigilQuarkusIntegration.serveHydrationJsMap(request, response)
    }
}
```

### Quarkus Reactive Alternative

For reactive endpoints without servlet dependency:

```kotlin
import codes.yousef.sigil.summon.integration.SigilQuarkusIntegration
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response

@Path("/")
class SigilAssetResource {
    
    @GET
    @Path("sigil-hydration.js")
    @Produces("application/javascript")
    fun hydrationJs(@HeaderParam("Accept-Encoding") acceptEncoding: String?): Response {
        val acceptsGzip = acceptEncoding?.contains("gzip") == true
        val result = SigilQuarkusIntegration.buildResponseBytes(acceptsGzip, "sigil-hydration.js")
            ?: return Response.status(Response.Status.NOT_FOUND).build()
        
        val builder = Response.ok(result.bytes, result.contentType)
            .header("Cache-Control", "public, max-age=31536000, immutable")
            .header("Vary", "Accept-Encoding")
        
        if (result.isCompressed) {
            builder.header("Content-Encoding", "gzip")
        }
        
        return builder.build()
    }
}
```

---

## Generic Servlet Integration

For any Servlet-based framework, use `SigilAssets` directly:

```kotlin
import codes.yousef.sigil.summon.integration.SigilAssets

class SigilServlet : HttpServlet() {
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val acceptsGzip = req.getHeader("Accept-Encoding")?.contains("gzip") == true
        val result = SigilAssets.loadAsset("sigil-hydration.js", acceptsGzip)
        
        if (result == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND)
            return
        }
        
        resp.contentType = result.contentType
        resp.setHeader("Cache-Control", "public, max-age=31536000, immutable")
        if (result.isCompressed) {
            resp.setHeader("Content-Encoding", "gzip")
        }
        resp.outputStream.write(result.bytes)
    }
}
```

---

## How It Works

### Asset Serving

All integrations use the shared `SigilAssets` class which:

| Feature | Description |
|---------|-------------|
| **JAR Resources** | Loads `sigil-hydration.js` from the library JAR |
| **Caching** | In-memory cache for raw and compressed assets |
| **Compression** | Automatic gzip when client supports it |
| **Headers** | Sets cache headers (1 year immutable) |

### Hydration Process

1. Page loads with `<canvas data-sigil-effects='{"id":"...","effects":[...]}'>` elements
2. `sigil-hydration.js` executes on DOMContentLoaded
3. Auto-detects all Sigil effect canvases via `canvas[data-sigil-effects]` selector
4. Parses effect data directly from the `data-sigil-effects` attribute (contains full `EffectComposerData` JSON)
5. Checks browser capabilities (WebGPU → WebGL → CSS fallback)
6. Initializes appropriate renderer and starts animation loop

### HTML Output Structure

When `SigilEffectCanvas` renders on the server, it produces:

```html
<div id="my-canvas-container" style="width: 100%; height: 400px; position: relative;">
  <canvas 
    id="my-canvas" 
    data-sigil-effects='{"id":"my-canvas","effects":[{"id":"aurora","name":"Aurora Background","fragmentShader":"...WGSL...","glslFragmentShader":"...GLSL...","uniforms":{...}}]}'
    data-sigil-config='{"respectDevicePixelRatio":true,"powerPreference":"high-performance",...}'
    data-sigil-interactions='{"enableMouse":true,...}'
    style="width: 100%; height: 100%; display: block;">
  </canvas>
  <script type="module">/* Hydration loader */</script>
  <noscript><!-- Fallback content --></noscript>
</div>
```

The `data-sigil-effects` attribute contains the complete serialized effect data including:
- Effect ID and name
- WGSL fragment shader code (for WebGPU)
- GLSL fragment shader code (for WebGL fallback)
- Uniform values
- Blend mode and opacity settings

---

## Client-Side API

The hydration bundle exposes `window.SigilEffectHydrator`:

```javascript
// Manually hydrate a specific canvas
SigilEffectHydrator.hydrate("my-canvas-id");

// Check browser capabilities
SigilEffectHydrator.isWebGPUAvailable();   // true/false
SigilEffectHydrator.isWebGLAvailable();    // true/false
SigilEffectHydrator.getAvailableRenderer(); // "webgpu" | "webgl" | "css"
```

Also available under `window.Sigil.EffectHydrator`.

---

## Browser Support

| Browser | Renderer | Notes |
|---------|----------|-------|
| Chrome 113+ | WebGPU | Full support |
| Edge 113+ | WebGPU | Full support |
| Firefox | WebGL | Automatic fallback |
| Safari 17+ | WebGPU | Full support |
| Older browsers | CSS | Fallback gradient/static content |

---

## See Also

- [Summon API Reference](api-reference/summon.md) - SigilEffectCanvas and effect components
- [Schema API Reference](api-reference/schema.md) - Effect data structures
