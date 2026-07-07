package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Comprehensive E2E tests for SigilNodeData and its subclasses.
 * Tests serialization, deserialization, and all data class functionality.
 */
class SigilNodeDataTest {

    // ===== Polymorphic Serialization Tests =====

    @Test
    fun polymorphicSerialization_containsTypeDiscriminator() {
        val mesh = MeshData(id = "type-test")
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        
        assertTrue(json.contains("\"type\":\"mesh\""))
    }

    @Test
    fun polymorphicSerialization_differentTypesHaveCorrectDiscriminators() {
        val nodes = listOf(
            MeshData(id = "mesh") to "mesh",
            ModelData(id = "model", url = "models/room.glb") to "model",
            TextData(id = "text", text = "Label") to "text",
            GroupData(id = "group") to "group",
            LightData(id = "light") to "light",
            CameraData(id = "camera") to "camera",
            ControlsData(id = "controls") to "controls"
        )
        
        nodes.forEach { (node, expectedType) ->
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), node)
            assertTrue(json.contains("\"type\":\"$expectedType\""), "Expected type $expectedType in $json")
        }
    }

    @Test
    fun polymorphicSerialization_interactionMetadataRoundTrips() {
        val mesh = MeshData(
            id = "interactive-mesh",
            interaction = InteractionMetadata(
                interactionId = "inventory-slot-1",
                cursor = CursorHint.GRAB,
                events = listOf("select", "drop"),
                drag = DragMetadata(dropGroups = listOf("inventory")),
                dropTarget = DropTargetMetadata(
                    groups = listOf("inventory"),
                    states = DropTargetStateMetadata(
                        hover = HighlightPatch(active = true, color = 0xFF38BDF8.toInt()),
                        valid = HighlightPatch(active = true, color = 0xFF22C55E.toInt()),
                        invalid = HighlightPatch(active = true, color = 0xFFEF4444.toInt())
                    )
                )
            ),
            animations = listOf(
                SceneAnimationData(
                    kind = AnimationKind.PULSE,
                    trigger = AnimationTrigger.INTERACTION,
                    durationMs = 120
                )
            )
        )

        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData

        assertEquals("inventory-slot-1", restored.interaction?.interactionId)
        assertEquals(CursorHint.GRAB, restored.interaction?.cursor)
        assertEquals(listOf("select", "drop"), restored.interaction?.events)
        assertEquals(listOf("inventory"), restored.interaction?.drag?.dropGroups)
        assertEquals(0xFF38BDF8.toInt(), restored.interaction?.dropTarget?.states?.hover?.color)
        assertEquals(0xFF22C55E.toInt(), restored.interaction?.dropTarget?.states?.valid?.color)
        assertEquals(0xFFEF4444.toInt(), restored.interaction?.dropTarget?.states?.invalid?.color)
        assertEquals(AnimationKind.PULSE, restored.animations.single().kind)
    }

    @Test
    fun polymorphicSerialization_legacyMeshDefaultsInteractionMetadata() {
        val restored = SigilJson.decodeFromString(
            SigilNodeData.serializer(),
            """{"type":"mesh","id":"legacy-mesh"}"""
        ) as MeshData

        assertNull(restored.interaction)
        assertTrue(restored.animations.isEmpty())
    }

    @Test
    fun polymorphicSerialization_textDataRoundTrips() {
        val text = TextData(
            id = "label-1",
            position = listOf(1f, 2f, 3f),
            rotation = listOf(0.1f, 0.2f, 0.3f),
            scale = listOf(2f, 2f, 2f),
            name = "main-label",
            text = "Dispatch",
            color = 0xFF67E8F9.toInt(),
            size = 0.8f,
            depth = 0.04f,
            curveSegments = 8,
            letterSpacing = 0.03f,
            lineHeight = 1.1f,
            align = TextAlignMode.CENTER,
            baseline = TextBaselineMode.MIDDLE,
            maxWidth = 4f,
            wordWrap = true,
            facingMode = TextFacingMode.BILLBOARD,
            fontUrl = "/fonts/game.typeface.json",
            castShadow = true,
            receiveShadow = true
        )

        val json = SigilJson.encodeToString(SigilNodeData.serializer(), text)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as TextData

        assertEquals(text, restored)
    }

    @Test
    fun textData_defaultsAreStable() {
        val text = TextData(id = "label", text = "Hello")

        assertEquals(0xFFFFFFFF.toInt(), text.color)
        assertEquals(1f, text.size)
        assertEquals(0.02f, text.depth)
        assertEquals(12, text.curveSegments)
        assertEquals(TextAlignMode.LEFT, text.align)
        assertEquals(TextBaselineMode.ALPHABETIC, text.baseline)
        assertEquals(TextFacingMode.FIXED, text.facingMode)
        assertNull(text.fontUrl)
        assertTrue(!text.castShadow)
        assertTrue(!text.receiveShadow)
    }

    @Test
    fun textData_validationRejectsInvalidValues() {
        assertFailsWith<IllegalArgumentException> { TextData(id = "blank", text = " ") }
        assertFailsWith<IllegalArgumentException> { TextData(id = "size", text = "Text", size = 0f) }
        assertFailsWith<IllegalArgumentException> { TextData(id = "depth", text = "Text", depth = -0.01f) }
        assertFailsWith<IllegalArgumentException> { TextData(id = "segments", text = "Text", curveSegments = 2) }
        assertFailsWith<IllegalArgumentException> { TextData(id = "line", text = "Text", lineHeight = 0f) }
    }

    // ===== Unicode and Special Characters Tests =====

    @Test
    fun nodeData_unicodeInName_serializes() {
        val names = listOf(
            "日本語",
            "العربية",
            "中文",
            "🎮🎲🎯",
            "Special™ Characters® © 2024",
            "Line\nBreak\tTab",
            "Quote\"Test"
        )
        
        names.forEach { name ->
            val mesh = MeshData(id = "unicode-test", name = name)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
            assertEquals(name, restored.name)
        }
    }

    @Test
    fun nodeData_emptyString_inName_serializes() {
        val mesh = MeshData(id = "empty-name", name = "")
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        assertEquals("", restored.name)
    }

    @Test
    fun nodeData_veryLongName_serializes() {
        val longName = "A".repeat(10000)
        val mesh = MeshData(id = "long-name", name = longName)
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        assertEquals(longName, restored.name)
    }
}
