package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SigilMeshDataTest {

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
