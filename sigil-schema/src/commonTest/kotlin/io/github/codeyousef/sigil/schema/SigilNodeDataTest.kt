package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
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
            GroupData(id = "group") to "group",
            LightData(id = "light") to "light",
            CameraData(id = "camera") to "camera"
        )
        
        nodes.forEach { (node, expectedType) ->
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), node)
            assertTrue(json.contains("\"type\":\"$expectedType\""), "Expected type $expectedType in $json")
        }
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
