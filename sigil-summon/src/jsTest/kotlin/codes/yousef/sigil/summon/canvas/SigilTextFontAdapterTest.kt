package codes.yousef.sigil.summon.canvas

import io.materia.loader.Font
import io.materia.loader.FontBoundingBox
import io.materia.loader.FontGlyph
import io.materia.geometry.PathCommand
import io.materia.geometry.TextGeometry
import io.materia.geometry.TextOptions
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun toGeometryFont_parsesTypefaceJsonGlyphMap() {
        val geometryFont = """
            {
              "familyName": "Test Typeface",
              "resolution": 1000,
              "ascender": 800,
              "descender": -200,
              "boundingBox": { "xMin": 0, "xMax": 800, "yMin": -200, "yMax": 800 },
              "glyphs": {
                "A": { "ha": 700, "o": "m 0 0 l 350 700 l 700 0 z" },
                "B": { "ha": 650, "o": "m 0 0 l 0 700 l 500 520 l 500 180 z" },
                "?": { "ha": 500, "o": "m 0 0 l 500 0 l 500 500 l 0 500 z" }
              }
            }
        """.trimIndent().toGeometryFont()

        assertNotNull(geometryFont.getGlyph('A'))
        assertNotNull(geometryFont.getGlyph('B'))
        assertTrue(geometryFont.measureText("AB", 1f).width > 0f)

        val geometry = TextGeometry("AB", geometryFont, TextOptions(size = 1f, height = 0.02f))
        assertTrue((geometry.getAttribute("position")?.count ?: 0) > 0)
    }

    @Test
    fun toGeometryFont_usesTypefaceEndpointFirstCurveOperands() {
        val geometryFont = """
            {
              "familyName": "Curve Test",
              "resolution": 1000,
              "ascender": 800,
              "descender": -200,
              "boundingBox": { "xMin": 0, "xMax": 800, "yMin": -200, "yMax": 800 },
              "glyphs": {
                "Q": { "ha": 700, "o": "m 0 0 q 100 200 300 400 b 500 600 700 800 900 1000 z" }
              }
            }
        """.trimIndent().toGeometryFont()

        val commands = geometryFont.getGlyph('Q')!!.path.commands
        val quadratic = commands.filterIsInstance<PathCommand.QuadraticCurveTo>().single()
        val cubic = commands.filterIsInstance<PathCommand.BezierCurveTo>().single()

        assertEquals(300f, quadratic.cpx)
        assertEquals(400f, quadratic.cpy)
        assertEquals(100f, quadratic.x)
        assertEquals(200f, quadratic.y)
        assertEquals(700f, cubic.cp1x)
        assertEquals(800f, cubic.cp1y)
        assertEquals(900f, cubic.cp2x)
        assertEquals(1000f, cubic.cp2y)
        assertEquals(500f, cubic.x)
        assertEquals(600f, cubic.y)
    }

    @Test
    fun fallbackBlockFont_createsControlLabelTextGeometry() {
        val geometryFont = SigilDefaultBlockFont.font

        assertEquals("Sigil Block", geometryFont.familyName)
        assertNotNull(geometryFont.getGlyph('R'))
        assertNotNull(geometryFont.getGlyph('r'))
        assertNotNull(geometryFont.getGlyph('1'))
        assertTrue(geometryFont.measureText("ROUTE TRUCK A", 1f).width > 0f)

        val geometry = TextGeometry(
            "ROUTE TRUCK A RETURN BIN RESET SHIFT FOCUSED P1",
            geometryFont,
            TextOptions(size = 1f, height = 0.02f)
        )
        assertTrue((geometry.getAttribute("position")?.count ?: 0) > 0)
    }
}
