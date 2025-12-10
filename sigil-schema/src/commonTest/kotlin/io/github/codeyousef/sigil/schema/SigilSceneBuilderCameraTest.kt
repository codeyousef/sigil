package io.github.codeyousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SigilSceneBuilderCameraTest {

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
}
