package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

/**
 * JVM-specific tests for ID generation.
 * Tests UUID-based ID generation on the JVM platform.
 */
class IdGeneratorJvmTest {

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
    fun generateNodeId_validUuidFormat() {
        val id = generateNodeId()
        
        // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        // Total length: 36 characters
        assertTrue(id.length == 36, "UUID should be 36 characters, got ${id.length}")
        
        // Check dashes at correct positions
        assertTrue(id[8] == '-', "Dash expected at position 8")
        assertTrue(id[13] == '-', "Dash expected at position 13")
        assertTrue(id[18] == '-', "Dash expected at position 18")
        assertTrue(id[23] == '-', "Dash expected at position 23")
        
        // Check that all other characters are hex digits
        val hexParts = id.split('-')
        hexParts.forEach { part ->
            assertTrue(part.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }, 
                "All characters should be hex digits: $part")
        }
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
}
