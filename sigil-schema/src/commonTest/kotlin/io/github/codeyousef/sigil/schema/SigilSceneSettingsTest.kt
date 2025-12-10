package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilSceneSettingsTest {

    @Test
    fun sceneSettings_defaultValues_areCorrect() {
        val settings = SceneSettings()
        
        assertEquals(0xFF1A1A2E.toInt(), settings.backgroundColor)
        assertFalse(settings.fogEnabled)
        assertEquals(0xFFFFFFFF.toInt(), settings.fogColor)
        assertEquals(10f, settings.fogNear)
        assertEquals(100f, settings.fogFar)
        assertTrue(settings.shadowsEnabled)
        assertEquals(ToneMappingMode.ACES_FILMIC, settings.toneMapping)
        assertEquals(1f, settings.exposure)
    }

    @Test
    fun sceneSettings_customValues_serialize() {
        val settings = SceneSettings(
            backgroundColor = 0xFF000000.toInt(),
            fogEnabled = true,
            fogColor = 0xFFAAAAAA.toInt(),
            fogNear = 5f,
            fogFar = 500f,
            shadowsEnabled = false,
            toneMapping = ToneMappingMode.REINHARD,
            exposure = 2.5f
        )
        val scene = SigilScene(settings = settings)
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(0xFF000000.toInt(), restored.settings.backgroundColor)
        assertTrue(restored.settings.fogEnabled)
        assertEquals(0xFFAAAAAA.toInt(), restored.settings.fogColor)
        assertEquals(5f, restored.settings.fogNear)
        assertEquals(500f, restored.settings.fogFar)
        assertFalse(restored.settings.shadowsEnabled)
        assertEquals(ToneMappingMode.REINHARD, restored.settings.toneMapping)
        assertEquals(2.5f, restored.settings.exposure)
    }

    @Test
    fun sceneSettings_allToneMappingModes_serialize() {
        ToneMappingMode.entries.forEach { mode ->
            val scene = SigilScene(settings = SceneSettings(toneMapping = mode))
            val json = scene.toJson()
            val restored = SigilScene.fromJson(json)
            assertEquals(mode, restored.settings.toneMapping)
        }
    }

    @Test
    fun sceneSettings_edgeCaseExposure_serializes() {
        val exposures = listOf(0f, 0.001f, 1f, 10f, 100f, Float.MAX_VALUE)
        
        exposures.forEach { exposure ->
            val scene = SigilScene(settings = SceneSettings(exposure = exposure))
            val json = scene.toJson()
            val restored = SigilScene.fromJson(json)
            assertEquals(exposure, restored.settings.exposure)
        }
    }

    @Test
    fun sceneSettings_fogDistanceEdgeCases_serialize() {
        // Test fog near >= fog far (edge case that should still serialize)
        val scene = SigilScene(
            settings = SceneSettings(fogEnabled = true, fogNear = 100f, fogFar = 50f)
        )
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(100f, restored.settings.fogNear)
        assertEquals(50f, restored.settings.fogFar)
    }

    @Test
    fun sceneSettings_equality_worksCorrectly() {
        val s1 = SceneSettings(fogEnabled = true, fogNear = 10f)
        val s2 = SceneSettings(fogEnabled = true, fogNear = 10f)
        val s3 = SceneSettings(fogEnabled = false, fogNear = 10f)
        
        assertEquals(s1, s2)
        assertTrue(s1 != s3)
    }
}
