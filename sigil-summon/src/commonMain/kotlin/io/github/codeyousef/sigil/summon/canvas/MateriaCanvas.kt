package io.github.codeyousef.sigil.summon.canvas

import code.yousef.summon.annotation.Composable
import io.github.codeyousef.sigil.schema.SigilScene
import io.github.codeyousef.sigil.schema.SceneSettings

/**
 * Entry point for rendering a Materia 3D scene within Summon.
 *
 * Platform implementations:
 * - JVM (Server): Serializes scene data to JSON embedded in HTML
 * - JS (Client): Hydrates the scene and creates actual Materia objects
 *
 * @param id Unique ID for the canvas container element
 * @param width CSS width of the canvas
 * @param height CSS height of the canvas
 * @param backgroundColor Background color as ARGB int
 * @param content Composable lambda containing 3D scene components
 */
@Composable
expect fun MateriaCanvas(
    id: String = "sigil-materia-canvas",
    width: String = "100%",
    height: String = "100%",
    backgroundColor: Int = 0xFF1A1A2E.toInt(),
    content: @Composable () -> String
): String

/**
 * Configure scene-level settings.
 */
@Composable
fun SceneConfig(
    backgroundColor: Int = 0xFF1A1A2E.toInt(),
    fogEnabled: Boolean = false,
    fogColor: Int = 0xFFFFFFFF.toInt(),
    fogNear: Float = 10f,
    fogFar: Float = 100f,
    shadowsEnabled: Boolean = true
): String {
    val context = io.github.codeyousef.sigil.summon.context.SigilSummonContext.current()
    context.configureSettings {
        SceneSettings(
            backgroundColor = backgroundColor,
            fogEnabled = fogEnabled,
            fogColor = fogColor,
            fogNear = fogNear,
            fogFar = fogFar,
            shadowsEnabled = shadowsEnabled
        )
    }
    return ""
}
