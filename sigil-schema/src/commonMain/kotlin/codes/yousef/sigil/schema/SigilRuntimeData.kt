package codes.yousef.sigil.schema

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RendererPreference {
    @SerialName("auto") AUTO,
    @SerialName("webgl") WEBGL,
    @SerialName("webgpu") WEBGPU
}

@Serializable
data class AdaptiveResolutionData(
    val enabled: Boolean = true,
    val targetFps: Float = 55f,
    val minimumDpr: Float = 0.75f,
    val maximumDpr: Float = 1.25f,
    val scaleStep: Float = 0.1f,
    val sampleWindow: Int = 60
) {
    init {
        require(targetFps > 0f) { "AdaptiveResolutionData.targetFps must be positive" }
        require(minimumDpr > 0f) { "AdaptiveResolutionData.minimumDpr must be positive" }
        require(maximumDpr >= minimumDpr) {
            "AdaptiveResolutionData.maximumDpr must be at least minimumDpr"
        }
        require(scaleStep > 0f) { "AdaptiveResolutionData.scaleStep must be positive" }
        require(sampleWindow > 0) { "AdaptiveResolutionData.sampleWindow must be positive" }
    }
}

@Serializable
enum class ScreenAnchor {
    @SerialName("topLeft") TOP_LEFT,
    @SerialName("topCenter") TOP_CENTER,
    @SerialName("topRight") TOP_RIGHT,
    @SerialName("centerLeft") CENTER_LEFT,
    @SerialName("center") CENTER,
    @SerialName("centerRight") CENTER_RIGHT,
    @SerialName("bottomLeft") BOTTOM_LEFT,
    @SerialName("bottomCenter") BOTTOM_CENTER,
    @SerialName("bottomRight") BOTTOM_RIGHT
}

/** Pixel-space layout applied to a screen layer root. */
@Serializable
data class ScreenLayoutData(
    val anchor: ScreenAnchor = ScreenAnchor.TOP_LEFT,
    val offsetX: Float = 24f,
    val offsetY: Float = 24f,
    val scale: Float = 1f,
    val visible: Boolean = true
) {
    init {
        require(scale > 0f) { "ScreenLayoutData.scale must be positive" }
    }
}

/** A canvas-native orthographic layer rendered after the world scene. */
@Serializable
@SerialName("screenLayer")
data class ScreenLayerData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,
    override val interaction: InteractionMetadata? = null,
    override val animations: List<SceneAnimationData> = emptyList(),
    val desktop: ScreenLayoutData = ScreenLayoutData(),
    val mobile: ScreenLayoutData = desktop,
    val mobileBreakpoint: Int = 640,
    val order: Int = 0,
    val clearDepth: Boolean = true,
    val children: List<SigilNodeData> = emptyList()
) : SigilNodeData() {
    init {
        require(mobileBreakpoint > 0) { "ScreenLayerData.mobileBreakpoint must be positive" }
    }
}

/** Dynamic text node backed by Materia's smoothed frame statistics. */
@Serializable
@SerialName("frameStatsText")
data class FrameStatsTextData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,
    override val interaction: InteractionMetadata? = null,
    override val animations: List<SceneAnimationData> = emptyList(),
    val prefix: String = "FPS ",
    val decimalPlaces: Int = 0,
    val updateIntervalMs: Int = 250,
    val color: Int = 0xFFFFFFFF.toInt(),
    val size: Float = 18f,
    val depth: Float = 0.01f,
    val align: TextAlignMode = TextAlignMode.LEFT,
    val baseline: TextBaselineMode = TextBaselineMode.TOP,
    val fontUrl: String? = null
) : SigilNodeData() {
    init {
        require(decimalPlaces in 0..3) { "FrameStatsTextData.decimalPlaces must be between 0 and 3" }
        require(updateIntervalMs > 0) { "FrameStatsTextData.updateIntervalMs must be positive" }
        require(size > 0f) { "FrameStatsTextData.size must be positive" }
        require(depth >= 0f) { "FrameStatsTextData.depth must be non-negative" }
    }
}

