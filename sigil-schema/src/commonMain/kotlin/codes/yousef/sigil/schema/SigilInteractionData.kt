package codes.yousef.sigil.schema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Optional semantic metadata that makes a scene node addressable by gameplay
 * code without depending on renderer-generated IDs.
 */
@Serializable
data class InteractionMetadata(
    val interactionId: String? = null,
    val cursor: CursorHint = CursorHint.AUTO,
    val hitVolume: HitVolumeData? = null,
    val actions: List<String> = emptyList(),
    val events: List<String> = emptyList(),
    val enabled: Boolean = true,
    val drag: DragMetadata? = null,
    val dropTarget: DropTargetMetadata? = null
)

@Serializable
enum class CursorHint {
    @SerialName("auto") AUTO,
    @SerialName("pointer") POINTER,
    @SerialName("grab") GRAB,
    @SerialName("grabbing") GRABBING,
    @SerialName("crosshair") CROSSHAIR,
    @SerialName("none") NONE
}

@Serializable
data class HitVolumeData(
    val shape: HitVolumeShape = HitVolumeShape.BOUNDING_BOX,
    val center: List<Float> = listOf(0f, 0f, 0f),
    val size: List<Float>? = null,
    val radius: Float? = null
)

@Serializable
enum class HitVolumeShape {
    @SerialName("mesh") MESH,
    @SerialName("boundingBox") BOUNDING_BOX,
    @SerialName("box") BOX,
    @SerialName("sphere") SPHERE
}

@Serializable
data class DragMetadata(
    val enabled: Boolean = true,
    val mode: DragConstraintMode = DragConstraintMode.CAMERA_PLANE,
    val planeNormal: List<Float>? = null,
    val planePoint: List<Float>? = null,
    val laneAxis: List<Float>? = null,
    val min: Float? = null,
    val max: Float? = null,
    val dropGroups: List<String> = emptyList()
)

@Serializable
enum class DragConstraintMode {
    @SerialName("cameraPlane") CAMERA_PLANE,
    @SerialName("horizontal") HORIZONTAL,
    @SerialName("vertical") VERTICAL,
    @SerialName("lane") LANE
}

@Serializable
data class DropTargetMetadata(
    val enabled: Boolean = true,
    val targetId: String? = null,
    val groups: List<String> = emptyList(),
    val accepts: List<String> = emptyList()
)

@Serializable
data class SceneAnimationData(
    val id: String? = null,
    val trigger: AnimationTrigger = AnimationTrigger.SCENE_LOAD,
    val kind: AnimationKind,
    val durationMs: Int = 300,
    val delayMs: Int = 0,
    val easing: AnimationEasing = AnimationEasing.EASE_OUT,
    val vector: List<Float>? = null,
    val color: Int? = null,
    val intensity: Float = 1f,
    val repeat: Int = 0
)

@Serializable
enum class AnimationTrigger {
    @SerialName("sceneLoad") SCENE_LOAD,
    @SerialName("interaction") INTERACTION,
    @SerialName("patch") PATCH
}

@Serializable
enum class AnimationKind {
    @SerialName("slide") SLIDE,
    @SerialName("bob") BOB,
    @SerialName("thunk") THUNK,
    @SerialName("bounce") BOUNCE,
    @SerialName("tint") TINT,
    @SerialName("pulse") PULSE,
    @SerialName("shake") SHAKE,
    @SerialName("glitch") GLITCH,
    @SerialName("success") SUCCESS,
    @SerialName("failure") FAILURE,
    @SerialName("visibility") VISIBILITY
}

@Serializable
enum class AnimationEasing {
    @SerialName("linear") LINEAR,
    @SerialName("easeIn") EASE_IN,
    @SerialName("easeOut") EASE_OUT,
    @SerialName("easeInOut") EASE_IN_OUT
}

@Serializable
data class ScenePatch(
    val nodes: List<SceneNodePatch> = emptyList()
) {
    companion object {
        fun fromJson(json: String): ScenePatch =
            SigilJson.parseToJsonElement(json).toScenePatch()
    }
}

@Serializable
data class SceneNodePatch(
    val id: String? = null,
    val interactionId: String? = null,
    val position: List<Float>? = null,
    val rotation: List<Float>? = null,
    val scale: List<Float>? = null,
    val visible: Boolean? = null,
    val name: String? = null,
    val highlight: HighlightPatch? = null,
    val label: String? = null,
    val animations: List<SceneAnimationData> = emptyList()
)

@Serializable
data class HighlightPatch(
    val active: Boolean,
    val color: Int = 0xFFFFD166.toInt(),
    val intensity: Float = 1f
)

