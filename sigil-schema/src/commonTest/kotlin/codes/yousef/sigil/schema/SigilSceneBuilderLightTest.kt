package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SigilSceneBuilderLightTest {

    @Test
    fun light_defaultParameters_createsValidLight() {
        val scene = sigilScene {
            light()
        }
        
        assertEquals(1, scene.rootNodes.size)
        val light = scene.rootNodes[0] as LightData
        
        assertNotNull(light.id)
        assertEquals(LightType.POINT, light.lightType)
        assertEquals(1f, light.intensity)
    }

    @Test
    fun light_customParameters_createsCorrectLight() {
        val scene = sigilScene {
            light(
                id = "custom-light",
                lightType = LightType.SPOT,
                position = listOf(0f, 10f, 0f),
                color = 0xFFFF0000.toInt(),
                intensity = 2.5f,
                distance = 50f,
                decay = 1.5f,
                angle = 0.785f,
                penumbra = 0.3f,
                castShadow = true,
                target = listOf(0f, 0f, 0f),
                visible = true,
                name = "Spotlight"
            )
        }
        
        val light = scene.rootNodes[0] as LightData
        
        assertEquals("custom-light", light.id)
        assertEquals(LightType.SPOT, light.lightType)
        assertEquals(listOf(0f, 10f, 0f), light.position)
        assertEquals(0xFFFF0000.toInt(), light.color)
        assertEquals(2.5f, light.intensity)
        assertEquals(50f, light.distance)
        assertEquals(1.5f, light.decay)
        assertEquals(0.785f, light.angle)
        assertEquals(0.3f, light.penumbra)
        assertTrue(light.castShadow)
        assertEquals(listOf(0f, 0f, 0f), light.target)
        assertEquals("Spotlight", light.name)
    }

    @Test
    fun light_allLightTypes_work() {
        LightType.entries.forEach { lightType ->
            val scene = sigilScene {
                light(lightType = lightType)
            }
            
            val light = scene.rootNodes[0] as LightData
            assertEquals(lightType, light.lightType)
        }
    }
}