@Serializable
enum class ProceduralWaveform {
    @SerialName("sine") SINE,
    @SerialName("square") SQUARE,
    @SerialName("sawtooth") SAWTOOTH,
    @SerialName("triangle") TRIANGLE
}

@Serializable
data class ProceduralAudioData(
    val waveform: ProceduralWaveform = ProceduralWaveform.SINE,
    val startFrequencyHz: Float = 440f,
    val endFrequencyHz: Float = startFrequencyHz,
    val durationSeconds: Float = 0.25f,
    val attackSeconds: Float = 0.01f,
    val releaseSeconds: Float = 0.05f,
    val oscillatorGain: Float = 1f,
    val noiseGain: Float = 0f,
    val lowPassFrequencyHz: Float? = null
) {
    init {
        require(startFrequencyHz > 0f && endFrequencyHz > 0f) {
            "ProceduralAudioData frequencies must be positive"
        }
        require(durationSeconds > 0f) { "ProceduralAudioData.durationSeconds must be positive" }
        require(attackSeconds >= 0f && releaseSeconds >= 0f) {
            "ProceduralAudioData envelope times must be non-negative"
        }
        require(oscillatorGain >= 0f && noiseGain >= 0f) {
            "ProceduralAudioData gains must be non-negative"
        }
        require(oscillatorGain > 0f || noiseGain > 0f) {
            "ProceduralAudioData requires oscillatorGain or noiseGain"
        }
        require(lowPassFrequencyHz == null || lowPassFrequencyHz > 0f) {
            "ProceduralAudioData.lowPassFrequencyHz must be positive"
        }
    }
}

/** Buffered or procedurally generated browser audio source. */
@Serializable
@SerialName("audio")
data class AudioData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,
    override val interaction: InteractionMetadata? = null,
    override val animations: List<SceneAnimationData> = emptyList(),
    val url: String? = null,
    val procedural: ProceduralAudioData? = null,
    val bus: String = "master",
    val volume: Float = 1f,
    val loop: Boolean = false,
    val autoplay: Boolean = false,
    val positional: Boolean = false,
    val refDistance: Float = 1f,
    val maxDistance: Float = 10000f,
    val rolloffFactor: Float = 1f
) : SigilNodeData() {
    init {
        require(!url.isNullOrBlank() || procedural != null) {
            "AudioData requires a nonblank url or a procedural source"
        }
        require(url.isNullOrBlank() || procedural == null) {
            "AudioData accepts either url or procedural, not both"
        }
        require(bus.isNotBlank()) { "AudioData.bus must not be blank" }
        require(volume in 0f..1f) { "AudioData.volume must be between 0 and 1" }
        require(refDistance > 0f) { "AudioData.refDistance must be positive" }
        require(maxDistance >= refDistance) { "AudioData.maxDistance must be at least refDistance" }
        require(rolloffFactor >= 0f) { "AudioData.rolloffFactor must be non-negative" }
    }
}

/** Named Web Audio gain bus with optional local-storage persistence. */
@Serializable
@SerialName("audioBus")
data class AudioBusData(
    override val id: String,
    override val position: List<Float> = listOf(0f, 0f, 0f),
    override val rotation: List<Float> = listOf(0f, 0f, 0f),
    override val scale: List<Float> = listOf(1f, 1f, 1f),
    override val visible: Boolean = true,
    override val name: String? = null,
    override val interaction: InteractionMetadata? = null,
    override val animations: List<SceneAnimationData> = emptyList(),
    val bus: String,
    val volume: Float = 1f,
    val storageKey: String? = null,
    val storageBackend: StorageBackend = StorageBackend.LOCAL_STORAGE
) : SigilNodeData() {
    init {
        require(bus.isNotBlank()) { "AudioBusData.bus must not be blank" }
        require(volume in 0f..1f) { "AudioBusData.volume must be between 0 and 1" }
        require(storageKey == null || storageKey.isNotBlank()) {
            "AudioBusData.storageKey must be null or nonblank"
        }
    }
}
