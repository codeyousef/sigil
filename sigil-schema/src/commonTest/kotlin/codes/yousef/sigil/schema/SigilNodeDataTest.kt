package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
                dropTarget = DropTargetMetadata(groups = listOf("inventory"))
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
