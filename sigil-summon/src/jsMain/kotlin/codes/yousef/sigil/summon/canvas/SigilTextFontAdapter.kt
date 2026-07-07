package codes.yousef.sigil.summon.canvas

import io.materia.geometry.BoundingBox2D
import io.materia.geometry.Font as GeometryFont
import io.materia.geometry.Glyph
import io.materia.geometry.GlyphPath
import io.materia.geometry.PathCommand as GeometryPathCommand
import io.materia.geometry.TextGeometryHelper
import io.materia.geometry.TextMetrics
import io.materia.loader.Font as LoaderFont
import io.materia.loader.FontGlyph
import io.materia.loader.FontLoader

internal const val SIGIL_DEFAULT_FONT_URL = "/sigil-default-font.json"

internal object SigilTextFontCache {
    private val loader = FontLoader()
    private val fonts = mutableMapOf<String, GeometryFont>()

    suspend fun load(fontUrl: String?): GeometryFont {
        val requestedUrl = fontUrl?.takeIf { it.isNotBlank() } ?: SIGIL_DEFAULT_FONT_URL
        fonts[requestedUrl]?.let { return it }

        val font = try {
            loader.load(requestedUrl).toGeometryFont()
        } catch (customFailure: Throwable) {
            if (requestedUrl == SIGIL_DEFAULT_FONT_URL) {
                console.warn("Sigil: Failed to load default text font: ${customFailure.message}")
                TextGeometryHelper.createTestFont()
            } else {
                console.warn("Sigil: Failed to load text font $requestedUrl: ${customFailure.message}; using default")
                load(SIGIL_DEFAULT_FONT_URL)
            }
        }

        fonts[requestedUrl] = font
        return font
    }

    fun clear() {
        fonts.clear()
    }
}

internal fun LoaderFont.toGeometryFont(): GeometryFont {
    val convertedGlyphs = glyphs.mapValues { (_, glyph) -> glyph.toGeometryGlyph(boundingBox) }
    return LoadedGeometryFont(
        familyName = familyName,
        unitsPerEm = resolution,
        ascender = boundingBox.yMax,
        descender = boundingBox.yMin,
        glyphs = convertedGlyphs
    )
}

private class LoadedGeometryFont(
    override val familyName: String,
    override val unitsPerEm: Int,
    override val ascender: Float,
    override val descender: Float,
    override val glyphs: Map<Char, Glyph>
) : GeometryFont {
    override val styleName: String = "Regular"
    override val lineGap: Float = 0f

    override fun getGlyph(char: Char): Glyph? =
        glyphs[char] ?: glyphs[char.uppercaseChar()] ?: glyphs['?']

    override fun getKerning(leftChar: Char, rightChar: Char): Float = 0f

    override fun measureText(text: String, size: Float): TextMetrics {
        val scale = size / unitsPerEm
        var width = 0f
        for (char in text) {
            width += (getGlyph(char)?.width ?: 0f) * scale
        }

        val ascent = ascender * scale
        val descent = -descender * scale
        return TextMetrics(
            width = width,
            height = ascent + descent,
            actualBoundingBoxLeft = 0f,
            actualBoundingBoxRight = width,
            actualBoundingBoxAscent = ascent,
            actualBoundingBoxDescent = descent,
            fontBoundingBoxAscent = ascent,
            fontBoundingBoxDescent = descent
        )
    }
}

private fun FontGlyph.toGeometryGlyph(fontBounds: io.materia.loader.FontBoundingBox): Glyph {
    val commands = pathCommands.toGeometryCommands()
    return Glyph(
        unicode = char,
        width = horizontalAdvance,
        leftSideBearing = 0f,
        rightSideBearing = 0f,
        path = GlyphPath(commands, commands.boundsOrFontBounds(fontBounds))
    )
}

private fun String.toGeometryCommands(): List<GeometryPathCommand> {
    if (isBlank()) return emptyList()

    val tokens = trim().split(Regex("\\s+"))
    val commands = mutableListOf<GeometryPathCommand>()
    var index = 0

    fun nextFloat(): Float? = tokens.getOrNull(index++)?.toFloatOrNull()

    while (index < tokens.size) {
        when (tokens[index++]) {
            "m" -> {
                val x = nextFloat() ?: break
                val y = nextFloat() ?: break
                commands += GeometryPathCommand.MoveTo(x, y)
            }
            "l" -> {
                val x = nextFloat() ?: break
                val y = nextFloat() ?: break
                commands += GeometryPathCommand.LineTo(x, y)
            }
            "q" -> {
                val cpx = nextFloat() ?: break
                val cpy = nextFloat() ?: break
                val x = nextFloat() ?: break
                val y = nextFloat() ?: break
                commands += GeometryPathCommand.QuadraticCurveTo(cpx, cpy, x, y)
            }
            "b" -> {
                val cp1x = nextFloat() ?: break
                val cp1y = nextFloat() ?: break
                val cp2x = nextFloat() ?: break
                val cp2y = nextFloat() ?: break
                val x = nextFloat() ?: break
                val y = nextFloat() ?: break
                commands += GeometryPathCommand.BezierCurveTo(cp1x, cp1y, cp2x, cp2y, x, y)
            }
            "z" -> commands += GeometryPathCommand.ClosePath
        }
    }

    return commands
}

private fun List<GeometryPathCommand>.boundsOrFontBounds(
    fontBounds: io.materia.loader.FontBoundingBox
): BoundingBox2D {
    var hasPoint = false
    var minX = 0f
    var minY = 0f
    var maxX = 0f
    var maxY = 0f

    fun include(x: Float, y: Float) {
        if (!hasPoint) {
            minX = x
            maxX = x
            minY = y
            maxY = y
            hasPoint = true
            return
        }
        minX = kotlin.math.min(minX, x)
        maxX = kotlin.math.max(maxX, x)
        minY = kotlin.math.min(minY, y)
        maxY = kotlin.math.max(maxY, y)
    }

    for (command in this) {
        when (command) {
            is GeometryPathCommand.MoveTo -> include(command.x, command.y)
            is GeometryPathCommand.LineTo -> include(command.x, command.y)
            is GeometryPathCommand.QuadraticCurveTo -> {
                include(command.cpx, command.cpy)
                include(command.x, command.y)
            }
            is GeometryPathCommand.BezierCurveTo -> {
                include(command.cp1x, command.cp1y)
                include(command.cp2x, command.cp2y)
                include(command.x, command.y)
            }
            GeometryPathCommand.ClosePath -> Unit
        }
    }

    return if (hasPoint) {
        BoundingBox2D(minX, minY, maxX, maxY)
    } else {
        BoundingBox2D(fontBounds.xMin, fontBounds.yMin, fontBounds.xMax, fontBounds.yMax)
    }
}
