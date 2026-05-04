package codes.yousef.sigil.summon.canvas

import io.materia.loader.AssetResolver
import io.materia.renderer.TextureFilter
import io.materia.renderer.TextureFormat
import io.materia.renderer.TextureWrap
import io.materia.texture.Texture2D
import kotlinx.browser.document
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal data class SigilTextureOptions(
    val flipY: Boolean = false,
    val generateMipmaps: Boolean = true,
    val anisotropy: Float = 4f,
    val magFilter: TextureFilter = TextureFilter.LINEAR,
    val minFilter: TextureFilter = TextureFilter.LINEAR_MIPMAP_LINEAR,
    val wrapS: TextureWrap = TextureWrap.REPEAT,
    val wrapT: TextureWrap = TextureWrap.REPEAT,
    val format: TextureFormat = TextureFormat.RGBA8
)

internal object SigilBrowserTextureLoader {
    fun isBrowserImageApiAvailable(): Boolean =
        js(
            "typeof globalThis !== 'undefined' && " +
                "typeof globalThis.Image === 'function' && " +
                "typeof globalThis.document !== 'undefined'"
        ).unsafeCast<Boolean>()

    suspend fun load(
        uri: String,
        mimeType: String?,
        assetResolver: AssetResolver,
        options: SigilTextureOptions = SigilTextureOptions()
    ): Texture2D {
        check(isBrowserImageApiAvailable()) {
            "Browser image and canvas APIs are required to decode glTF textures"
        }

        val dataUri = if (uri.startsWith("data:", ignoreCase = true)) {
            uri
        } else {
            val bytes = assetResolver.load(uri, null)
            bytes.toImageDataUri(SigilGltfMetadata.imageMimeType(uri, mimeType))
        }

        val image = decodeImageData(dataUri)
        val pixels = if (options.flipY) {
            flipImageY(image.data, image.width, image.height)
        } else {
            image.data
        }

        return Texture2D.fromImageData(
            width = image.width,
            height = image.height,
            data = pixels,
            format = options.format
        ).apply {
            name = textureName(uri)
            flipY = options.flipY
            generateMipmaps = options.generateMipmaps
            anisotropy = options.anisotropy
            magFilter = options.magFilter
            minFilter = options.minFilter
            wrapS = options.wrapS
            wrapT = options.wrapT
            needsUpdate = true
        }
    }

    private suspend fun decodeImageData(src: String): BrowserDecodedImage =
        suspendCoroutine { continuation ->
            val image: dynamic = js("new Image()")
            image.crossOrigin = "anonymous"
            image.onload = {
                try {
                    val width = (image.width as Number).toInt()
                    val height = (image.height as Number).toInt()
                    val canvas: dynamic = document.createElement("canvas")
                    canvas.width = width
                    canvas.height = height

                    val context = canvas.getContext("2d")
                    if (context == null) {
                        continuation.resumeWithException(IllegalStateException("2D canvas context unavailable"))
                    } else {
                        context.drawImage(image, 0, 0)
                        val imageData = context.getImageData(0, 0, width, height)
                        val data: dynamic = imageData.data
                        val length = (data.length as Number).toInt()
                        val bytes = ByteArray(length) { index ->
                            (data[index] as Number).toInt().toByte()
                        }
                        continuation.resume(BrowserDecodedImage(width, height, bytes))
                    }
                } catch (t: Throwable) {
                    continuation.resumeWithException(t)
                }
            }
            image.onerror = { event: dynamic ->
                continuation.resumeWithException(IllegalArgumentException("Failed to decode image: $event"))
            }
            image.src = src
        }

    private fun flipImageY(data: ByteArray, width: Int, height: Int): ByteArray {
        val bytesPerRow = width * 4
        val flipped = ByteArray(data.size)
        for (y in 0 until height) {
            val sourceOffset = y * bytesPerRow
            val targetOffset = (height - y - 1) * bytesPerRow
            data.copyInto(flipped, targetOffset, sourceOffset, sourceOffset + bytesPerRow)
        }
        return flipped
    }

    private fun textureName(uri: String): String =
        uri.substringBefore("?")
            .substringBefore("#")
            .substringAfterLast("/")
            .takeIf { it.isNotBlank() && !it.startsWith("data:", ignoreCase = true) }
            ?: "gltf-base-color"

    @OptIn(ExperimentalEncodingApi::class)
    private fun ByteArray.toImageDataUri(mimeType: String): String =
        "data:$mimeType;base64,${Base64.Default.encode(this)}"
}

private data class BrowserDecodedImage(
    val width: Int,
    val height: Int,
    val data: ByteArray
)
