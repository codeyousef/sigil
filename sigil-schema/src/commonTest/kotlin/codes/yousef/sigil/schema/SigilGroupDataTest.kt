package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SigilGroupDataTest {

    @Test
    fun groupData_emptyGroup_serializes() {
        val group = GroupData(id = "empty-group", children = emptyList())
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), group)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as GroupData
        
        assertEquals("empty-group", restored.id)
        assertTrue(restored.children.isEmpty())
    }

    @Test
    fun groupData_withChildren_serializes() {
        val group = GroupData(
            id = "parent-group",
            position = listOf(1f, 2f, 3f),
            children = listOf(
                MeshData(id = "child-mesh-1"),
                MeshData(id = "child-mesh-2", geometryType = GeometryType.SPHERE)
            )
        )
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), group)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as GroupData
        
        assertEquals("parent-group", restored.id)
        assertEquals(2, restored.children.size)
        assertEquals("child-mesh-1", restored.children[0].id)
        assertEquals("child-mesh-2", restored.children[1].id)
        assertEquals(GeometryType.SPHERE, (restored.children[1] as MeshData).geometryType)
    }

    @Test
    fun groupData_nestedGroups_serialize() {
        val innerGroup = GroupData(
            id = "inner-group",
            children = listOf(MeshData(id = "inner-mesh"))
        )
        val outerGroup = GroupData(
            id = "outer-group",
            children = listOf(innerGroup, MeshData(id = "outer-mesh"))
        )
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), outerGroup)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as GroupData
        
        assertEquals(2, restored.children.size)
        val restoredInner = restored.children[0] as GroupData
        assertEquals("inner-group", restoredInner.id)
        assertEquals(1, restoredInner.children.size)
        assertEquals("inner-mesh", restoredInner.children[0].id)
    }

    @Test
    fun groupData_deeplyNestedGroups_serialize() {
        // Create 10-level deep nesting
        fun createNestedGroup(level: Int): GroupData {
            return if (level <= 0) {
                GroupData(id = "leaf-group", children = listOf(MeshData(id = "leaf-mesh")))
            } else {
                GroupData(id = "level-$level", children = listOf(createNestedGroup(level - 1)))
            }
        }
        
        val deepGroup = createNestedGroup(10)
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), deepGroup)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as GroupData
        
        // Traverse to verify
        var current: GroupData = restored
        for (level in 10 downTo 1) {
            assertEquals("level-$level", current.id)
            current = current.children[0] as GroupData
        }
        assertEquals("leaf-group", current.id)
        assertEquals("leaf-mesh", current.children[0].id)
    }

    @Test
    fun groupData_mixedChildTypes_serialize() {
        val group = GroupData(
            id = "mixed-group",
            children = listOf(
                MeshData(id = "mesh-child"),
                LightData(id = "light-child", lightType = LightType.POINT),
                CameraData(id = "camera-child"),
                GroupData(id = "group-child", children = emptyList())
            )
        )
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), group)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as GroupData
        
        assertEquals(4, restored.children.size)
        assertTrue(restored.children[0] is MeshData)
        assertTrue(restored.children[1] is LightData)
        assertTrue(restored.children[2] is CameraData)
        assertTrue(restored.children[3] is GroupData)
    }
}
