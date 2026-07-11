package codes.yousef.sigil.summon.components

import codes.yousef.sigil.schema.AudioBusData
import codes.yousef.sigil.schema.AudioData
import codes.yousef.sigil.schema.FrameStatsTextData
import codes.yousef.sigil.schema.InteractionMetadata
import codes.yousef.sigil.schema.ProceduralAudioData
import codes.yousef.sigil.schema.SceneAnimationData
import codes.yousef.sigil.schema.ScreenLayerData
import codes.yousef.sigil.schema.ScreenLayoutData
import codes.yousef.sigil.schema.SigilNodeData
import codes.yousef.sigil.schema.StorageBackend
import codes.yousef.sigil.schema.TextAlignMode
import codes.yousef.sigil.schema.TextBaselineMode
import codes.yousef.sigil.schema.generateNodeId
import codes.yousef.sigil.summon.context.SigilSummonContext
import codes.yousef.summon.annotation.Composable

/** Canvas-native responsive HUD layer rendered by Materia after the world scene. */
@Composable
fun SigilScreenLayer(
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    visible: Boolean = true,
    name: String? = null,
    interaction: InteractionMetadata? = null,
    animations: List<SceneAnimationData> = emptyList(),
    desktop: ScreenLayoutData = ScreenLayoutData(),
    mobile: ScreenLayoutData = desktop,
    mobileBreakpoint: Int = 640,
    order: Int = 0,
    clearDepth: Boolean = true,
    id: String = generateNodeId(),
    content: @Composable () -> String
): String {
    val context = SigilSummonContext.current()
    val children = mutableListOf<SigilNodeData>()
    context.enterGroup(children)
    try {
        content()
    } finally {
        context.exitGroup()
    }
    context.registerNode(
        ScreenLayerData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            desktop = desktop,
            mobile = mobile,
            mobileBreakpoint = mobileBreakpoint,
            order = order,
            clearDepth = clearDepth,
            children = children.toList()
        )
    )
    return ""
}

/** Smoothed FPS text rendered as regular Materia text geometry. */
@Composable
fun SigilFrameStatsText(
    position: List<Float> = listOf(0f, 0f, 0f),
    rotation: List<Float> = listOf(0f, 0f, 0f),
    scale: List<Float> = listOf(1f, 1f, 1f),
    visible: Boolean = true,
    prefix: String = "FPS ",
    decimalPlaces: Int = 0,
    updateIntervalMs: Int = 250,
    color: Int = 0xFFFFFFFF.toInt(),
    size: Float = 18f,
    depth: Float = 0.01f,
    align: TextAlignMode = TextAlignMode.LEFT,
    baseline: TextBaselineMode = TextBaselineMode.TOP,
    fontUrl: String? = null,
    name: String? = null,
    interaction: InteractionMetadata? = null,
    animations: List<SceneAnimationData> = emptyList(),
    id: String = generateNodeId()
): String {
    SigilSummonContext.current().registerNode(
        FrameStatsTextData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            prefix = prefix,
            decimalPlaces = decimalPlaces,
            updateIntervalMs = updateIntervalMs,
            color = color,
            size = size,
            depth = depth,
            align = align,
            baseline = baseline,
            fontUrl = fontUrl
        )
    )
    return ""
}

/** Buffered or procedural Materia audio source. */
@Composable
fun SigilAudio(
    url: String? = null,
    procedural: ProceduralAudioData? = null,
    position: List<Float> = listOf(0f, 0f, 0f),
    volume: Float = 1f,
    loop: Boolean = false,
    autoplay: Boolean = false,
    bus: String = "master",
    positional: Boolean = false,
    refDistance: Float = 1f,
    maxDistance: Float = 10000f,
    rolloffFactor: Float = 1f,
    visible: Boolean = true,
    name: String? = null,
    id: String = generateNodeId()
): String {
    SigilSummonContext.current().registerNode(
        AudioData(
            id = id,
            position = position,
            visible = visible,
            name = name,
            url = url,
            procedural = procedural,
            bus = bus,
            volume = volume,
            loop = loop,
            autoplay = autoplay,
            positional = positional,
            refDistance = refDistance,
            maxDistance = maxDistance,
            rolloffFactor = rolloffFactor
        )
    )
    return ""
}

/** Named gain bus. A storage key restores and persists its volume. */
@Composable
fun SigilSoundBus(
    bus: String,
    volume: Float = 1f,
    storageKey: String? = null,
    storageBackend: StorageBackend = StorageBackend.LOCAL_STORAGE,
    id: String = generateNodeId()
): String {
    SigilSummonContext.current().registerNode(
        AudioBusData(
            id = id,
            bus = bus,
            volume = volume,
            storageKey = storageKey,
            storageBackend = storageBackend
        )
    )
    return ""
}
