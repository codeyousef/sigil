package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.SigilJson
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