fun InteractionMetadata.toJsonObject(): JsonObject = jsonObjectOf(
    "interactionId" to interactionId?.let(::JsonPrimitive),
    "cursor" to JsonPrimitive(cursor.serialName),
    "hitVolume" to hitVolume?.toJsonObject(),
    "actions" to actions.toJsonArray(),
    "events" to events.toJsonArray(),
    "enabled" to JsonPrimitive(enabled),
    "drag" to drag?.toJsonObject(),
    "dropTarget" to dropTarget?.toJsonObject()
)

fun InteractionMetadata?.toInteractionJson(): JsonObject? = this?.toJsonObject()

fun List<SceneAnimationData>.toAnimationJson(): List<JsonObject> = map { it.toJsonObject() }

fun JsonElement.toInteractionMetadata(): InteractionMetadata {
    val obj = this as? JsonObject ?: JsonObject(emptyMap())
    return InteractionMetadata(
        interactionId = obj.stringOrNull("interactionId"),
        cursor = cursorHintFromSerialName(obj.stringOrNull("cursor")) ?: CursorHint.AUTO,
        hitVolume = obj.objOrNull("hitVolume")?.toHitVolumeData(),
        actions = obj.stringList("actions"),
        events = obj.stringList("events"),
        enabled = obj.booleanOrNull("enabled") ?: true,
        drag = obj.objOrNull("drag")?.toDragMetadata(),
        dropTarget = obj.objOrNull("dropTarget")?.toDropTargetMetadata()
    )
}

fun SceneAnimationData.toJsonObject(): JsonObject = jsonObjectOf(
    "id" to id?.let(::JsonPrimitive),
    "trigger" to JsonPrimitive(trigger.serialName),
    "kind" to JsonPrimitive(kind.serialName),
    "durationMs" to JsonPrimitive(durationMs),
    "delayMs" to JsonPrimitive(delayMs),
    "easing" to JsonPrimitive(easing.serialName),
    "vector" to vector?.toFloatJsonArray(),
    "color" to color?.let(::JsonPrimitive),
    "intensity" to JsonPrimitive(intensity),
    "repeat" to JsonPrimitive(repeat)
)

fun JsonElement.toSceneAnimationData(): SceneAnimationData {
    val obj = this as? JsonObject ?: JsonObject(emptyMap())
    return SceneAnimationData(
        id = obj.stringOrNull("id"),
        trigger = animationTriggerFromSerialName(obj.stringOrNull("trigger")) ?: AnimationTrigger.SCENE_LOAD,
        kind = animationKindFromSerialName(obj.stringOrNull("kind"))
            ?: throw SerializationException("SceneAnimationData.kind is required"),
        durationMs = obj.intOrNull("durationMs") ?: 300,
        delayMs = obj.intOrNull("delayMs") ?: 0,
        easing = animationEasingFromSerialName(obj.stringOrNull("easing")) ?: AnimationEasing.EASE_OUT,
        vector = obj.floatListOrNull("vector"),
        color = obj.intOrNull("color"),
        intensity = obj.floatOrNull("intensity") ?: 1f,
        repeat = obj.intOrNull("repeat") ?: 0
    )
}

fun ScenePatch.toJson(): String = toJsonObject().toString()

private fun ScenePatch.toJsonObject(): JsonObject = jsonObjectOf(
    "nodes" to JsonArray(nodes.map { it.toJsonObject() })
)

private fun JsonElement.toScenePatch(): ScenePatch {
    val obj = this as? JsonObject ?: JsonObject(emptyMap())
    return ScenePatch(
        nodes = obj.arrayOrEmpty("nodes").mapNotNull { (it as? JsonObject)?.toSceneNodePatch() }
    )
}

private fun SceneNodePatch.toJsonObject(): JsonObject = jsonObjectOf(
    "id" to id?.let(::JsonPrimitive),
    "interactionId" to interactionId?.let(::JsonPrimitive),
    "position" to position?.toFloatJsonArray(),
    "rotation" to rotation?.toFloatJsonArray(),
    "scale" to scale?.toFloatJsonArray(),
    "visible" to visible?.let(::JsonPrimitive),
    "name" to name?.let(::JsonPrimitive),
    "highlight" to highlight?.toJsonObject(),
    "label" to label?.let(::JsonPrimitive),
    "animations" to JsonArray(animations.map { it.toJsonObject() })
)

private fun JsonObject.toSceneNodePatch(): SceneNodePatch = SceneNodePatch(
    id = stringOrNull("id"),
    interactionId = stringOrNull("interactionId"),
    position = floatListOrNull("position"),
    rotation = floatListOrNull("rotation"),
    scale = floatListOrNull("scale"),
    visible = booleanOrNull("visible"),
    name = stringOrNull("name"),
    highlight = objOrNull("highlight")?.toHighlightPatch(),
    label = stringOrNull("label"),
    animations = arrayOrEmpty("animations").map { it.toSceneAnimationData() }
)

