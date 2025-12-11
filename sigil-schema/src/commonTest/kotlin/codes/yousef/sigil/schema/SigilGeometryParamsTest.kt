package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilGeometryParamsTest {

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

    @Test
    fun geometryType_allValues_serialize() {
        GeometryType.entries.forEach { type ->
            val mesh = MeshData(id = "geo-$type", geometryType = type)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), mesh)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as MeshData
            assertEquals(type, restored.geometryType)
        }
    }
}
