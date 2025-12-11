package io.github.codeyousef.sigil.sample.summon

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import codes.yousef.summon.core.PlatformRendererProvider
import io.github.codeyousef.sigil.summon.canvas.MateriaCanvas
import io.github.codeyousef.sigil.summon.context.SigilSummonContext

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    routing {
        get("/") {
            call.respondHtml {
                renderSamplePage()
            }
        }

        // Serve the compiled JS bundle
        get("/static/sample-summon.js") {
            // The compiled Webpack bundle should be placed at this path
            // Use: ./gradlew :sample-summon:jsBrowserProductionWebpack
            call.respondText(
                contentType = ContentType.Application.JavaScript,
                text = "// Webpack compiled bundle would be served here"
            )
        }
    }
}

private fun HTML.renderSamplePage() {
    head {
        meta(charset = "UTF-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1.0")
        title("Sigil - Summon Sample")
        style {
            unsafe {
                raw("""
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
                        min-height: 100vh;
                        color: #ffffff;
                    }
                    
                    .container {
                        max-width: 1200px;
                        margin: 0 auto;
                        padding: 2rem;
                    }
                    
                    .header {
                        text-align: center;
                        margin-bottom: 2rem;
                    }
                    
                    .header h1 {
                        font-size: 2.5rem;
                        background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        background-clip: text;
                    }
                    
                    .header p {
                        color: #8892b0;
                        margin-top: 0.5rem;
                    }
                    
                    .canvas-container {
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        background: rgba(255, 255, 255, 0.05);
                        border-radius: 12px;
                        padding: 1rem;
                        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                    }
                    
                    #sigil-scene {
                        border-radius: 8px;
                        display: block;
                    }
                    
                    .info {
                        margin-top: 2rem;
                        text-align: center;
                        color: #8892b0;
                    }
                    
                    .info code {
                        background: rgba(255, 255, 255, 0.1);
                        padding: 0.2rem 0.5rem;
                        border-radius: 4px;
                        font-family: 'Fira Code', monospace;
                    }
                """.trimIndent())
            }
        }
    }
    body {
        div(classes = "container") {
            div(classes = "header") {
                h1 { +"Sigil 3D Scene" }
                p { +"Server-Side Rendered with Summon + Materia" }
            }

            div(classes = "canvas-container") {
                // Render the MateriaCanvas with SSR
                // This generates the canvas element with serialized scene data
                val sceneHtml = renderMateriaCanvasToHtml()
                unsafe { raw(sceneHtml) }
            }

            div(classes = "info") {
                p {
                    +"Scene data serialized to "
                    unsafe { raw("<code>data-sigil-scene</code>") }
                    +" attribute for client hydration"
                }
            }
        }

        // Client-side hydration script
        script(src = "/static/sample-summon.js") {}
    }
}

/**
 * Renders the MateriaCanvas to HTML string for SSR.
 * Uses Summon's PlatformRenderer to generate the HTML.
 */
private fun renderMateriaCanvasToHtml(): String {
    // Create a server-side context
    val context = SigilSummonContext.createServerContext()
    
    // Run the composable within the context to populate nodes
    SigilSummonContext.withContext(context) {
        // Call the sample scene composable which registers nodes
        Sample3DScene()
    }
    
    // Build the scene from collected nodes
    val scene = context.buildScene()

    // Build the canvas HTML with embedded scene data
    return buildString {
        append("<canvas ")
        append("id=\"sigil-scene\" ")
        append("width=\"800\" ")
        append("height=\"600\" ")
        append("data-sigil-scene=\"")
        // Escape the JSON for HTML attribute
        append(kotlinx.serialization.json.Json.encodeToString(
            io.github.codeyousef.sigil.schema.SigilScene.serializer(),
            scene
        ).replace("\"", "&quot;"))
        append("\">")
        append("</canvas>")
    }
}
