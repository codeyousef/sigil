package codes.yousef.sigil.summon.canvas

import io.materia.loader.Font
import io.materia.loader.FontBoundingBox
import io.materia.loader.FontGlyph
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SigilTextFontAdapterTest {
    @Test
    fun toGeometryFont_convertsLoaderGlyphs() {
        val loaderFont = Font(
            familyName = "Test",
            resolution = 1000,
            boundingBox = FontBoundingBox(0f, 700f, -200f, 800f),
            glyphs = mapOf(
                'A' to FontGlyph(
                    char = 'A',
                    horizontalAdvance = 700f,
                    pathCommands = "m 0 0 l 300 700 l 600 0 l 480 0 l 410 180 l 190 180 l 120 0 l 0 0"
                )
            )
        )

        val geometryFont = loaderFont.toGeometryFont()

        assertNotNull(geometryFont.getGlyph('A'))
        assertTrue(geometryFont.glyphs.containsKey('A'))
        assertTrue(geometryFont.measureText("A", 1f).width > 0f)
    }
}
