package codes.yousef.sigil.summon.context

import codes.yousef.sigil.schema.AudioBusData
import codes.yousef.sigil.schema.AudioData
import codes.yousef.sigil.schema.FrameStatsTextData
import codes.yousef.sigil.schema.ProceduralAudioData
import codes.yousef.sigil.schema.ScreenAnchor
import codes.yousef.sigil.schema.ScreenLayerData
import codes.yousef.sigil.schema.ScreenLayoutData
import codes.yousef.sigil.schema.TextData
import codes.yousef.sigil.summon.components.SigilAudio
import codes.yousef.sigil.summon.components.SigilFrameStatsText
import codes.yousef.sigil.summon.components.SigilScreenLayer
import codes.yousef.sigil.summon.components.SigilSoundBus
import codes.yousef.sigil.summon.components.SigilText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SigilRuntimeComponentsTest {
    @Test
    fun runtimeComponentsRegisterCanvasNativeNodes() {
        val context = SigilSummonContext.createServerContext()

        SigilSummonContext.withContext(context) {
            SigilSoundBus(id = "bus", bus = "sfx", volume = 0.4f, storageKey = "sound-mode")
            SigilAudio(
                id = "cue",
                procedural = ProceduralAudioData(startFrequencyHz = 220f, endFrequencyHz = 440f),
                bus = "sfx"
            )
            SigilScreenLayer(
                id = "hud",
                desktop = ScreenLayoutData(ScreenAnchor.TOP_RIGHT, 24f, 24f),
                mobile = ScreenLayoutData(ScreenAnchor.BOTTOM_CENTER, 0f, 16f)
            ) {
                SigilText(id = "manifest", text = "MANIFEST")
                SigilFrameStatsText(id = "fps", prefix = "FPS ")
            }
        }

        assertIs<AudioBusData>(context.nodes[0])
        assertIs<AudioData>(context.nodes[1])
        val layer = assertIs<ScreenLayerData>(context.nodes[2])
        assertIs<TextData>(layer.children[0])
        assertIs<FrameStatsTextData>(layer.children[1])
        assertEquals(listOf("manifest", "fps"), layer.children.map { it.id })
    }
}