private fun HighlightPatch.toJsonObject(): JsonObject = jsonObjectOf(
    "active" to JsonPrimitive(active),
    "color" to JsonPrimitive(color),
    "intensity" to JsonPrimitive(intensity)
)

private fun JsonObject.toHighlightPatch(): HighlightPatch = HighlightPatch(
    active = booleanOrNull("active") ?: false,
    color = intOrNull("color") ?: 0xFFFFD166.toInt(),
    intensity = floatOrNull("intensity") ?: 1f
)

private fun HitVolumeData.toJsonObject(): JsonObject = jsonObjectOf(
    "shape" to JsonPrimitive(shape.serialName),
    "center" to center.toFloatJsonArray(),
    "size" to size?.toFloatJsonArray(),
    "radius" to radius?.let(::JsonPrimitive)
)

private fun JsonObject.toHitVolumeData(): HitVolumeData = HitVolumeData(
    shape = hitVolumeShapeFromSerialName(stringOrNull("shape")) ?: HitVolumeShape.BOUNDING_BOX,
    center = floatListOrNull("center") ?: listOf(0f, 0f, 0f),
    size = floatListOrNull("size"),
    radius = floatOrNull("radius")
)

private fun DragMetadata.toJsonObject(): JsonObject = jsonObjectOf(
    "enabled" to JsonPrimitive(enabled),
    "mode" to JsonPrimitive(mode.serialName),
    "planeNormal" to planeNormal?.toFloatJsonArray(),
    "planePoint" to planePoint?.toFloatJsonArray(),
    "laneAxis" to laneAxis?.toFloatJsonArray(),
    "min" to min?.let(::JsonPrimitive),
    "max" to max?.let(::JsonPrimitive),
    "dropGroups" to dropGroups.toJsonArray()
)

private fun JsonObject.toDragMetadata(): DragMetadata = DragMetadata(
    enabled = booleanOrNull("enabled") ?: true,
    mode = dragConstraintModeFromSerialName(stringOrNull("mode")) ?: DragConstraintMode.CAMERA_PLANE,
    planeNormal = floatListOrNull("planeNormal"),
    planePoint = floatListOrNull("planePoint"),
    laneAxis = floatListOrNull("laneAxis"),
    min = floatOrNull("min"),
    max = floatOrNull("max"),
    dropGroups = stringList("dropGroups")
)

private fun DropTargetMetadata.toJsonObject(): JsonObject = jsonObjectOf(
    "enabled" to JsonPrimitive(enabled),
    "targetId" to targetId?.let(::JsonPrimitive),
    "groups" to groups.toJsonArray(),
    "accepts" to accepts.toJsonArray()
)

private fun JsonObject.toDropTargetMetadata(): DropTargetMetadata = DropTargetMetadata(
    enabled = booleanOrNull("enabled") ?: true,
    targetId = stringOrNull("targetId"),
    groups = stringList("groups"),
    accepts = stringList("accepts")
)

private fun jsonObjectOf(vararg pairs: Pair<String, JsonElement?>): JsonObject =
    JsonObject(pairs.associate { (key, value) -> key to (value ?: JsonNull) })

private fun List<String>.toJsonArray(): JsonArray = JsonArray(map(::JsonPrimitive))

private fun List<Float>.toFloatJsonArray(): JsonArray = JsonArray(map(::JsonPrimitive))

