package codes.yousef.sigil.summon.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SigilAssetsTextFontTest {
    @Test
    fun hydrationBundleContainsCanvasRuntimePatchContracts() {
        val result = SigilAssets.loadAsset(SigilAssets.Assets.HYDRATION_JS)

        assertNotNull(result)
        assertEquals(SigilAssets.ContentTypes.JAVASCRIPT, result.contentType)
        val script = result.bytes.decodeToString()
        assertTrue(script.contains("screenLayer"))
        assertTrue(script.contains("modelUrl"))
        assertTrue(script.contains("interactionEnabled"))
        assertTrue(script.contains("pointercancel"))
        assertTrue(script.contains("lostpointercapture"))
        assertTrue(script.contains("setPointerCapture"))
        assertTrue(script.contains("cancelled"))
        assertTrue(script.contains("pointerenter"))
        assertTrue(script.contains("pointerleave"))
    }

    @Test
    fun loadAsset_defaultFont_returnsJsonAsset() {
        val result = SigilAssets.loadAsset(SigilAssets.Assets.DEFAULT_FONT_JSON)

        assertNotNull(result)
        assertEquals(SigilAssets.ContentTypes.JSON, result.contentType)
        assertTrue(result.bytes.decodeToString().contains("\"glyphs\""))
        assertTrue(!result.isCompressed)
    }

    @Test
    fun loadAsset_defaultFont_supportsGzip() {
        val result = SigilAssets.loadAsset(SigilAssets.Assets.DEFAULT_FONT_JSON, acceptsGzip = true)

        assertNotNull(result)
        assertEquals(SigilAssets.ContentTypes.JSON, result.contentType)
        assertTrue(result.isCompressed)
        assertTrue(result.bytes.isNotEmpty())
    }
}
