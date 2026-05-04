package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.ScenePatch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Declarative bridge from Sigil scene events to Summon-side actions.
 *
 * A handler is registered on the rendering platform, while the serializable
 * binding is sent to the browser so the Sigil hydrator can invoke the matching
 * action when a 3D interaction occurs.
 */
data class SigilSceneEventHandler(
    val match: SigilSceneEventMatch,
    val onEvent: () -> Unit,
    val reloadOnSuccess: Boolean? = null,
    val preventDefault: Boolean = true,
    val stopPropagation: Boolean = false
)

@Serializable
data class SigilSceneEventBinding(
    val match: SigilSceneEventMatch = SigilSceneEventMatch(),
    val callbackId: String? = null,
    val localHandlerId: String? = null,
    val url: String? = null,
    val reloadOnSuccess: Boolean? = null,
    val preventDefault: Boolean = true,
    val stopPropagation: Boolean = false
)

@Serializable
data class SigilSceneEventCallbackResponse(
    val action: String? = null,
    val status: String? = null,
    val scenePatch: ScenePatch? = null,
    val sigilPatch: ScenePatch? = null,
    val patch: ScenePatch? = null,
    val patches: List<SigilScenePatchTarget> = emptyList(),
    val scenePatches: List<SigilScenePatchTarget> = emptyList(),
    val domPatch: SigilDomPatch? = null,
    val domPatches: List<SigilDomPatch> = emptyList(),
    val summonPatch: SigilDomPatch? = null,
    val summonPatches: List<SigilDomPatch> = emptyList()
) {
    val wantsReload: Boolean
        get() = action == "reload"

    fun scenePatchesFor(canvasId: String): List<ScenePatch> =
        listOfNotNull(scenePatch, sigilPatch, patch)
            .filter { it.nodes.isNotEmpty() } +
            (patches + scenePatches)
                .filter { it.canvasId == null || it.canvasId == canvasId }
                .mapNotNull { it.patch?.takeIf { patch -> patch.nodes.isNotEmpty() } }

    fun domPatchesToApply(): List<SigilDomPatch> =
        listOfNotNull(domPatch, summonPatch) + domPatches + summonPatches
}

@Serializable
data class SigilScenePatchTarget(
    val canvasId: String? = null,
    val patch: ScenePatch? = null
)

@Serializable
data class SigilDomPatch(
    val selector: String,
    val html: String? = null,
    val text: String? = null,
    val mode: SigilDomPatchMode = SigilDomPatchMode.INNER_HTML
)

@Serializable
enum class SigilDomPatchMode {
    @SerialName("innerHtml") INNER_HTML,
    @SerialName("outerHtml") OUTER_HTML,
    @SerialName("textContent") TEXT_CONTENT,
    @SerialName("remove") REMOVE
}

@Serializable
data class SigilSceneEventMatch(
    val type: String? = null,
    val nodeId: String? = null,
    val nodeIdPrefix: String? = null,
    val interactionId: String? = null,
    val interactionIdPrefix: String? = null,
    val nodeAction: String? = null,
    val nodeEvent: String? = null,
    val dropState: String? = null,
    val sourceNodeId: String? = null,
    val sourceNodeIdPrefix: String? = null,
    val sourceInteractionId: String? = null,
    val sourceInteractionIdPrefix: String? = null,
    val targetNodeId: String? = null,
    val targetNodeIdPrefix: String? = null,
    val targetInteractionId: String? = null,
    val targetInteractionIdPrefix: String? = null,
    val targetId: String? = null,
    val targetIdPrefix: String? = null,
    val accepted: Boolean? = null,
    val result: String? = null
)

@Serializable
data class SigilSceneEventPayload(
    val type: String,
    val nodeId: String? = null,
    val interactionId: String? = null,
    val actions: List<String> = emptyList(),
    val events: List<String> = emptyList(),
    val dropState: String? = null,
    val drag: SigilSceneDragPayload? = null
)

@Serializable
data class SigilSceneDragPayload(
    val sourceNodeId: String? = null,
    val sourceInteractionId: String? = null,
    val targetNodeId: String? = null,
    val targetInteractionId: String? = null,
    val targetId: String? = null,
    val targetState: String? = null,
    val accepted: Boolean? = null,
    val result: String? = null,
    val sourceDropGroups: List<String> = emptyList(),
    val targetGroups: List<String> = emptyList()
)

fun SigilSceneEventMatch.matches(payload: SigilSceneEventPayload): Boolean =
    matchesString(type, payload.type) &&
        matchesString(nodeId, payload.nodeId) &&
        matchesPrefix(nodeIdPrefix, payload.nodeId) &&
        matchesString(interactionId, payload.interactionId) &&
        matchesPrefix(interactionIdPrefix, payload.interactionId) &&
        matchesString(dropState, payload.dropState) &&
        matchesContains(nodeAction, payload.actions) &&
        matchesContains(nodeEvent, payload.events) &&
        matchesDrag(payload.drag)

private fun SigilSceneEventMatch.matchesDrag(drag: SigilSceneDragPayload?): Boolean {
    if (
        sourceNodeId == null &&
        sourceNodeIdPrefix == null &&
        sourceInteractionId == null &&
        sourceInteractionIdPrefix == null &&
        targetNodeId == null &&
        targetNodeIdPrefix == null &&
        targetInteractionId == null &&
        targetInteractionIdPrefix == null &&
        targetId == null &&
        targetIdPrefix == null &&
        accepted == null &&
        result == null
    ) {
        return true
    }

    drag ?: return false

    return matchesString(sourceNodeId, drag.sourceNodeId) &&
        matchesPrefix(sourceNodeIdPrefix, drag.sourceNodeId) &&
        matchesString(sourceInteractionId, drag.sourceInteractionId) &&
        matchesPrefix(sourceInteractionIdPrefix, drag.sourceInteractionId) &&
        matchesString(targetNodeId, drag.targetNodeId) &&
        matchesPrefix(targetNodeIdPrefix, drag.targetNodeId) &&
        matchesString(targetInteractionId, drag.targetInteractionId) &&
        matchesPrefix(targetInteractionIdPrefix, drag.targetInteractionId) &&
        matchesString(targetId, drag.targetId) &&
        matchesPrefix(targetIdPrefix, drag.targetId) &&
        matchesString(result, drag.result) &&
        (accepted == null || accepted == drag.accepted)
}

internal fun SigilSceneEventHandler.toCallbackBinding(callbackId: String): SigilSceneEventBinding =
    SigilSceneEventBinding(
        match = match,
        callbackId = callbackId,
        reloadOnSuccess = reloadOnSuccess,
        preventDefault = preventDefault,
        stopPropagation = stopPropagation
    )

internal fun SigilSceneEventHandler.toLocalBinding(localHandlerId: String): SigilSceneEventBinding =
    SigilSceneEventBinding(
        match = match,
        localHandlerId = localHandlerId,
        reloadOnSuccess = reloadOnSuccess,
        preventDefault = preventDefault,
        stopPropagation = stopPropagation
    )

private fun matchesString(expected: String?, actual: String?): Boolean =
    expected == null || expected == actual

private fun matchesPrefix(expectedPrefix: String?, actual: String?): Boolean =
    expectedPrefix == null || actual?.startsWith(expectedPrefix) == true

private fun matchesContains(expected: String?, actual: List<String>): Boolean =
    expected == null || expected in actual
