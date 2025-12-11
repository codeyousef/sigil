package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive E2E tests for SigilScene and SceneSettings.
 * Tests scene construction, serialization, traversal methods, and edge cases.
 */
class SigilSceneTest {

    // ===== Empty Scene Tests =====

    @Test
    fun sigilScene_empty_serializes() {
        val scene = SigilScene()
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(scene, restored)
        assertTrue(restored.rootNodes.isEmpty())
    }

    @Test
    fun sigilScene_emptyWithDefaults_hasCorrectSettings() {
        val scene = SigilScene()
        
        assertEquals(0xFF1A1A2E.toInt(), scene.settings.backgroundColor)
        assertFalse(scene.settings.fogEnabled)
        assertEquals(0xFFFFFFFF.toInt(), scene.settings.fogColor)
        assertEquals(10f, scene.settings.fogNear)
        assertEquals(100f, scene.settings.fogFar)
        assertTrue(scene.settings.shadowsEnabled)
        assertEquals(ToneMappingMode.ACES_FILMIC, scene.settings.toneMapping)
        assertEquals(1f, scene.settings.exposure)
    }

    // ===== Single Node Tests =====

    @Test
    fun sigilScene_singleMesh_serializes() {
        val mesh = MeshData(id = "single-mesh", geometryType = GeometryType.SPHERE)
        val scene = SigilScene(rootNodes = listOf(mesh))
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(1, restored.rootNodes.size)
        assertEquals("single-mesh", restored.rootNodes[0].id)
        assertTrue(restored.rootNodes[0] is MeshData)
        assertEquals(GeometryType.SPHERE, (restored.rootNodes[0] as MeshData).geometryType)
    }

    @Test
    fun sigilScene_singleLight_serializes() {
        val light = LightData(id = "single-light", lightType = LightType.DIRECTIONAL, intensity = 2f)
        val scene = SigilScene(rootNodes = listOf(light))
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(1, restored.rootNodes.size)
        assertTrue(restored.rootNodes[0] is LightData)
        assertEquals(2f, (restored.rootNodes[0] as LightData).intensity)
    }

    @Test
    fun sigilScene_singleCamera_serializes() {
        val camera = CameraData(id = "single-camera", fov = 60f)
        val scene = SigilScene(rootNodes = listOf(camera))
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(1, restored.rootNodes.size)
        assertTrue(restored.rootNodes[0] is CameraData)
        assertEquals(60f, (restored.rootNodes[0] as CameraData).fov)
    }

    @Test
    fun sigilScene_singleGroup_serializes() {
        val group = GroupData(
            id = "single-group",
            children = listOf(MeshData(id = "child-mesh"))
        )
        val scene = SigilScene(rootNodes = listOf(group))
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(1, restored.rootNodes.size)
        assertTrue(restored.rootNodes[0] is GroupData)
        assertEquals(1, (restored.rootNodes[0] as GroupData).children.size)
    }

    // ===== Complex Scene Tests =====

    @Test
    fun sigilScene_multipleRootNodes_serializes() {
        val nodes = listOf(
            MeshData(id = "mesh-1"),
            MeshData(id = "mesh-2"),
            LightData(id = "light-1"),
            CameraData(id = "camera-1"),
            GroupData(id = "group-1", children = emptyList())
        )
        val scene = SigilScene(rootNodes = nodes)
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(5, restored.rootNodes.size)
        assertEquals("mesh-1", restored.rootNodes[0].id)
        assertEquals("mesh-2", restored.rootNodes[1].id)
        assertEquals("light-1", restored.rootNodes[2].id)
        assertEquals("camera-1", restored.rootNodes[3].id)
        assertEquals("group-1", restored.rootNodes[4].id)
    }

