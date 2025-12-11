package codes.yousef.sigil.schema

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SigilSceneBuilderMeshTest {

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
}
