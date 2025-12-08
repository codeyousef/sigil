package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * Comprehensive E2E tests for SigilSceneBuilder and the DSL.
 * Tests builder methods, nested group building, and sigilScene DSL entry point.
 */
class SigilSceneBuilderTest {

    // ===== Empty Scene Tests =====

    @Test
    fun sigilScene_emptyBuilder_createsEmptyScene() {
        val scene = sigilScene { }
        
        assertTrue(scene.rootNodes.isEmpty())
        assertEquals(SceneSettings(), scene.settings)
    }

    // ===== Mesh Builder Tests =====

    @Test
    fun mesh_defaultParameters_createsValidMesh() {
        val scene = sigilScene {
            mesh()
        }
        
        assertEquals(1, scene.rootNodes.size)
        val mesh = scene.rootNodes[0] as MeshData
        
        assertNotNull(mesh.id)
        assertTrue(mesh.id.isNotEmpty())
        assertEquals(GeometryType.BOX, mesh.geometryType)
        assertEquals(listOf(0f, 0f, 0f), mesh.position)
        assertEquals(0xFFFFFFFF.toInt(), mesh.materialColor)
    }

    @Test
    fun mesh_customParameters_createsCorrectMesh() {
        val scene = sigilScene {
            mesh(
                id = "custom-mesh",
                geometryType = GeometryType.SPHERE,
                position = listOf(1f, 2f, 3f),
                rotation = listOf(0.5f, 1f, 1.5f),
                scale = listOf(2f, 2f, 2f),
                color = 0xFF0000FF.toInt(),
                metalness = 0.8f,
                roughness = 0.2f,
                geometryParams = GeometryParams(radius = 5f),
                visible = false,
                name = "MySphere",
                castShadow = false,
                receiveShadow = false
            )
        }
        
        val mesh = scene.rootNodes[0] as MeshData
        
        assertEquals("custom-mesh", mesh.id)
        assertEquals(GeometryType.SPHERE, mesh.geometryType)
        assertEquals(listOf(1f, 2f, 3f), mesh.position)
        assertEquals(listOf(0.5f, 1f, 1.5f), mesh.rotation)
        assertEquals(listOf(2f, 2f, 2f), mesh.scale)
        assertEquals(0xFF0000FF.toInt(), mesh.materialColor)
        assertEquals(0.8f, mesh.metalness)
        assertEquals(0.2f, mesh.roughness)
        assertEquals(5f, mesh.geometryParams.radius)
        assertFalse(mesh.visible)
        assertEquals("MySphere", mesh.name)
        assertFalse(mesh.castShadow)
        assertFalse(mesh.receiveShadow)
    }

    @Test
    fun mesh_returnsMeshData() {
        var capturedMesh: MeshData? = null
        
        sigilScene {
            capturedMesh = mesh(id = "returned-mesh")
        }
        
        assertNotNull(capturedMesh)
        assertEquals("returned-mesh", capturedMesh!!.id)
    }

    @Test
    fun mesh_multipleMeshes_addedInOrder() {
        val scene = sigilScene {
            mesh(id = "mesh-1")
            mesh(id = "mesh-2")
            mesh(id = "mesh-3")
        }
        
        assertEquals(3, scene.rootNodes.size)
        assertEquals("mesh-1", scene.rootNodes[0].id)
        assertEquals("mesh-2", scene.rootNodes[1].id)
        assertEquals("mesh-3", scene.rootNodes[2].id)
    }

    @Test
    fun mesh_allGeometryTypes_work() {
        GeometryType.entries.forEach { geoType ->
            val scene = sigilScene {
                mesh(geometryType = geoType)
            }
            
            val mesh = scene.rootNodes[0] as MeshData
            assertEquals(geoType, mesh.geometryType)
        }
    }

    // ===== Light Builder Tests =====

    @Test
    fun light_defaultParameters_createsValidLight() {
        val scene = sigilScene {
            light()
        }
        
        assertEquals(1, scene.rootNodes.size)
        val light = scene.rootNodes[0] as LightData
        
        assertNotNull(light.id)
        assertEquals(LightType.POINT, light.lightType)
        assertEquals(1f, light.intensity)
    }

