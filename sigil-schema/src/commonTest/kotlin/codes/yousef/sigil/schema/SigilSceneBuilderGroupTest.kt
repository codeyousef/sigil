package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SigilSceneBuilderGroupTest {

    @Test
    fun group_empty_createsEmptyGroup() {
        val scene = sigilScene {
            group(id = "empty-group") { }
        }
        
        assertEquals(1, scene.rootNodes.size)
        val group = scene.rootNodes[0] as GroupData
        
        assertEquals("empty-group", group.id)
        assertTrue(group.children.isEmpty())
    }

    @Test
    fun group_withChildren_createsPopulatedGroup() {
        val scene = sigilScene {
            group(id = "parent") {
                mesh(id = "child-1")
                mesh(id = "child-2")
            }
        }
        
        val group = scene.rootNodes[0] as GroupData
        assertEquals(2, group.children.size)
        assertEquals("child-1", group.children[0].id)
        assertEquals("child-2", group.children[1].id)
    }

    @Test
    fun group_customTransform_appliedCorrectly() {
        val scene = sigilScene {
            group(
                id = "transformed-group",
                position = listOf(10f, 20f, 30f),
                rotation = listOf(0.5f, 1f, 1.5f),
                scale = listOf(2f, 2f, 2f),
                visible = false,
                name = "TransformedGroup"
            ) {
                mesh(id = "child")
            }
        }
        
        val group = scene.rootNodes[0] as GroupData
        
        assertEquals(listOf(10f, 20f, 30f), group.position)
        assertEquals(listOf(0.5f, 1f, 1.5f), group.rotation)
        assertEquals(listOf(2f, 2f, 2f), group.scale)
        assertFalse(group.visible)
        assertEquals("TransformedGroup", group.name)
    }

    @Test
    fun group_returnsGroupData() {
        var capturedGroup: GroupData? = null
        
        sigilScene {
            capturedGroup = group(id = "returned-group") {
                mesh(id = "child")
            }
        }
        
        assertNotNull(capturedGroup)
        assertEquals("returned-group", capturedGroup!!.id)
        assertEquals(1, capturedGroup!!.children.size)
    }

    @Test
    fun group_nested_createsHierarchy() {
        val scene = sigilScene {
            group(id = "level-1") {
                group(id = "level-2") {
                    group(id = "level-3") {
                        mesh(id = "deep-mesh")
                    }
                }
            }
        }
        
        val level1 = scene.rootNodes[0] as GroupData
        val level2 = level1.children[0] as GroupData
        val level3 = level2.children[0] as GroupData
        val deepMesh = level3.children[0] as MeshData
        
        assertEquals("level-1", level1.id)
        assertEquals("level-2", level2.id)
        assertEquals("level-3", level3.id)
        assertEquals("deep-mesh", deepMesh.id)
    }

    @Test
    fun group_mixedChildren_allTypesSupported() {
        val scene = sigilScene {
            group(id = "mixed") {
                mesh(id = "mesh-child")
                light(id = "light-child")
                group(id = "group-child") { }
            }
        }
        
        val group = scene.rootNodes[0] as GroupData
        
        assertEquals(3, group.children.size)
        assertTrue(group.children[0] is MeshData)
        assertTrue(group.children[1] is LightData)
        assertTrue(group.children[2] is GroupData)
    }
}
