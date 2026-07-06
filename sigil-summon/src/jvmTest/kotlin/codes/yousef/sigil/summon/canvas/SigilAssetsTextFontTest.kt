package codes.yousef.sigil.summon.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SigilAssetsTextFontTest {
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
