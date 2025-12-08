package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * Comprehensive E2E tests for SigilNodeData and its subclasses.
 * Tests serialization, deserialization, and all data class functionality.
 */
class SigilNodeDataTest {

    // ===== MeshData Tests =====

    @Test
    fun meshData_defaultValues_areCorrect() {
        val mesh = MeshData(id = "test-mesh")
        
        assertEquals("test-mesh", mesh.id)
        assertEquals(listOf(0f, 0f, 0f), mesh.position)
        assertEquals(listOf(0f, 0f, 0f), mesh.rotation)
        assertEquals(listOf(1f, 1f, 1f), mesh.scale)
        assertTrue(mesh.visible)
        assertNull(mesh.name)
        assertEquals(GeometryType.BOX, mesh.geometryType)
        assertEquals(0xFFFFFFFF.toInt(), mesh.materialColor)
        assertEquals(0f, mesh.metalness)
        assertEquals(1f, mesh.roughness)
        assertTrue(mesh.castShadow)
        assertTrue(mesh.receiveShadow)
    }

    @Test
    fun meshData_customValues_arePreserved() {
        val mesh = MeshData(
            id = "custom-mesh",
            position = listOf(1f, 2f, 3f),
            rotation = listOf(0.5f, 1f, 1.5f),
            scale = listOf(2f, 2f, 2f),
            visible = false,
            name = "MyMesh",
            geometryType = GeometryType.SPHERE,
            geometryParams = GeometryParams(radius = 2f, widthSegments = 64, heightSegments = 32),
            materialColor = 0xFF0000FF.toInt(),
            metalness = 0.8f,
            roughness = 0.2f,
            castShadow = false,
            receiveShadow = false
        )
        
        assertEquals(listOf(1f, 2f, 3f), mesh.position)
        assertEquals(listOf(0.5f, 1f, 1.5f), mesh.rotation)
        assertEquals(listOf(2f, 2f, 2f), mesh.scale)
        assertFalse(mesh.visible)
        assertEquals("MyMesh", mesh.name)
        assertEquals(GeometryType.SPHERE, mesh.geometryType)
        assertEquals(2f, mesh.geometryParams.radius)
        assertEquals(64, mesh.geometryParams.widthSegments)
        assertEquals(32, mesh.geometryParams.heightSegments)
        assertEquals(0xFF0000FF.toInt(), mesh.materialColor)
        assertEquals(0.8f, mesh.metalness)
        assertEquals(0.2f, mesh.roughness)
        assertFalse(mesh.castShadow)
        assertFalse(mesh.receiveShadow)
    }