private fun JsonObject.stringOrNull(name: String): String? =
    (this[name] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.contentOrNull

private fun JsonObject.booleanOrNull(name: String): Boolean? =
    (this[name] as? JsonPrimitive)?.booleanOrNull

private fun JsonObject.intOrNull(name: String): Int? =
    (this[name] as? JsonPrimitive)?.intOrNull

private fun JsonObject.floatOrNull(name: String): Float? =
    (this[name] as? JsonPrimitive)?.floatOrNull

private fun JsonObject.objOrNull(name: String): JsonObject? =
    this[name] as? JsonObject

private fun JsonObject.arrayOrEmpty(name: String): JsonArray =
    this[name] as? JsonArray ?: JsonArray(emptyList())

private fun JsonObject.stringList(name: String): List<String> =
    arrayOrEmpty(name).mapNotNull { (it as? JsonPrimitive)?.contentOrNull }

private fun JsonObject.floatListOrNull(name: String): List<Float>? =
    (this[name] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.floatOrNull }

private val CursorHint.serialName: String
    get() = when (this) {
        CursorHint.AUTO -> "auto"
        CursorHint.POINTER -> "pointer"
        CursorHint.GRAB -> "grab"
        CursorHint.GRABBING -> "grabbing"
        CursorHint.CROSSHAIR -> "crosshair"
        CursorHint.NONE -> "none"
    }

private fun cursorHintFromSerialName(value: String?): CursorHint? = when (value) {
    "auto", "AUTO" -> CursorHint.AUTO
    "pointer", "POINTER" -> CursorHint.POINTER
    "grab", "GRAB" -> CursorHint.GRAB
    "grabbing", "GRABBING" -> CursorHint.GRABBING
    "crosshair", "CROSSHAIR" -> CursorHint.CROSSHAIR
    "none", "NONE" -> CursorHint.NONE
    else -> null
}

private val HitVolumeShape.serialName: String
    get() = when (this) {
        HitVolumeShape.MESH -> "mesh"
        HitVolumeShape.BOUNDING_BOX -> "boundingBox"
        HitVolumeShape.BOX -> "box"
        HitVolumeShape.SPHERE -> "sphere"
    }

private fun hitVolumeShapeFromSerialName(value: String?): HitVolumeShape? = when (value) {
    "mesh", "MESH" -> HitVolumeShape.MESH
    "boundingBox", "BOUNDING_BOX" -> HitVolumeShape.BOUNDING_BOX
    "box", "BOX" -> HitVolumeShape.BOX
    "sphere", "SPHERE" -> HitVolumeShape.SPHERE
    else -> null
}

private val DragConstraintMode.serialName: String
    get() = when (this) {
        DragConstraintMode.CAMERA_PLANE -> "cameraPlane"
        DragConstraintMode.HORIZONTAL -> "horizontal"
        DragConstraintMode.VERTICAL -> "vertical"
        DragConstraintMode.LANE -> "lane"
    }

private fun dragConstraintModeFromSerialName(value: String?): DragConstraintMode? = when (value) {
    "cameraPlane", "CAMERA_PLANE" -> DragConstraintMode.CAMERA_PLANE
    "horizontal", "HORIZONTAL" -> DragConstraintMode.HORIZONTAL
    "vertical", "VERTICAL" -> DragConstraintMode.VERTICAL
    "lane", "LANE" -> DragConstraintMode.LANE
    else -> null
}

private val AnimationTrigger.serialName: String
    get() = when (this) {
        AnimationTrigger.SCENE_LOAD -> "sceneLoad"
        AnimationTrigger.INTERACTION -> "interaction"
        AnimationTrigger.PATCH -> "patch"
    }

private fun animationTriggerFromSerialName(value: String?): AnimationTrigger? = when (value) {
    "sceneLoad", "SCENE_LOAD" -> AnimationTrigger.SCENE_LOAD
    "interaction", "INTERACTION" -> AnimationTrigger.INTERACTION
    "patch", "PATCH" -> AnimationTrigger.PATCH
    else -> null
}

private val AnimationKind.serialName: String
    get() = when (this) {
        AnimationKind.SLIDE -> "slide"
        AnimationKind.BOB -> "bob"
        AnimationKind.THUNK -> "thunk"
        AnimationKind.BOUNCE -> "bounce"
        AnimationKind.TINT -> "tint"
        AnimationKind.PULSE -> "pulse"
        AnimationKind.SHAKE -> "shake"
        AnimationKind.GLITCH -> "glitch"
        AnimationKind.SUCCESS -> "success"
        AnimationKind.FAILURE -> "failure"
        AnimationKind.VISIBILITY -> "visibility"
    }

private fun animationKindFromSerialName(value: String?): AnimationKind? = when (value) {
    "slide", "SLIDE" -> AnimationKind.SLIDE
    "bob", "BOB" -> AnimationKind.BOB
    "thunk", "THUNK" -> AnimationKind.THUNK
    "bounce", "BOUNCE" -> AnimationKind.BOUNCE
    "tint", "TINT" -> AnimationKind.TINT
    "pulse", "PULSE" -> AnimationKind.PULSE
    "shake", "SHAKE" -> AnimationKind.SHAKE
    "glitch", "GLITCH" -> AnimationKind.GLITCH
    "success", "SUCCESS" -> AnimationKind.SUCCESS
    "failure", "FAILURE" -> AnimationKind.FAILURE
    "visibility", "VISIBILITY" -> AnimationKind.VISIBILITY
    else -> null
}

private val AnimationEasing.serialName: String
    get() = when (this) {
        AnimationEasing.LINEAR -> "linear"
        AnimationEasing.EASE_IN -> "easeIn"
        AnimationEasing.EASE_OUT -> "easeOut"
        AnimationEasing.EASE_IN_OUT -> "easeInOut"
    }

private fun animationEasingFromSerialName(value: String?): AnimationEasing? = when (value) {
    "linear", "LINEAR" -> AnimationEasing.LINEAR
    "easeIn", "EASE_IN" -> AnimationEasing.EASE_IN
    "easeOut", "EASE_OUT" -> AnimationEasing.EASE_OUT
    "easeInOut", "EASE_IN_OUT" -> AnimationEasing.EASE_IN_OUT
    else -> null
}
