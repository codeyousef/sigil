package codes.yousef.sigil.summon.effects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import codes.yousef.sigil.schema.effects.*

/**
 * TDD tests for EffectSummonContext - manages screen-space effects during composition.
 */
class EffectSummonContextTest {

    @Test
    fun effectSummonContext_shouldTrackRegisteredEffects() {
        val context = EffectSummonContext.createServerContext()
        
        val effect = ShaderEffectData(
            id = "aurora-1",
            name = "Aurora",
            fragmentShader = "// shader code"
        )
        
        context.registerEffect(effect)
        
        assertEquals(1, context.effects.size)
        assertEquals("aurora-1", context.effects[0].id)
    }

    @Test
    fun effectSummonContext_shouldSupportMultipleEffects() {
        val context = EffectSummonContext.createServerContext()
        
        context.registerEffect(ShaderEffectData(id = "effect-1", fragmentShader = "// 1"))
        context.registerEffect(ShaderEffectData(id = "effect-2", fragmentShader = "// 2"))
        context.registerEffect(ShaderEffectData(id = "effect-3", fragmentShader = "// 3"))
        
        assertEquals(3, context.effects.size)
    }

    @Test
    fun effectSummonContext_shouldBuildComposerData() {
        val context = EffectSummonContext.createServerContext()
        
        context.registerEffect(ShaderEffectData(
            id = "bg",
            fragmentShader = "// background",
            blendMode = BlendMode.NORMAL
        ))
        context.registerEffect(ShaderEffectData(
            id = "overlay",
            fragmentShader = "// overlay",
            blendMode = BlendMode.MULTIPLY,
            opacity = 0.5f
        ))
        
        val composer = context.buildComposerData("test-composer")
        
        assertEquals("test-composer", composer.id)
        assertEquals(2, composer.effects.size)
        assertEquals(BlendMode.MULTIPLY, composer.effects[1].blendMode)
    }

    @Test
    fun effectSummonContext_shouldClear() {
        val context = EffectSummonContext.createServerContext()
        context.registerEffect(ShaderEffectData(id = "test", fragmentShader = "//"))
        
        context.clear()
        
        assertTrue(context.effects.isEmpty())
    }

    @Test
    fun effectSummonContext_shouldDistinguishServerAndClient() {
        val serverContext = EffectSummonContext.createServerContext()
        val clientContext = EffectSummonContext.createClientContext()
        
        assertTrue(serverContext.isServer)
        assertTrue(!clientContext.isServer)
    }
}