    @Test
    fun meshData_serializationRoundTrip_preservesData() {
        val original = MeshData(
            id = "round-trip-mesh",
            position = listOf(10f, -5f, 3.5f),
            rotation = listOf(1.57f, 0f, 3.14f),
            scale = listOf(0.5f, 1.5f, 2.5f),
            visible = true,
            name = "RoundTripMesh",
            geometryType = GeometryType.CYLINDER,
            geometryParams = GeometryParams(radiusTop = 1f, radiusBottom = 2f, height = 3f, radialSegments = 24),
            materialColor = 0xAABBCCDD.toInt(),
            metalness = 0.5f,
            roughness = 0.5f,
            castShadow = true,
            receiveShadow = false
        )
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), original)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        
        assertEquals(original, restored)
    }

    @Test
    fun meshData_allGeometryTypes_serialize() {
        GeometryType.entries.forEach { geoType ->
            val mesh = MeshData(id = "geo-$geoType", geometryType = geoType)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
            assertEquals(geoType, restored.geometryType)
        }
    }

    @Test
    fun meshData_edgeCaseColors_serialize() {
        val colorCases = listOf(
            0x00000000,              // Fully transparent black
            0xFFFFFFFF.toInt(),      // Opaque white
            0xFF000000.toInt(),      // Opaque black
            0xFF0000FF.toInt(),      // Opaque blue
            Int.MAX_VALUE,           // Max positive
            Int.MIN_VALUE            // Min negative
        )
        
        colorCases.forEach { color ->
            val mesh = MeshData(id = "color-test", materialColor = color)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
            assertEquals(color, restored.materialColor)
        }
    }

    @Test
    fun meshData_extremePositionValues_serialize() {
        val extremePositions = listOf(
            listOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE),
            listOf(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE),
            listOf(-1000000f, 1000000f, 0f),
            listOf(0.000001f, 0.000001f, 0.000001f)
        )
        
        extremePositions.forEach { pos ->
            val mesh = MeshData(id = "extreme-pos", position = pos)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
            assertEquals(pos, restored.position)
        }
    }

    @Test
    fun meshData_metalnessRoughnessEdgeCases_serialize() {
        val edgeCases = listOf(
            0f to 0f,
            1f to 1f,
            0.5f to 0.5f,
            0f to 1f,
            1f to 0f,
            // Values outside 0-1 range should still serialize (validation is renderer's job)
            -0.5f to 1.5f,
            2f to -1f
        )
        
        edgeCases.forEach { (metalness, roughness) ->
            val mesh = MeshData(id = "mr-test", metalness = metalness, roughness = roughness)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
            assertEquals(metalness, restored.metalness)
            assertEquals(roughness, restored.roughness)
        }
    }

    // ===== GroupData Tests =====

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

    // ===== LightData Tests =====

    @Test
    fun lightData_allLightTypes_serialize() {
        LightType.entries.forEach { lightType ->
            val light = LightData(id = "light-$lightType", lightType = lightType)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), light)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as LightData
            assertEquals(lightType, restored.lightType)
        }
    }

    @Test
    fun lightData_defaultValues_areCorrect() {
        val light = LightData(id = "default-light")
        
        assertEquals(LightType.POINT, light.lightType)
        assertEquals(0xFFFFFFFF.toInt(), light.color)
        assertEquals(1f, light.intensity)
        assertEquals(0f, light.distance)
        assertEquals(2f, light.decay)
        assertEquals(0.523599f, light.angle) // PI/6
        assertEquals(0f, light.penumbra)
        assertFalse(light.castShadow)
        assertEquals(listOf(0f, 0f, 0f), light.target)
    }

    @Test
    fun lightData_spotLightParameters_serialize() {
        val spotLight = LightData(
            id = "spot-light",
            lightType = LightType.SPOT,
            angle = 0.785398f, // PI/4
            penumbra = 0.5f,
            distance = 100f,
            decay = 1f,
            target = listOf(0f, -1f, 0f),
            castShadow = true
        )
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), spotLight)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as LightData
        
        assertEquals(0.785398f, restored.angle)
        assertEquals(0.5f, restored.penumbra)
        assertEquals(100f, restored.distance)
        assertEquals(1f, restored.decay)
        assertEquals(listOf(0f, -1f, 0f), restored.target)
        assertTrue(restored.castShadow)
    }

    @Test
    fun lightData_edgeCaseIntensity_serializes() {
        val intensities = listOf(0f, 0.001f, 1f, 100f, 1000000f, -1f, Float.MAX_VALUE)
        
        intensities.forEach { intensity ->
            val light = LightData(id = "intensity-test", intensity = intensity)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), light)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as LightData
            assertEquals(intensity, restored.intensity)
        }
    }

    // ===== CameraData Tests =====

    @Test
    fun cameraData_allCameraTypes_serialize() {
        CameraType.entries.forEach { cameraType ->
            val camera = CameraData(id = "camera-$cameraType", cameraType = cameraType)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), camera)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as CameraData
            assertEquals(cameraType, restored.cameraType)
        }
    }

    @Test
    fun cameraData_defaultValues_areCorrect() {
        val camera = CameraData(id = "default-camera")
        
        assertEquals(CameraType.PERSPECTIVE, camera.cameraType)
        assertEquals(75f, camera.fov)
        assertEquals(1.777778f, camera.aspect)
        assertEquals(0.1f, camera.near)
        assertEquals(1000f, camera.far)
        assertEquals(listOf(-1f, 1f, 1f, -1f), camera.orthoBounds)
        assertNull(camera.lookAt)
        assertEquals(listOf(0f, 0f, 5f), camera.position)
    }

    @Test
    fun cameraData_perspectiveParameters_serialize() {
        val camera = CameraData(
            id = "perspective-camera",
            cameraType = CameraType.PERSPECTIVE,
            fov = 60f,
            aspect = 1.333333f, // 4:3
            near = 0.01f,
            far = 10000f,
            lookAt = listOf(0f, 0f, 0f)
        )
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), camera)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as CameraData
        
        assertEquals(60f, restored.fov)
        assertEquals(1.333333f, restored.aspect)
        assertEquals(0.01f, restored.near)
        assertEquals(10000f, restored.far)
        assertEquals(listOf(0f, 0f, 0f), restored.lookAt)
    }

    @Test
    fun cameraData_orthographicParameters_serialize() {
        val camera = CameraData(
            id = "ortho-camera",
            cameraType = CameraType.ORTHOGRAPHIC,
            orthoBounds = listOf(-10f, 10f, 10f, -10f)
        )
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), camera)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as CameraData
        
        assertEquals(CameraType.ORTHOGRAPHIC, restored.cameraType)
        assertEquals(listOf(-10f, 10f, 10f, -10f), restored.orthoBounds)
    }

    @Test
    fun cameraData_edgeCaseFov_serializes() {
        val fovValues = listOf(0.001f, 1f, 45f, 90f, 120f, 179f, 179.999f)
        
        fovValues.forEach { fov ->
            val camera = CameraData(id = "fov-test", fov = fov)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), camera)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as CameraData
            assertEquals(fov, restored.fov)
        }
    }

    // ===== GeometryParams Tests =====

    @Test
    fun geometryParams_defaultValues_areCorrect() {
        val params = GeometryParams()
        
        assertEquals(1f, params.width)
        assertEquals(1f, params.height)
        assertEquals(1f, params.depth)
        assertEquals(1f, params.radius)
        assertEquals(1, params.widthSegments)
        assertEquals(1, params.heightSegments)
        assertEquals(32, params.radialSegments)
        assertEquals(1f, params.radiusTop)
        assertEquals(1f, params.radiusBottom)
        assertFalse(params.openEnded)
        assertEquals(0.4f, params.tube)
        assertEquals(48, params.tubularSegments)
        assertEquals(0.5f, params.innerRadius)
        assertEquals(1f, params.outerRadius)
        assertEquals(0, params.detail)
    }

    @Test
    fun geometryParams_boxParameters_serialize() {
        val params = GeometryParams(width = 2f, height = 3f, depth = 4f)
        val mesh = MeshData(id = "box", geometryType = GeometryType.BOX, geometryParams = params)
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        
        assertEquals(2f, restored.geometryParams.width)
        assertEquals(3f, restored.geometryParams.height)
        assertEquals(4f, restored.geometryParams.depth)
    }

    @Test
    fun geometryParams_sphereParameters_serialize() {
        val params = GeometryParams(radius = 5f, widthSegments = 64, heightSegments = 32)
        val mesh = MeshData(id = "sphere", geometryType = GeometryType.SPHERE, geometryParams = params)
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        
        assertEquals(5f, restored.geometryParams.radius)
        assertEquals(64, restored.geometryParams.widthSegments)
        assertEquals(32, restored.geometryParams.heightSegments)
    }

    @Test
    fun geometryParams_torusParameters_serialize() {
        val params = GeometryParams(radius = 2f, tube = 0.5f, radialSegments = 16, tubularSegments = 64)
        val mesh = MeshData(id = "torus", geometryType = GeometryType.TORUS, geometryParams = params)
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        
        assertEquals(2f, restored.geometryParams.radius)
        assertEquals(0.5f, restored.geometryParams.tube)
        assertEquals(16, restored.geometryParams.radialSegments)
        assertEquals(64, restored.geometryParams.tubularSegments)
    }

    @Test
    fun geometryParams_ringParameters_serialize() {
        val params = GeometryParams(innerRadius = 0.25f, outerRadius = 2f)
        val mesh = MeshData(id = "ring", geometryType = GeometryType.RING, geometryParams = params)
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        
        assertEquals(0.25f, restored.geometryParams.innerRadius)
        assertEquals(2f, restored.geometryParams.outerRadius)
    }

    @Test
    fun geometryParams_cylinderParameters_serialize() {
        val params = GeometryParams(
            radiusTop = 1f, 
            radiusBottom = 2f, 
            height = 5f, 
            radialSegments = 24,
            openEnded = true
        )
        val mesh = MeshData(id = "cylinder", geometryType = GeometryType.CYLINDER, geometryParams = params)
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        
        assertEquals(1f, restored.geometryParams.radiusTop)
        assertEquals(2f, restored.geometryParams.radiusBottom)
        assertEquals(5f, restored.geometryParams.height)
        assertEquals(24, restored.geometryParams.radialSegments)
        assertTrue(restored.geometryParams.openEnded)
    }

    @Test
    fun geometryParams_zeroValues_serialize() {
        val params = GeometryParams(
            width = 0f,
            height = 0f,
            depth = 0f,
            radius = 0f
        )
        val mesh = MeshData(id = "zero-geo", geometryParams = params)
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        
        assertEquals(0f, restored.geometryParams.width)
        assertEquals(0f, restored.geometryParams.height)
        assertEquals(0f, restored.geometryParams.depth)
        assertEquals(0f, restored.geometryParams.radius)
    }

    @Test
    fun geometryParams_negativeValues_serialize() {
        val params = GeometryParams(
            width = -1f,
            height = -2f,
            depth = -3f,
            radius = -0.5f
        )
        val mesh = MeshData(id = "negative-geo", geometryParams = params)
        
        val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
        val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
        
        assertEquals(-1f, restored.geometryParams.width)
        assertEquals(-2f, restored.geometryParams.height)
        assertEquals(-3f, restored.geometryParams.depth)
        assertEquals(-0.5f, restored.geometryParams.radius)
    }

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

    // ===== Enum Serialization Tests =====

    @Test
    fun geometryType_allValues_serialize() {
        GeometryType.entries.forEach { type ->
            val mesh = MeshData(id = "geo-$type", geometryType = type)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
            assertEquals(type, restored.geometryType)
        }
    }

    @Test
    fun lightType_allValues_serialize() {
        LightType.entries.forEach { type ->
            val light = LightData(id = "light-$type", lightType = type)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), light)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as LightData
            assertEquals(type, restored.lightType)
        }
    }

    @Test
    fun cameraType_allValues_serialize() {
        CameraType.entries.forEach { type ->
            val camera = CameraData(id = "cam-$type", cameraType = type)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), camera)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as CameraData
            assertEquals(type, restored.cameraType)
        }
    }

    // ===== Data Class Equality Tests =====

    @Test
    fun meshData_equality_worksCorrectly() {
        val mesh1 = MeshData(id = "test", position = listOf(1f, 2f, 3f))
        val mesh2 = MeshData(id = "test", position = listOf(1f, 2f, 3f))
        val mesh3 = MeshData(id = "test", position = listOf(3f, 2f, 1f))
        
        assertEquals(mesh1, mesh2)
        assertNotEquals(mesh1, mesh3)
    }

    @Test
    fun meshData_hashCode_isConsistent() {
        val mesh1 = MeshData(id = "test", position = listOf(1f, 2f, 3f))
        val mesh2 = MeshData(id = "test", position = listOf(1f, 2f, 3f))
        
        assertEquals(mesh1.hashCode(), mesh2.hashCode())
    }

    @Test
    fun meshData_copy_worksCorrectly() {
        val original = MeshData(id = "original", geometryType = GeometryType.BOX)
        val copied = original.copy(id = "copied", geometryType = GeometryType.SPHERE)
        
        assertEquals("copied", copied.id)
        assertEquals(GeometryType.SPHERE, copied.geometryType)
        // Original unchanged
        assertEquals("original", original.id)
        assertEquals(GeometryType.BOX, original.geometryType)
    }
}
