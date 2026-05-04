package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.SceneNodePatch
import codes.yousef.sigil.schema.ScenePatch
import codes.yousef.summon.runtime.CallbackRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MateriaCanvasSceneEventBindingTest {
    @Test
    fun jvmCanvasRegistersSceneEventCallbacksForHydration() {
        CallbackRegistry.clear()
        CallbackRegistry.beginRender()

        try {
            val html = MateriaCanvas(
                id = "scene",
                sceneEventHandlers = listOf(
                    SigilSceneEventHandler(
                        match = SigilSceneEventMatch(
                            type = "drop",
                            sourceInteractionIdPrefix = "package:",
                            targetInteractionIdPrefix = "truck:",
                            accepted = true
                        ),
                        onEvent = {},
                        reloadOnSuccess = true
                    )
                )
            ) { "" }

            val callbackIds = CallbackRegistry.finishRenderAndCollectCallbackIds()

            assertEquals(1, callbackIds.size)
            assertTrue(html.contains("""id="scene-actions""""))
            assertTrue(html.contains(""""callbackId":"${callbackIds.single()}""""))
            assertTrue(html.contains(""""reloadOnSuccess":true"""))
        } finally {
            CallbackRegistry.abandonRenderContext()
            CallbackRegistry.clear()
        }
    }

    @Test
    fun jvmCanvasRegistersPatchResponseCallbacksWithSigilEndpoint() {
        var invoked = 0
        CallbackRegistry.clear()
        SigilSceneCallbackRegistry.clear()
        CallbackRegistry.beginRender()

        try {
            val html = MateriaCanvas(
                id = "scene",
                sceneEventHandlers = listOf(
                    SigilSceneEventHandler(
                        match = SigilSceneEventMatch(type = "drop", accepted = true),
                        onEvent = { invoked += 1 },
                        onResponse = {
                            SigilSceneEventCallbackResponse(
                                scenePatch = ScenePatch(
                                    nodes = listOf(
                                        SceneNodePatch(interactionId = "package:1", visible = false)
                                    )
                                ),
                                domPatch = SigilDomPatch(selector = "#processed", text = "Processed 1/10")
                            )
                        }
                    )
                )
            ) { "" }

            val callbackIds = CallbackRegistry.finishRenderAndCollectCallbackIds()
            val callbackId = """"callbackId":"([^"]+)"""".toRegex()
                .find(html)
                ?.groupValues
                ?.get(1)
                ?: error("missing callback id")

            assertTrue(callbackIds.isEmpty())

            assertTrue(html.contains(""""callbackUrl":"/sigil/callback/$callbackId""""))
            assertTrue(SigilSceneCallbackRegistry.hasCallback(callbackId))

            val firstResult = SigilSceneCallbackRegistry.executeCallback(callbackId)
            val secondResult = SigilSceneCallbackRegistry.executeCallback(callbackId)

            assertTrue(firstResult.found)
            assertEquals(200, firstResult.statusCode)
            assertEquals("package:1", firstResult.response.scenePatch?.nodes?.single()?.interactionId)
            assertEquals("#processed", firstResult.response.domPatch?.selector)
            assertTrue(secondResult.found)
            assertEquals(2, invoked)
        } finally {
            CallbackRegistry.abandonRenderContext()
            CallbackRegistry.clear()
            SigilSceneCallbackRegistry.clear()
        }
    }
}
