package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.AdaptiveResolutionData
import codes.yousef.sigil.schema.GeometryParams
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.MeshData
import codes.yousef.sigil.schema.RendererPreference
import codes.yousef.sigil.schema.SceneSettings
import codes.yousef.sigil.schema.ScreenAnchor
import codes.yousef.sigil.schema.ScreenLayerData
import codes.yousef.sigil.schema.ScreenLayoutData
import codes.yousef.sigil.schema.SigilScene
import codes.yousef.sigil.schema.TextData
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SigilHydratorScreenLayerTest {
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun browserHydratorInitializesAndRendersAnOrthographicLayer(): Promise<Unit> = GlobalScope.promise {
        if (!browserCanvasAvailable()) return@promise
        val host = document.createElement("div") as HTMLElement
        host.style.width = "320px"
        host.style.height = "180px"
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.style.width = "100%"
        canvas.style.height = "100%"
        host.appendChild(canvas)
        document.body?.appendChild(host)

        val scene = SigilScene(
            rootNodes = listOf(
                ScreenLayerData(
                    id = "hud",
                    desktop = ScreenLayoutData(ScreenAnchor.TOP_LEFT, 12f, 12f),
                    children = listOf(
                        MeshData(
                            id = "hud-backplate",
                            position = listOf(50f, -20f, 0f),
                            geometryType = GeometryType.PLANE,
                            geometryParams = GeometryParams(width = 100f, height = 40f),
                            materialColor = 0xFF111820.toInt()
                        ),
                        TextData(
                            id = "hud-title",
                            position = listOf(12f, -12f, 1f),
                            text = "HUD",
                            size = 14f,
                            depth = 0.01f,
                            curveSegments = 4
                        )
                    )
                )
            ),
            settings = SceneSettings(
                rendererPreference = RendererPreference.WEBGL,
                adaptiveResolution = AdaptiveResolutionData(
                    minimumDpr = 0.75f,
                    maximumDpr = 1.25f
                )
            )
        )
        val hydrator = SigilHydrator(canvas, scene)

        try {
            hydrator.initialize()
            hydrator.startRenderLoop()
            delay(50)

            assertEquals(1, hydrator.screenLayerCountForTesting())
            assertEquals(1, hydrator.hydratedTextMeshCountForTesting())
            assertTrue(canvas.width > 0)
            assertTrue(canvas.height > 0)
            assertTrue(hydrator.renderScaleForTesting() in 0.75f..1.25f)
            canvasHasRenderedPixels(canvas)?.let { assertTrue(it) }
        } finally {
            hydrator.dispose()
            host.remove()
        }
    }

    private fun browserCanvasAvailable(): Boolean = js(
        "typeof document !== 'undefined' && typeof HTMLCanvasElement !== 'undefined'"
    ) as Boolean

    private fun canvasHasRenderedPixels(canvas: HTMLCanvasElement): Boolean? {
        val context = canvas.getContext("webgl2") ?: canvas.getContext("webgl") ?: return null
        val gl = context.asDynamic()
        val pixel = Uint8Array(4)
        gl.readPixels(canvas.width / 2, canvas.height / 2, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel)
        return (0 until pixel.length).any { index ->
            (pixel.asDynamic()[index] as Number).toInt() != 0
        }
    }
}
