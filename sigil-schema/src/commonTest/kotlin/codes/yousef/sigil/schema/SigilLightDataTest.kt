package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilLightDataTest {

    @Test
    fun lightData_allLightTypes_serialize() {
        LightType.entries.forEach { lightType ->
            val light = LightData(id = "light-$lightType", lightType = lightType)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), light)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as LightData
            assertEquals(lightType, restored.lightType)
        }
    }

    @Test
    fun lightData_defaultValues_areCorrect() {
        val light = LightData(id = "default-light")
        
        assertEquals(LightType.POINT, light.lightType)
        assertEquals(0xFFFFFFFF.toInt(), light.color)
        assertEquals(1f, light.intensity)
        assertEquals(0f, light.distance)
        assertEquals(2f, light.decay)
        assertEquals(0.523599f, light.angle) // PI/6
        assertEquals(0f, light.penumbra)
        assertFalse(light.castShadow)
        assertEquals(listOf(0f, 0f, 0f), light.target)
    }

    @Test
    fun lightData_spotLightParameters_serialize() {
        val spotLight = LightData(
            id = "spot-light",
            lightType = LightType.SPOT,
            angle = 0.785398f, // PI/4
            penumbra = 0.5f,
            distance = 100f,
            decay = 1f,
            target = listOf(0f, -1f, 0f),
            castShadow = true
        )
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), spotLight)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as LightData
        
        assertEquals(0.785398f, restored.angle)
        assertEquals(0.5f, restored.penumbra)
        assertEquals(100f, restored.distance)
        assertEquals(1f, restored.decay)
        assertEquals(listOf(0f, -1f, 0f), restored.target)
        assertTrue(restored.castShadow)
    }

    @Test
    fun lightData_edgeCaseIntensity_serializes() {
        val intensities = listOf(0f, 0.001f, 1f, 100f, 1000000f, -1f, Float.MAX_VALUE)
        
        intensities.forEach { intensity ->
            val light = LightData(id = "intensity-test", intensity = intensity)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), light)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as LightData
            assertEquals(intensity, restored.intensity)
        }
    }

    @Test
    fun lightType_allValues_serialize() {
        LightType.entries.forEach { type ->
            val light = LightData(id = "light-$type", lightType = type)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), light)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as LightData
            assertEquals(type, restored.lightType)
        }
    }
}
