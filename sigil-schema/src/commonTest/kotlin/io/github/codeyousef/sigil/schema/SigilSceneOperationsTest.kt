package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SigilSceneOperationsTest {

    // ===== findNodeById Tests =====

    @Test
    fun findNodeById_rootLevel_findsNode() {
        val scene = SigilScene(
            rootNodes = listOf(
                MeshData(id = "mesh-1"),
                LightData(id = "light-1"),
                CameraData(id = "camera-1")
            )
        )
        
        val found = scene.findNodeById("light-1")
        
        assertNotNull(found)
        assertEquals("light-1", found.id)
        assertTrue(found is LightData)
    }

    @Test
    fun findNodeById_nestedInGroup_findsNode() {
        val scene = SigilScene(
            rootNodes = listOf(
                GroupData(
                    id = "parent-group",
                    children = listOf(
                        MeshData(id = "nested-mesh")
                    )
                )
            )
        )
        
        val found = scene.findNodeById("nested-mesh")
        
        assertNotNull(found)
        assertEquals("nested-mesh", found.id)
    }

    @Test
    fun findNodeById_deeplyNested_findsNode() {
        val scene = SigilScene(
            rootNodes = listOf(
                GroupData(
                    id = "level-1",
                    children = listOf(
                        GroupData(
                            id = "level-2",
                            children = listOf(
                                GroupData(
                                    id = "level-3",
                                    children = listOf(
                                        MeshData(id = "deep-mesh")
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        
        val found = scene.findNodeById("deep-mesh")
        
        assertNotNull(found)
        assertEquals("deep-mesh", found.id)
    }

    @Test
    fun findNodeById_nonExistent_returnsNull() {
        val scene = SigilScene(
            rootNodes = listOf(
                MeshData(id = "mesh-1"),
                GroupData(id = "group-1", children = listOf(MeshData(id = "mesh-2")))
            )
        )
        
        val found = scene.findNodeById("non-existent")
        
        assertNull(found)
    }

    @Test
    fun findNodeById_emptyScene_returnsNull() {
        val scene = SigilScene()
        
        val found = scene.findNodeById("any-id")
        
        assertNull(found)
    }

    @Test
    fun findNodeById_findsGroup_notJustChildren() {
        val scene = SigilScene(
            rootNodes = listOf(
                GroupData(
                    id = "target-group",
                    children = listOf(MeshData(id = "child"))
                )
            )
        )
        
        val found = scene.findNodeById("target-group")
        
        assertNotNull(found)
        assertTrue(found is GroupData)
    }

    // ===== flattenNodes Tests =====

    @Test
    fun flattenNodes_emptyScene_returnsEmpty() {
        val scene = SigilScene()
        
        val flattened = scene.flattenNodes()
        
        assertTrue(flattened.isEmpty())
    }

    @Test
    fun flattenNodes_singleNode_returnsSingleElement() {
        val scene = SigilScene(rootNodes = listOf(MeshData(id = "single")))
        
        val flattened = scene.flattenNodes()
        
        assertEquals(1, flattened.size)
        assertEquals("single", flattened[0].id)
    }

    @Test
    fun flattenNodes_multipleRootNodes_returnsAll() {
        val scene = SigilScene(
            rootNodes = listOf(
                MeshData(id = "mesh-1"),
                LightData(id = "light-1"),
                CameraData(id = "camera-1")
            )
        )
        
        val flattened = scene.flattenNodes()
        
        assertEquals(3, flattened.size)
    }

    @Test
    fun flattenNodes_nestedGroups_includesAllNodes() {
        val scene = SigilScene(
            rootNodes = listOf(
                GroupData(
                    id = "group-1",
                    children = listOf(
                        MeshData(id = "mesh-1"),
                        GroupData(
                            id = "group-2",
                            children = listOf(
                                MeshData(id = "mesh-2"),
                                MeshData(id = "mesh-3")
                            )
                        )
                    )
                ),
                LightData(id = "light-1")
            )
        )
        
        val flattened = scene.flattenNodes()
        
        // group-1, mesh-1, group-2, mesh-2, mesh-3, light-1 = 6 nodes
        assertEquals(6, flattened.size)
        
        val ids = flattened.map { it.id }
        assertTrue("group-1" in ids)
        assertTrue("mesh-1" in ids)
        assertTrue("group-2" in ids)
        assertTrue("mesh-2" in ids)
        assertTrue("mesh-3" in ids)
        assertTrue("light-1" in ids)
    }

    @Test
    fun flattenNodes_preservesOrder_depthFirst() {
        val scene = SigilScene(
            rootNodes = listOf(
                GroupData(
                    id = "A",
                    children = listOf(
                        MeshData(id = "B"),
                        MeshData(id = "C")
                    )
                ),
                MeshData(id = "D")
            )
        )
        
        val flattened = scene.flattenNodes()
        val ids = flattened.map { it.id }
        
        // Should be: A, B, C, D (depth-first)
        assertEquals(listOf("A", "B", "C", "D"), ids)
    }

    @Test
    fun sigilScene_flattenNodes_manyNodes_returnsAll() {
        val nodes = (1..50).map { MeshData(id = "mesh-$it") }
        val scene = SigilScene(rootNodes = nodes)
        
        val flattened = scene.flattenNodes()
        
        assertEquals(50, flattened.size)
    }
}
