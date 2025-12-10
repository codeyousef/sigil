package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilSceneBuilderSettingsTest {

    @Test
    fun settings_default_usesDefaultSettings() {
        val scene = sigilScene { }
        
        assertEquals(SceneSettings(), scene.settings)
    }

    @Test
    fun settings_custom_appliesCorrectly() {
        val scene = sigilScene {
            settings(
                backgroundColor = 0xFF000000.toInt(),
                fogEnabled = true,
                fogColor = 0xFFAAAAAA.toInt(),
                fogNear = 5f,
                fogFar = 500f,
                shadowsEnabled = false,
                toneMapping = ToneMappingMode.REINHARD,
                exposure = 2.5f
            )
        }
        
        assertEquals(0xFF000000.toInt(), scene.settings.backgroundColor)
        assertTrue(scene.settings.fogEnabled)
        assertEquals(0xFFAAAAAA.toInt(), scene.settings.fogColor)
        assertEquals(5f, scene.settings.fogNear)
        assertEquals(500f, scene.settings.fogFar)
        assertFalse(scene.settings.shadowsEnabled)
        assertEquals(ToneMappingMode.REINHARD, scene.settings.toneMapping)
        assertEquals(2.5f, scene.settings.exposure)
    }

    @Test
    fun settings_calledMultipleTimes_lastWins() {
        val scene = sigilScene {
            settings(backgroundColor = 0xFF111111.toInt())
            settings(backgroundColor = 0xFF222222.toInt())
            settings(backgroundColor = 0xFF333333.toInt())
        }
        
        assertEquals(0xFF333333.toInt(), scene.settings.backgroundColor)
    }

    @Test
    fun settings_allToneMappingModes_work() {
        ToneMappingMode.entries.forEach { mode ->
            val scene = sigilScene {
                settings(toneMapping = mode)
            }
            assertEquals(mode, scene.settings.toneMapping)
        }
    }
}
