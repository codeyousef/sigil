package codes.yousef.sigil.summon.canvas

import io.materia.geometry.BoundingBox2D
import io.materia.geometry.Font as GeometryFont
import io.materia.geometry.Glyph
import io.materia.geometry.GlyphPath
import io.materia.geometry.PathCommand as GeometryPathCommand
import io.materia.geometry.TextMetrics
import io.materia.loader.AssetResolver
import io.materia.loader.Font as LoaderFont
import io.materia.loader.FontGlyph
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal const val SIGIL_DEFAULT_FONT_URL = "/sigil-default-font.json"

internal object SigilTextFontCache {
    private val resolver = AssetResolver.default()
    private val fonts = mutableMapOf<String, GeometryFont>()

    suspend fun load(fontUrl: String?): GeometryFont {
        val requestedUrl = fontUrl?.takeIf { it.isNotBlank() } ?: SIGIL_DEFAULT_FONT_URL
        fonts[requestedUrl]?.let { return it }

        val font =
            if (requestedUrl == SIGIL_DEFAULT_FONT_URL) {
                SigilDefaultBlockFont.font
            } else {
                try {
                    resolver.load(requestedUrl).decodeToString().toGeometryFont()
                } catch (customFailure: Throwable) {
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

internal object SigilDefaultBlockFont {
    private const val UNITS_PER_EM = 1000
    private const val ASCENDER = 760f
    private const val DESCENDER = -140f
    private const val CELL = 100f
    private const val FILL = 82f
    private const val ADVANCE = 620f
    private const val SPACE_ADVANCE = 360f

    val font: GeometryFont by lazy {
        LoadedGeometryFont(
            familyName = "Sigil Block",
            unitsPerEm = UNITS_PER_EM,
            ascender = ASCENDER,
            descender = DESCENDER,
            glyphs = glyphRows.mapValues { (char, rows) -> char.toBlockGlyph(rows) }
        )
    }

    private val glyphRows = mapOf(
        ' ' to emptyList(),
        'A' to rows(
            "01110",
            "10001",
            "10001",
            "11111",
            "10001",
            "10001",
            "10001"
        ),
        'B' to rows(
            "11110",
            "10001",
            "10001",
            "11110",
            "10001",
            "10001",
            "11110"
        ),
        'C' to rows(
            "01111",
            "10000",
            "10000",
            "10000",
            "10000",
            "10000",
            "01111"
        ),
        'D' to rows(
            "11110",
            "10001",
            "10001",
            "10001",
            "10001",
            "10001",
            "11110"
        ),
        'E' to rows(
            "11111",
            "10000",
            "10000",
            "11110",
            "10000",
            "10000",
            "11111"
        ),
        'F' to rows(
            "11111",
            "10000",
            "10000",
            "11110",
            "10000",
            "10000",
            "10000"
        ),
        'G' to rows(
            "01111",
            "10000",
            "10000",
            "10011",
            "10001",
            "10001",
            "01111"
        ),
        'H' to rows(
            "10001",
            "10001",
            "10001",
            "11111",
            "10001",
            "10001",
            "10001"
        ),
        'I' to rows(
            "11111",
            "00100",
            "00100",
            "00100",
            "00100",
            "00100",
            "11111"
        ),
        'J' to rows(
            "00111",
            "00010",
            "00010",
            "00010",
            "00010",
            "10010",
            "01100"
        ),
        'K' to rows(
            "10001",
            "10010",
            "10100",
            "11000",
            "10100",
            "10010",
            "10001"
        ),
        'L' to rows(
            "10000",
            "10000",
            "10000",
            "10000",
            "10000",
            "10000",
            "11111"
        ),
        'M' to rows(
            "10001",
            "11011",
            "10101",
            "10101",
            "10001",
            "10001",
            "10001"
        ),
        'N' to rows(
            "10001",
            "11001",
            "10101",
            "10011",
            "10001",
            "10001",
            "10001"
        ),
        'O' to rows(
            "01110",
            "10001",
            "10001",
            "10001",
            "10001",
            "10001",
            "01110"
        ),
        'P' to rows(
            "11110",
            "10001",
            "10001",
            "11110",
            "10000",
            "10000",
            "10000"
        ),
        'Q' to rows(
            "01110",
            "10001",
            "10001",
            "10001",
            "10101",
            "10010",
            "01101"
        ),
        'R' to rows(
            "11110",
            "10001",
            "10001",
            "11110",
            "10100",
            "10010",
            "10001"
        ),
        'S' to rows(
            "01111",
            "10000",
            "10000",
            "01110",
            "00001",
            "00001",
            "11110"
        ),
        'T' to rows(
            "11111",
            "00100",
            "00100",
            "00100",
            "00100",
            "00100",
            "00100"
        ),
        'U' to rows(
            "10001",
            "10001",
            "10001",
            "10001",
            "10001",
            "10001",
            "01110"
        ),
        'V' to rows(
            "10001",
            "10001",
            "10001",
            "10001",
            "10001",
            "01010",
            "00100"
        ),
        'W' to rows(
            "10001",
            "10001",
            "10001",
            "10101",
            "10101",
            "11011",
            "10001"
        ),
        'X' to rows(
            "10001",
            "10001",
            "01010",
            "00100",
            "01010",
            "10001",
            "10001"
        ),
        'Y' to rows(
            "10001",
            "10001",
            "01010",
            "00100",
            "00100",
            "00100",
            "00100"
        ),
        'Z' to rows(
            "11111",
            "00001",
            "00010",
            "00100",
            "01000",
            "10000",
            "11111"
        ),
        '0' to rows(
            "01110",
            "10001",
            "10011",
            "10101",
            "11001",
            "10001",
            "01110"
        ),
        '1' to rows(
            "00100",
            "01100",
            "00100",
            "00100",
            "00100",
            "00100",
            "01110"
        ),
        '2' to rows(
            "01110",
            "10001",
            "00001",
            "00010",
            "00100",
            "01000",
            "11111"
        ),
        '3' to rows(
            "11110",
            "00001",
            "00001",
            "01110",
            "00001",
            "00001",
            "11110"
        ),
        '4' to rows(
            "00010",
            "00110",
            "01010",
            "10010",
            "11111",
            "00010",
            "00010"
        ),
        '5' to rows(
            "11111",
            "10000",
            "10000",
            "11110",
            "00001",
            "00001",
            "11110"
        ),
        '6' to rows(
            "01110",
            "10000",
            "10000",
            "11110",
            "10001",
            "10001",
            "01110"
        ),
        '7' to rows(
            "11111",
            "00001",
            "00010",
            "00100",
            "01000",
            "01000",
            "01000"
        ),
        '8' to rows(
            "01110",
            "10001",
            "10001",
            "01110",
            "10001",
            "10001",
            "01110"
        ),
        '9' to rows(
            "01110",
            "10001",
            "10001",
            "01111",
            "00001",
            "00001",
            "01110"
        ),
        '?' to rows(
            "01110",
            "10001",
            "00001",
            "00010",
            "00100",
            "00000",
            "00100"
        ),
        '!' to rows(
            "00100",
            "00100",
            "00100",
            "00100",
            "00100",
            "00000",
            "00100"
        ),
        '-' to rows(
            "00000",
            "00000",
            "00000",
            "11111",
            "00000",
            "00000",
            "00000"
        ),
        '_' to rows(
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "11111"
        ),
        '.' to rows(
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00000",
            "00100"
        ),
        ':' to rows(
            "00000",
            "00100",
            "00100",
            "00000",
            "00100",
            "00100",
            "00000"
        ),
        '/' to rows(
            "00001",
            "00001",
            "00010",
            "00100",
            "01000",
            "10000",
            "10000"
        )
    )

    private fun rows(vararg rows: String): List<String> = rows.toList()

    private fun Char.toBlockGlyph(rows: List<String>): Glyph {
        val commands = rows.flatMapIndexed { rowIndex, row ->
            row.flatMapIndexed { columnIndex, marker ->
                if (marker == '1') {
                    blockCellCommands(
                        x = columnIndex * CELL,
                        y = (rows.lastIndex - rowIndex) * CELL
                    )
                } else {
                    emptyList()
                }
            }
        }
        val width = if (this == ' ') SPACE_ADVANCE else ADVANCE
        return Glyph(
            unicode = this,
            width = width,
            leftSideBearing = 0f,
            rightSideBearing = 0f,
            path = GlyphPath(
                commands = commands,
                boundingBox = BoundingBox2D(0f, 0f, 5f * CELL, 7f * CELL)
            )
        )
    }

    private fun blockCellCommands(x: Float, y: Float): List<GeometryPathCommand> = listOf(
        GeometryPathCommand.MoveTo(x, y),
        GeometryPathCommand.LineTo(x, y + FILL),
        GeometryPathCommand.LineTo(x + FILL, y + FILL),
        GeometryPathCommand.LineTo(x + FILL, y),
        GeometryPathCommand.ClosePath
    )
}

private val typefaceJson = Json { ignoreUnknownKeys = true }

internal fun String.toGeometryFont(): GeometryFont {
    val root = typefaceJson.parseToJsonElement(this).jsonObject
    val bounds = root["boundingBox"]?.jsonObject
    val fontBounds = ParsedFontBounds(
        xMin = bounds.floatValue("xMin"),
        xMax = bounds.floatValue("xMax"),
        yMin = bounds.floatValue("yMin"),
        yMax = bounds.floatValue("yMax")
    )
    val convertedGlyphs = root["glyphs"]
        ?.jsonObject
        ?.mapNotNull { (key, value) ->
            val char = key.singleOrNull() ?: return@mapNotNull null
            val glyph = value.jsonObject
            char to ParsedFontGlyph(
                char = char,
                horizontalAdvance = glyph.floatValue("ha"),
                pathCommands = glyph.stringValue("o")
            ).toGeometryGlyph(fontBounds)
        }
        ?.toMap()
        .orEmpty()

    return LoadedGeometryFont(
        familyName = root.stringValue("familyName").ifBlank { "Unknown" },
        unitsPerEm = root.intValue("resolution", 1000),
        ascender = root.floatValue("ascender", fontBounds.yMax),
        descender = root.floatValue("descender", fontBounds.yMin),
        glyphs = convertedGlyphs
    )
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

private data class ParsedFontBounds(
    val xMin: Float,
    val xMax: Float,
    val yMin: Float,
    val yMax: Float
)

private data class ParsedFontGlyph(
    val char: Char,
    val horizontalAdvance: Float,
    val pathCommands: String
)

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

private fun ParsedFontGlyph.toGeometryGlyph(fontBounds: ParsedFontBounds): Glyph {
    val commands = pathCommands.toGeometryCommands()
    return Glyph(
        unicode = char,
        width = horizontalAdvance,
        leftSideBearing = 0f,
        rightSideBearing = 0f,
        path = GlyphPath(commands, commands.boundsOrFontBounds(fontBounds))
    )
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

private fun List<GeometryPathCommand>.boundsOrFontBounds(
    fontBounds: ParsedFontBounds
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

private fun JsonObject.stringValue(name: String): String =
    this[name]?.jsonPrimitive?.contentOrNull.orEmpty()

private fun JsonObject.floatValue(name: String, default: Float = 0f): Float =
    this[name]?.jsonPrimitive?.floatOrNull ?: default

private fun JsonObject?.floatValue(name: String, default: Float = 0f): Float =
    this?.get(name)?.jsonPrimitive?.floatOrNull ?: default

private fun JsonObject.intValue(name: String, default: Int = 0): Int =
    this[name]?.jsonPrimitive?.intOrNull ?: default
