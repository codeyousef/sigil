package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

/**
 * JS-specific tests for ID generation.
 * Tests crypto.randomUUID or fallback ID generation on the JS platform.
 */
class IdGeneratorJsTest {

    @Test
    fun generateNodeId_returnsNonEmptyString() {
        val id = generateNodeId()
        
        assertTrue(id.isNotEmpty(), "Generated ID should not be empty")
    }

    @Test
    fun generateNodeId_returnsUniqueIds() {
        val ids = (1..1000).map { generateNodeId() }
        val uniqueIds = ids.toSet()
        
        assertTrue(ids.size == uniqueIds.size, "All generated IDs should be unique")
    }

    @Test
    fun generateNodeId_consecutiveCalls_returnDifferentIds() {
        val id1 = generateNodeId()
        val id2 = generateNodeId()
        val id3 = generateNodeId()
        
        assertNotEquals(id1, id2)
        assertNotEquals(id2, id3)
        assertNotEquals(id1, id3)
    }

    @Test
    fun generateNodeId_hasReasonableLength() {
        val id = generateNodeId()
        
        // Should be at least 8 characters (fallback uses 9+)
        // If crypto.randomUUID is available, it will be 36 (UUID format)
        assertTrue(id.length >= 8, "ID should have reasonable length, got ${id.length}")
    }

    @Test
    fun generateNodeId_containsOnlyValidCharacters() {
        val id = generateNodeId()
        
        // Should contain only alphanumeric characters and dashes
        assertTrue(id.all { it.isLetterOrDigit() || it == '-' }, 
            "ID should contain only alphanumeric characters and dashes: $id")
    }

    @Test
    fun generateNodeId_largeVolume_remainsUnique() {
        val ids = (1..10000).map { generateNodeId() }
        val uniqueIds = ids.toSet()
        
        assertTrue(ids.size == uniqueIds.size, 
            "All 10000 generated IDs should be unique, got ${uniqueIds.size} unique out of ${ids.size}")
    }

    @Test
    fun generateNodeId_usableInScene() {
        val scene = sigilScene {
            // Use auto-generated IDs
            mesh()
            light()
            camera()
            group {
                mesh()
                mesh()
            }
        }
        
        val allNodes = scene.flattenNodes()
        val ids = allNodes.map { it.id }
        
        // All should be non-empty
        ids.forEach { id ->
            assertTrue(id.isNotEmpty(), "All auto-generated IDs should be non-empty")
        }
        
        // All should be unique
        val uniqueIds = ids.toSet()
        assertTrue(ids.size == uniqueIds.size, "All auto-generated IDs should be unique")
    }

    @Test
    fun generateNodeId_serializesAndDeserializesCorrectly() {
        val scene = sigilScene {
            mesh()
            mesh()
        }
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        // IDs should be preserved through serialization
        scene.rootNodes.forEachIndexed { index, node ->
            assertTrue(restored.rootNodes[index].id == node.id, 
                "ID should be preserved through serialization")
        }
    }
}