    @Test
    fun light_customParameters_createsCorrectLight() {
        val scene = sigilScene {
            light(
                id = "custom-light",
                lightType = LightType.SPOT,
                position = listOf(0f, 10f, 0f),
                color = 0xFFFF0000.toInt(),
                intensity = 2.5f,
                distance = 50f,
                decay = 1.5f,
                angle = 0.785f,
                penumbra = 0.3f,
                castShadow = true,
                target = listOf(0f, 0f, 0f),
                visible = true,
                name = "Spotlight"
            )
        }
        
        val light = scene.rootNodes[0] as LightData
        
        assertEquals("custom-light", light.id)
        assertEquals(LightType.SPOT, light.lightType)
        assertEquals(listOf(0f, 10f, 0f), light.position)
        assertEquals(0xFFFF0000.toInt(), light.color)
        assertEquals(2.5f, light.intensity)
        assertEquals(50f, light.distance)
        assertEquals(1.5f, light.decay)
        assertEquals(0.785f, light.angle)
        assertEquals(0.3f, light.penumbra)
        assertTrue(light.castShadow)
        assertEquals(listOf(0f, 0f, 0f), light.target)
        assertEquals("Spotlight", light.name)
    }

    @Test
    fun light_allLightTypes_work() {
        LightType.entries.forEach { lightType ->
            val scene = sigilScene {
                light(lightType = lightType)
            }
            
            val light = scene.rootNodes[0] as LightData
            assertEquals(lightType, light.lightType)
        }
    }

    // ===== Camera Builder Tests =====

    @Test
    fun camera_defaultParameters_createsValidCamera() {
        val scene = sigilScene {
            camera()
        }
        
        assertEquals(1, scene.rootNodes.size)
        val camera = scene.rootNodes[0] as CameraData
        
        assertNotNull(camera.id)
        assertEquals(CameraType.PERSPECTIVE, camera.cameraType)
        assertEquals(75f, camera.fov)
    }

    @Test
    fun camera_customParameters_createsCorrectCamera() {
        val scene = sigilScene {
            camera(
                id = "custom-camera",
                cameraType = CameraType.PERSPECTIVE,
                position = listOf(0f, 5f, 10f),
                rotation = listOf(-0.3f, 0f, 0f),
                fov = 60f,
                aspect = 1.333f,
                near = 0.01f,
                far = 5000f,
                lookAt = listOf(0f, 0f, 0f),
                visible = true,
                name = "MainCamera"
            )
        }
        
        val camera = scene.rootNodes[0] as CameraData
        
        assertEquals("custom-camera", camera.id)
        assertEquals(CameraType.PERSPECTIVE, camera.cameraType)
        assertEquals(listOf(0f, 5f, 10f), camera.position)
        assertEquals(60f, camera.fov)
        assertEquals(1.333f, camera.aspect)
        assertEquals(0.01f, camera.near)
        assertEquals(5000f, camera.far)
        assertEquals(listOf(0f, 0f, 0f), camera.lookAt)
        assertEquals("MainCamera", camera.name)
    }

    @Test
    fun camera_allCameraTypes_work() {
        CameraType.entries.forEach { cameraType ->
            val scene = sigilScene {
                camera(cameraType = cameraType)
            }
            
            val camera = scene.rootNodes[0] as CameraData
            assertEquals(cameraType, camera.cameraType)
        }
    }

    // ===== Group Builder Tests =====

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

    // ===== Settings Builder Tests =====

    @Test
    fun settings_default_usesDefaultSettings() {
        val scene = sigilScene { }
        
        assertEquals(SceneSettings(), scene.settings)
    }

    @Test
    fun settings_custom_appliesCorrectly() {
        val scene = sigilScene {
            settings(
                backgroundColor = 0xFF000000.toInt(),
                fogEnabled = true,
                fogColor = 0xFFAAAAAA.toInt(),
                fogNear = 5f,
                fogFar = 500f,
                shadowsEnabled = false,
                toneMapping = ToneMappingMode.REINHARD,
                exposure = 2.5f
            )
        }
        
        assertEquals(0xFF000000.toInt(), scene.settings.backgroundColor)
        assertTrue(scene.settings.fogEnabled)
        assertEquals(0xFFAAAAAA.toInt(), scene.settings.fogColor)
        assertEquals(5f, scene.settings.fogNear)
        assertEquals(500f, scene.settings.fogFar)
        assertFalse(scene.settings.shadowsEnabled)
        assertEquals(ToneMappingMode.REINHARD, scene.settings.toneMapping)
        assertEquals(2.5f, scene.settings.exposure)
    }

    @Test
    fun settings_calledMultipleTimes_lastWins() {
        val scene = sigilScene {
            settings(backgroundColor = 0xFF111111.toInt())
            settings(backgroundColor = 0xFF222222.toInt())
            settings(backgroundColor = 0xFF333333.toInt())
        }
        
        assertEquals(0xFF333333.toInt(), scene.settings.backgroundColor)
    }