    @Test
    fun sigilScene_complexHierarchy_serializes() {
        val scene = SigilScene(
            rootNodes = listOf(
                GroupData(
                    id = "root-group",
                    children = listOf(
                        MeshData(id = "ground", geometryType = GeometryType.PLANE),
                        GroupData(
                            id = "objects-group",
                            children = listOf(
                                MeshData(id = "box-1", geometryType = GeometryType.BOX),
                                MeshData(id = "sphere-1", geometryType = GeometryType.SPHERE)
                            )
                        )
                    )
                ),
                LightData(id = "sun", lightType = LightType.DIRECTIONAL),
                LightData(id = "ambient", lightType = LightType.AMBIENT),
                CameraData(id = "main-camera", position = listOf(0f, 5f, 10f))
            ),
            settings = SceneSettings(
                backgroundColor = 0xFF87CEEB.toInt(),
                fogEnabled = true,
                fogNear = 50f,
                fogFar = 200f
            )
        )
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(4, restored.rootNodes.size)
        
        val rootGroup = restored.rootNodes[0] as GroupData
        assertEquals(2, rootGroup.children.size)
        
        val objectsGroup = rootGroup.children[1] as GroupData
        assertEquals(2, objectsGroup.children.size)
        
        assertTrue(restored.settings.fogEnabled)
        assertEquals(50f, restored.settings.fogNear)
    }

    // ===== JSON Handling Tests =====

    @Test
    fun sigilScene_ignoresUnknownKeys() {
        // JSON with an unknown field
        val json = """{"rootNodes":[],"settings":{"backgroundColor":-15132370},"unknownField":"should be ignored"}"""
        
        // Should not throw, ignoreUnknownKeys = true
        val scene = SigilScene.fromJson(json)
        
        assertNotNull(scene)
        assertTrue(scene.rootNodes.isEmpty())
    }

    @Test
    fun sigilScene_encodesDefaults() {
        val scene = SigilScene()
        val json = scene.toJson()
        
        // With encodeDefaults = true, even empty rootNodes should be present
        assertTrue(json.contains("rootNodes"))
        assertTrue(json.contains("settings"))
    }

    @Test
    fun sigilScene_handlesEmptyJson() {
        // Empty object should use all defaults
        val json = "{}"
        val scene = SigilScene.fromJson(json)
        
        assertTrue(scene.rootNodes.isEmpty())
        assertEquals(SceneSettings(), scene.settings)
    }

    // ===== Large Scene Tests =====

    @Test
    fun sigilScene_manyNodes_serializes() {
        val nodes = (1..100).map { i ->
            MeshData(
                id = "mesh-$i",
                position = listOf(i.toFloat(), 0f, 0f),
                geometryType = GeometryType.entries[i % GeometryType.entries.size]
            )
        }
        val scene = SigilScene(rootNodes = nodes)
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(100, restored.rootNodes.size)
        
        // Verify a few random nodes
        assertEquals("mesh-1", restored.rootNodes[0].id)
        assertEquals("mesh-50", restored.rootNodes[49].id)
        assertEquals("mesh-100", restored.rootNodes[99].id)
    }

    // ===== Data Class Tests =====

    @Test
    fun sigilScene_equality_worksCorrectly() {
        val scene1 = SigilScene(rootNodes = listOf(MeshData(id = "mesh")))
        val scene2 = SigilScene(rootNodes = listOf(MeshData(id = "mesh")))
        val scene3 = SigilScene(rootNodes = listOf(MeshData(id = "different")))
        
        assertEquals(scene1, scene2)
        assertTrue(scene1 != scene3)
    }

    @Test
    fun sigilScene_copy_worksCorrectly() {
        val original = SigilScene(
            rootNodes = listOf(MeshData(id = "original")),
            settings = SceneSettings(backgroundColor = 0xFF000000.toInt())
        )
        
        val copied = original.copy(
            settings = SceneSettings(backgroundColor = 0xFFFFFFFF.toInt())
        )
        
        assertEquals(0xFFFFFFFF.toInt(), copied.settings.backgroundColor)
        assertEquals(0xFF000000.toInt(), original.settings.backgroundColor)
        assertEquals(original.rootNodes, copied.rootNodes)
    }
}
