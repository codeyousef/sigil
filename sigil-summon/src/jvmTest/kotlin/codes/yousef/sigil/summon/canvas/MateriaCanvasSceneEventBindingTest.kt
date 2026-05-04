package codes.yousef.sigil.summon.canvas

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
}
