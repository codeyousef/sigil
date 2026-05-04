package codes.yousef.sigil.summon.canvas

import io.materia.loader.AssetResolver
import io.materia.renderer.TextureFilter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

class SigilBrowserTextureLoaderTest {

    @OptIn(DelicateCoroutinesApi::class, ExperimentalEncodingApi::class)
    @Test
    fun load_decodesTexturedGltfImageBytesWithoutMateriaTextureLoader(): Promise<Unit> =
        GlobalScope.promise {
            if (!SigilBrowserTextureLoader.isBrowserImageApiAvailable()) return@promise

            val resolver = MapAssetResolver(
                mapOf("textures/baseColor.png" to Base64.Default.decode(ONE_PIXEL_PNG))
            )

            val texture = SigilBrowserTextureLoader.load(
                uri = "textures/baseColor.png",
                mimeType = "image/png",
                assetResolver = resolver
            )

            assertEquals(1, texture.width)
            assertEquals(1, texture.height)
            assertEquals("baseColor.png", texture.name)
            assertEquals(TextureFilter.LINEAR_MIPMAP_LINEAR, texture.minFilter)
            assertTrue(texture.hasData())
            assertEquals(4, texture.getDataSize())
        }

    private class MapAssetResolver(
        private val assets: Map<String, ByteArray>
    ) : AssetResolver {
        override suspend fun load(uri: String, basePath: String?): ByteArray =
            assets[uri] ?: error("Missing fixture asset $uri")
    }

    private companion object {
        private const val ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
    }
}
