package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.SceneNodePatch
import codes.yousef.sigil.schema.ScenePatch
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.builtins.ListSerializer

class SigilSceneEventBindingTest {
    @Test
    fun acceptedDropCanMatchSourceAndTargetInteractions() {
        val match = SigilSceneEventMatch(
            type = "drop",
            sourceInteractionId = "package:pkg-7",
            targetInteractionId = "truck:0",
            accepted = true,
            result = "accepted"
        )

        val acceptedDrop = dropPayload(
            sourceInteractionId = "package:pkg-7",
            targetInteractionId = "truck:0",
            accepted = true,
            result = "accepted"
        )
        val rejectedDrop = acceptedDrop.copy(
            drag = acceptedDrop.drag?.copy(accepted = false, result = "rejected")
        )

        assertTrue(match.matches(acceptedDrop))
        assertFalse(match.matches(rejectedDrop))
    }

    @Test
    fun generatedInteractionFamiliesCanBeMatchedByPrefix() {
        val match = SigilSceneEventMatch(
            type = "drop",
            sourceInteractionIdPrefix = "package:",
            targetInteractionIdPrefix = "truck:",
            accepted = true
        )

        assertTrue(
            match.matches(
                dropPayload(
                    sourceInteractionId = "package:fragile-11",
                    targetInteractionId = "truck:2",
                    accepted = true
                )
            )
        )
        assertFalse(
            match.matches(
                dropPayload(
                    sourceInteractionId = "package:fragile-11",
                    targetInteractionId = "return-bin",
                    accepted = true
                )
            )
        )
    }

    @Test
    fun nodeActionMatchersUseInteractionActions() {
        val match = SigilSceneEventMatch(type = "click", nodeAction = "repair")

        assertTrue(
            match.matches(
                SigilSceneEventPayload(
                    type = "click",
                    interactionId = "repair-wrench",
                    actions = listOf("repair", "wrench")
                )
            )
        )
        assertFalse(
            match.matches(
                SigilSceneEventPayload(
                    type = "click",
                    interactionId = "truck:0",
                    actions = listOf("route", "truck")
                )
            )
        )
    }

    @Test
    fun serializableBindingsRoundTripForHydration() {
        val bindings = listOf(
            SigilSceneEventBinding(
                match = SigilSceneEventMatch(
                    type = "dragenter",
                    targetIdPrefix = "truck",
                    accepted = true
                ),
                callbackId = "callback-1",
                reloadOnSuccess = true,
                stopPropagation = true
            )
        )

        val serializer = ListSerializer(SigilSceneEventBinding.serializer())
        val json = SigilJson.encodeToString(serializer, bindings)
        val restored = SigilJson.decodeFromString(serializer, json)

        assertTrue(restored.single().match.matches(
            dropPayload(
                sourceInteractionId = "package:pkg-7",
                targetInteractionId = "truck:0",
                targetId = "truck:0",
                accepted = true
            ).copy(type = "dragenter")
        ))
        assertTrue(restored.single().reloadOnSuccess == true)
        assertTrue(restored.single().stopPropagation)
    }

    @Test
    fun callbackResponsesExposeCurrentCanvasScenePatches() {
        val response = SigilSceneEventCallbackResponse(
            action = "reload",
            patches = listOf(
                SigilScenePatchTarget(
                    canvasId = "scene-a",
                    patch = ScenePatch(nodes = listOf(SceneNodePatch(interactionId = "package:1", visible = false)))
                ),
                SigilScenePatchTarget(
                    canvasId = "scene-b",
                    patch = ScenePatch(nodes = listOf(SceneNodePatch(interactionId = "package:2", visible = false)))
                )
            )
        )

        val scenePatches = response.scenePatchesFor("scene-a")

        assertTrue(response.wantsReload)
        assertTrue(scenePatches.size == 1)
        assertTrue(scenePatches.single().nodes.single().interactionId == "package:1")
    }

    @Test
    fun callbackResponsesCollectDomAndSummonPatches() {
        val response = SigilSceneEventCallbackResponse(
            domPatch = SigilDomPatch(selector = "#processed", text = "Processed 1/10"),
            summonPatches = listOf(
                SigilDomPatch(selector = "#score", text = "Score 50", mode = SigilDomPatchMode.TEXT_CONTENT)
            )
        )

        val patches = response.domPatchesToApply()

        assertTrue(patches.size == 2)
        assertTrue(patches.any { it.selector == "#processed" })
        assertTrue(patches.any { it.selector == "#score" })
    }

    private fun dropPayload(
        sourceInteractionId: String,
        targetInteractionId: String,
        targetId: String = targetInteractionId,
        accepted: Boolean,
        result: String = if (accepted) "accepted" else "rejected"
    ): SigilSceneEventPayload =
        SigilSceneEventPayload(
            type = "drop",
            interactionId = targetInteractionId,
            drag = SigilSceneDragPayload(
                sourceInteractionId = sourceInteractionId,
                targetInteractionId = targetInteractionId,
                targetId = targetId,
                accepted = accepted,
                result = result
            )
        )
}