    @Test
    fun settings_allToneMappingModes_work() {
        ToneMappingMode.entries.forEach { mode ->
            val scene = sigilScene {
                settings(toneMapping = mode)
            }
            assertEquals(mode, scene.settings.toneMapping)
        }
    }

    // ===== ID Generation Tests =====

    @Test
    fun autoGeneratedIds_areUnique() {
        val scene = sigilScene {
            mesh()
            mesh()
            mesh()
            light()
            light()
            camera()
            group { }
        }
        
        val ids = scene.rootNodes.map { it.id }
        val uniqueIds = ids.toSet()
        
        assertEquals(ids.size, uniqueIds.size, "All auto-generated IDs should be unique")
    }

    @Test
    fun autoGeneratedIds_areNotEmpty() {
        val scene = sigilScene {
            mesh()
            light()
            camera()
            group { mesh() }
        }
        
        scene.flattenNodes().forEach { node ->
            assertTrue(node.id.isNotEmpty(), "Auto-generated ID should not be empty")
        }
    }

    // ===== Complex Scene Tests =====

    @Test
    fun complexScene_buildsCorrectly() {
        val scene = sigilScene {
            settings(
                backgroundColor = 0xFF87CEEB.toInt(),
                shadowsEnabled = true
            )
            
            // Ground
            mesh(
                id = "ground",
                geometryType = GeometryType.PLANE,
                geometryParams = GeometryParams(width = 100f, height = 100f),
                position = listOf(0f, 0f, 0f),
                rotation = listOf(-1.5708f, 0f, 0f)
            )
            
            // Objects group
            group(id = "objects", position = listOf(0f, 0f, 0f)) {
                mesh(id = "cube", geometryType = GeometryType.BOX, position = listOf(-2f, 0.5f, 0f))
                mesh(id = "sphere", geometryType = GeometryType.SPHERE, position = listOf(0f, 0.5f, 0f))
                mesh(id = "cylinder", geometryType = GeometryType.CYLINDER, position = listOf(2f, 0.5f, 0f))
            }
            
            // Lights
            light(id = "ambient", lightType = LightType.AMBIENT, intensity = 0.3f)
            light(id = "sun", lightType = LightType.DIRECTIONAL, position = listOf(10f, 20f, 10f))
            
            // Camera
            camera(id = "main-camera", position = listOf(0f, 5f, 10f), lookAt = listOf(0f, 0f, 0f))
        }
        
        assertEquals(5, scene.rootNodes.size) // ground, objects, ambient, sun, camera
        
        val objectsGroup = scene.rootNodes[1] as GroupData
        assertEquals(3, objectsGroup.children.size)
        
        val foundCube = scene.findNodeById("cube")
        assertNotNull(foundCube)
        assertEquals(GeometryType.BOX, (foundCube as MeshData).geometryType)
    }

    @Test
    fun builder_sceneIsImmutableAfterBuild() {
        val builder = SigilSceneBuilder()
        builder.mesh(id = "mesh-1")
        val scene1 = builder.build()
        
        // Adding more after build should not affect the already built scene
        builder.mesh(id = "mesh-2")
        val scene2 = builder.build()
        
        // Actually, the current implementation allows this - testing actual behavior
        // scene1 has 1 node, scene2 has 2 nodes (because builder is reused)
        assertEquals(1, scene1.rootNodes.size)
        assertEquals(2, scene2.rootNodes.size)
    }

    // ===== Round-Trip Tests =====

    @Test
    fun builtScene_serializesCorrectly() {
        val scene = sigilScene {
            mesh(id = "mesh-1", geometryType = GeometryType.TORUS)
            group(id = "group-1") {
                mesh(id = "nested-mesh")
                light(id = "nested-light")
            }
            settings(fogEnabled = true)
        }
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        assertEquals(scene, restored)
    }

    @Test
    fun deeplyNestedScene_serializesCorrectly() {
        val scene = sigilScene {
            group(id = "l1") {
                group(id = "l2") {
                    group(id = "l3") {
                        group(id = "l4") {
                            group(id = "l5") {
                                mesh(id = "deep-mesh")
                            }
                        }
                    }
                }
            }
        }
        
        val json = scene.toJson()
        val restored = SigilScene.fromJson(json)
        
        val found = restored.findNodeById("deep-mesh")
        assertNotNull(found)
    }
}
