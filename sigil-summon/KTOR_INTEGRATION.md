# Sigil Ktor Integration

This module provides Ktor integration for serving Sigil's client-side hydration assets.

## Usage

Add the `sigilStaticAssets()` route to your Ktor application:

```kotlin
import codes.yousef.sigil.summon.integration.sigilStaticAssets
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.module() {
    routing {
        sigilStaticAssets()  // Serves /sigil-hydration.js
        
        // Your other routes
        get("/") {
            // Your page with Sigil effects
        }
    }
}
```

## What It Does

The `sigilStaticAssets()` function registers a route that serves the Sigil hydration JavaScript file directly from the library JAR at:

- `/sigil-hydration.js` - Client-side JavaScript for hydrating Sigil effect canvases

## Features

- **Zero Configuration**: Just call `sigilStaticAssets()` - no need to manually copy files
- **Automatic Compression**: Serves gzip-compressed assets when the client supports it
- **Caching**: Sets appropriate cache headers for optimal performance
- **Embedded in JAR**: The JavaScript is packaged inside the library JAR, no external files needed

## Example

```kotlin
import codes.yousef.sigil.summon.integration.sigilStaticAssets
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.html.*
import kotlinx.html.*

fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            sigilStaticAssets()
            
            get("/") {
                call.respondHtml {
                    head {
                        script(src = "/sigil-hydration.js") {}
                    }
                    body {
                        canvas {
                            attributes["data-sigil-effects"] = """{"effects":[]}"""
                        }
                    }
                }
            }
        }
    }.start(wait = true)
}
```

## Advanced Usage

If you need direct access to the asset serving function:

```kotlin
import codes.yousef.sigil.summon.integration.SigilKtorIntegration.respondSigilAsset
import io.ktor.http.*

routing {
    get("/custom/path/sigil.js") {
        call.respondSigilAsset("sigil-hydration.js", ContentType.Application.JavaScript)
    }
}
```

## Dependencies

This integration requires Ktor Server Core. Add it to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("codes.yousef:sigil-summon:0.2.1.0")
}
```

## TODO

Currently, `sigil-hydration.js` is a placeholder that logs initialization messages. The full implementation will:

1. Compile the Kotlin/JS `sigil-summon` code to JavaScript
2. Package the compiled JS into the JVM JAR during build
3. Provide complete client-side hydration of Sigil effect canvases

For production use, you'll need to build the full JS bundle and replace the placeholder file.
