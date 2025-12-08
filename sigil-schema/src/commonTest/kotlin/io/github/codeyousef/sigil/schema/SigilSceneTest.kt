package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

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

    // ===== SceneSettings Tests =====

    @Test
    fun sceneSettings_defaultValues_areCorrect() {
        val settings = SceneSettings()
        
        assertEquals(0xFF1A1A2E.toInt(), settings.backgroundColor)
        assertFalse(settings.fogEnabled)
        assertEquals(0xFFFFFFFF.toInt(), settings.fogColor)
        assertEquals(10f, settings.fogNear)
        assertEquals(100f, settings.fogFar)
        assertTrue(settings.shadowsEnabled)
        assertEquals(ToneMappingMode.ACES_FILMIC, settings.toneMapping)
        assertEquals(1f, settings.exposure)
    }

    @Test
    fun sceneSettings_customValues_serialize() {
        val settings = SceneSettings(
            backgroundColor = 0xFF000000.toInt(),
            fogEnabled = true,
            fogColor = 0xFFAAAAAA.toInt(),
            fogNear = 5f,
            fogFar = 500f,
            shadowsEnabled = false,
            toneMapping = ToneMappingMode.REINHARD,
            exposure = 2.5f
        )
        val scene = SigilScene(settings = settings)
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(0xFF000000.toInt(), restored.settings.backgroundColor)
        assertTrue(restored.settings.fogEnabled)
        assertEquals(0xFFAAAAAA.toInt(), restored.settings.fogColor)
        assertEquals(5f, restored.settings.fogNear)
        assertEquals(500f, restored.settings.fogFar)
        assertFalse(restored.settings.shadowsEnabled)
        assertEquals(ToneMappingMode.REINHARD, restored.settings.toneMapping)
        assertEquals(2.5f, restored.settings.exposure)
    }

    @Test
    fun sceneSettings_allToneMappingModes_serialize() {
        ToneMappingMode.entries.forEach { mode ->
            val scene = SigilScene(settings = SceneSettings(toneMapping = mode))
            val json = scene.toJson()
            val restored = SigilScene.fromJson(json)
            assertEquals(mode, restored.settings.toneMapping)
        }
    }

    @Test
    fun sceneSettings_edgeCaseExposure_serializes() {
        val exposures = listOf(0f, 0.001f, 1f, 10f, 100f, Float.MAX_VALUE)
        
        exposures.forEach { exposure ->
            val scene = SigilScene(settings = SceneSettings(exposure = exposure))
            val json = scene.toJson()
            val restored = SigilScene.fromJson(json)
            assertEquals(exposure, restored.settings.exposure)
        }
    }

    @Test
    fun sceneSettings_fogDistanceEdgeCases_serialize() {
        // Test fog near >= fog far (edge case that should still serialize)
        val scene = SigilScene(
            settings = SceneSettings(fogEnabled = true, fogNear = 100f, fogFar = 50f)
        )
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(100f, restored.settings.fogNear)
        assertEquals(50f, restored.settings.fogFar)
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

    @Test
    fun sigilScene_flattenNodes_manyNodes_returnsAll() {
        val nodes = (1..50).map { MeshData(id = "mesh-$it") }
        val scene = SigilScene(rootNodes = nodes)
        
        val flattened = scene.flattenNodes()
        
        assertEquals(50, flattened.size)
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

    @Test
    fun sceneSettings_equality_worksCorrectly() {
        val s1 = SceneSettings(fogEnabled = true, fogNear = 10f)
        val s2 = SceneSettings(fogEnabled = true, fogNear = 10f)
        val s3 = SceneSettings(fogEnabled = false, fogNear = 10f)
        
        assertEquals(s1, s2)
        assertTrue(s1 != s3)
    }
}
