package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SigilCameraDataTest {

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

    @Test
    fun cameraType_allValues_serialize() {
        CameraType.entries.forEach { type ->
            val camera = CameraData(id = "cam-$type", cameraType = type)
            val json = SigilJson.encodeToString(SigilNodeData.serializer(), camera)
            val restored = SigilJson.decodeFromString(SigilNodeData.serializer(), json) as CameraData
            assertEquals(type, restored.cameraType)
        }
    }
}
