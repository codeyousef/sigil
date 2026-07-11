package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SigilRuntimeDataTest {
    @Test
    fun sceneRoundTripPreservesScreenAudioAndFrameStatsNodes() {
        val scene = SigilScene(
            rootNodes = listOf(
                ScreenLayerData(
                    id = "hud",
                    desktop = ScreenLayoutData(ScreenAnchor.TOP_RIGHT, 20f, 24f),
                    mobile = ScreenLayoutData(ScreenAnchor.BOTTOM_CENTER, 0f, 18f, scale = 0.8f),
                    order = 4,
                    children = listOf(
                        TextData(id = "status", text = "READY"),
                        FrameStatsTextData(id = "fps", prefix = "FPS:")
                    )
                ),
                AudioBusData(id = "sfx-bus", bus = "sfx", volume = 0.35f, storageKey = "sound"),
                AudioData(
                    id = "confirm",
                    procedural = ProceduralAudioData(
                        waveform = ProceduralWaveform.SQUARE,
                        startFrequencyHz = 220f,
                        endFrequencyHz = 660f
                    ),
                    bus = "sfx"
                )
            ),
            settings = SceneSettings(
                rendererPreference = RendererPreference.WEBGL,
                adaptiveResolution = AdaptiveResolutionData(targetFps = 55f)
            )
        )

        val restored = SigilScene.fromJson(scene.toJson())

        assertEquals(scene, restored)
        assertIs<ScreenLayerData>(restored.rootNodes.first())
        assertEquals("fps", restored.findNodeById("fps")?.id)
    }

    @Test
    fun patchRoundTripPreservesRuntimeCommandsAndDynamicNodeFields() {
        val patch = ScenePatch(
            nodes = listOf(
                SceneNodePatch(
                    interactionId = "package:1",
                    text = "FOCUS PACKAGE 1",
                    modelUrl = "/models/package-b.glb",
                    interactionEnabled = false,
                    label = "legacy alias"
                )
            ),
            camera = CameraPatch(
                position = listOf(4f, 5f, 6f),
                lookAt = listOf(0f, 1f, 0f),
                durationMs = 600,
                cancelMomentum = true
            ),
            audio = listOf(
                AudioPatch(
                    action = AudioPatchAction.SET_VOLUME,
                    bus = "ambience",
                    volume = 0.25f
                )
            ),
            storage = listOf(
                StoragePatch(
                    action = StoragePatchAction.SET,
                    key = "fifth-wall-progress-v1",
                    value = "{\"bay\":2}",
                    backend = StorageBackend.COOKIE,
                    expiresDays = 30
                )
            )
        )

        val restored = ScenePatch.fromJson(patch.toJson())

        assertEquals(patch, restored)
        assertFalse(restored.isEmpty)
    }

    @Test
    fun legacyLabelRemainsAValidTextAlias() {
        val patch = ScenePatch.fromJson("""{"nodes":[{"id":"status","label":"READY"}]}""")

        assertEquals("READY", patch.nodes.single().label)
        assertEquals(null, patch.nodes.single().text)
    }

    @Test
    fun builderPreservesResponsiveLayerChildrenAndModelPreloads() {
        val scene = sigilScene {
            screenLayer(
                id = "hud",
                mobile = ScreenLayoutData(ScreenAnchor.BOTTOM_LEFT, 12f, 12f)
            ) {
                text(id = "hud-title", text = "MANIFEST")
                model(
                    id = "preview",
                    url = "/models/a.glb",
                    preloadUrls = listOf("/models/b.glb", "/models/c.glb")
                )
            }
        }

        val layer = assertIs<ScreenLayerData>(scene.rootNodes.single())
        assertEquals(listOf("hud-title", "preview"), layer.children.map(SigilNodeData::id))
        assertEquals(listOf("/models/b.glb", "/models/c.glb"), (layer.children[1] as ModelData).preloadUrls)
    }

    @Test
    fun runtimeDataValidationRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> {
            AdaptiveResolutionData(minimumDpr = 1.25f, maximumDpr = 0.75f)
        }
        assertFailsWith<IllegalArgumentException> {
            AudioData(id = "silent", procedural = ProceduralAudioData(oscillatorGain = 0f, noiseGain = 0f))
        }
        assertFailsWith<IllegalArgumentException> {
            StoragePatch(StoragePatchAction.SET, key = "checkpoint")
        }
        assertTrue(ScenePatch().isEmpty)
    }
}
