package codes.yousef.sigil.schema.effects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import codes.yousef.sigil.schema.SigilJson

/**
 * TDD tests for effect data serialization.
 */
class EffectDataSerializationTest {

    private val json = SigilJson

    @Test
    fun shaderEffectData_shouldSerializeAndDeserialize() {
        val effect = ShaderEffectData(
            id = "aurora-1",
            name = "Aurora Background",
            fragmentShader = "// WGSL shader code",
            uniforms = mapOf(
                "time" to UniformValue.FloatValue(0f),
                "resolution" to UniformValue.Vec2Value(Vec2(1920f, 1080f))
            ),
            blendMode = BlendMode.NORMAL,
            opacity = 0.85f,
            enabled = true
        )

        val serialized = json.encodeToString(effect)
        val deserialized = json.decodeFromString<ShaderEffectData>(serialized)

        assertEquals(effect.id, deserialized.id)
        assertEquals(effect.name, deserialized.name)
        assertEquals(effect.fragmentShader, deserialized.fragmentShader)
        assertEquals(effect.blendMode, deserialized.blendMode)
        assertEquals(effect.opacity, deserialized.opacity)
        assertEquals(effect.enabled, deserialized.enabled)
    }

    @Test
    fun uniformValue_floatValue_shouldSerialize() {
        val uniform: UniformValue = UniformValue.FloatValue(3.14f)
        val serialized = json.encodeToString<UniformValue>(uniform)
        val deserialized = json.decodeFromString<UniformValue>(serialized)

        assertTrue(deserialized is UniformValue.FloatValue)
        assertEquals(3.14f, (deserialized as UniformValue.FloatValue).value)
    }

    @Test
    fun uniformValue_vec2Value_shouldSerialize() {
        val uniform: UniformValue = UniformValue.Vec2Value(Vec2(1f, 2f))
        val serialized = json.encodeToString<UniformValue>(uniform)
        val deserialized = json.decodeFromString<UniformValue>(serialized)

        assertTrue(deserialized is UniformValue.Vec2Value)
        val vec = (deserialized as UniformValue.Vec2Value).value
        assertEquals(1f, vec.x)
        assertEquals(2f, vec.y)
    }

    @Test
    fun uniformValue_vec3Value_shouldSerialize() {
        val uniform: UniformValue = UniformValue.Vec3Value(Vec3(1f, 2f, 3f))
        val serialized = json.encodeToString<UniformValue>(uniform)
        val deserialized = json.decodeFromString<UniformValue>(serialized)

        assertTrue(deserialized is UniformValue.Vec3Value)
        val vec = (deserialized as UniformValue.Vec3Value).value
        assertEquals(1f, vec.x)
        assertEquals(2f, vec.y)
        assertEquals(3f, vec.z)
    }

    @Test
    fun uniformValue_vec4Value_shouldSerialize() {
        val uniform: UniformValue = UniformValue.Vec4Value(Vec4(0.5f, 0.5f, 0.5f, 1.0f))
        val serialized = json.encodeToString<UniformValue>(uniform)
        val deserialized = json.decodeFromString<UniformValue>(serialized)

        assertTrue(deserialized is UniformValue.Vec4Value)
        val vec = (deserialized as UniformValue.Vec4Value).value
        assertEquals(0.5f, vec.x)
        assertEquals(0.5f, vec.y)
        assertEquals(0.5f, vec.z)
        assertEquals(1.0f, vec.w)
    }

    @Test
    fun effectComposerData_shouldSerializeMultipleEffects() {
        val composer = EffectComposerData(
            id = "composer-1",
            effects = listOf(
                ShaderEffectData(
                    id = "effect-1",
                    name = "Background",
                    fragmentShader = "// shader 1",
                    blendMode = BlendMode.NORMAL
                ),
                ShaderEffectData(
                    id = "effect-2",
                    name = "Vignette",
                    fragmentShader = "// shader 2",
                    blendMode = BlendMode.MULTIPLY,
                    opacity = 0.3f
                )
            )
        )

        val serialized = json.encodeToString(composer)
        val deserialized = json.decodeFromString<EffectComposerData>(serialized)

        assertEquals(2, deserialized.effects.size)
        assertEquals("effect-1", deserialized.effects[0].id)
        assertEquals("effect-2", deserialized.effects[1].id)
        assertEquals(BlendMode.MULTIPLY, deserialized.effects[1].blendMode)
    }
}
