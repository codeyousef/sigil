package io.github.codeyousef.sigil.summon.context

import io.github.codeyousef.sigil.schema.CameraData
import io.github.codeyousef.sigil.schema.GeometryType
import io.github.codeyousef.sigil.schema.GroupData
import io.github.codeyousef.sigil.schema.LightData
import io.github.codeyousef.sigil.schema.LightType
import io.github.codeyousef.sigil.schema.MeshData
import io.github.codeyousef.sigil.schema.SceneSettings
import io.github.codeyousef.sigil.schema.ToneMappingMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/**
 * Comprehensive E2E tests for SigilSummonContext.
 * Tests context creation, node registration, grouping, and scene building.
 */
class SigilSummonContextTest {

    // ===== Context Creation Tests =====

    @Test
    fun createServerContext_createsServerContext() {
        val context = SigilSummonContext.createServerContext()
        
        assertTrue(context.isServer)
    }

    @Test
    fun createClientContext_createsClientContext() {
        val context = SigilSummonContext.createClientContext()
        
        assertFalse(context.isServer)
    }

    @Test
    fun newContext_hasEmptyNodes() {
        val context = SigilSummonContext.createServerContext()
        
        assertTrue(context.nodes.isEmpty())
    }

    @Test
    fun newContext_hasDefaultSettings() {
        val context = SigilSummonContext.createServerContext()
        
        assertEquals(SceneSettings(), context.settings)
    }

    // ===== Node Registration Tests =====

    @Test
    fun registerNode_addsMeshToNodes() {
        val context = SigilSummonContext.createServerContext()
        val mesh = MeshData(id = "test-mesh")
        
        context.registerNode(mesh)
        
        assertEquals(1, context.nodes.size)
        assertEquals("test-mesh", context.nodes[0].id)
    }

    @Test
    fun registerNode_addsMultipleNodes() {
        val context = SigilSummonContext.createServerContext()
        
        context.registerNode(MeshData(id = "mesh-1"))
        context.registerNode(MeshData(id = "mesh-2"))
        context.registerNode(LightData(id = "light-1"))
        context.registerNode(CameraData(id = "camera-1"))
        
        assertEquals(4, context.nodes.size)
    }

    @Test
    fun registerNode_preservesOrder() {
        val context = SigilSummonContext.createServerContext()
        
        context.registerNode(MeshData(id = "first"))
        context.registerNode(MeshData(id = "second"))
        context.registerNode(MeshData(id = "third"))
        
        assertEquals("first", context.nodes[0].id)
        assertEquals("second", context.nodes[1].id)
        assertEquals("third", context.nodes[2].id)
    }

    @Test
    fun registerNode_allNodeTypes_work() {
        val context = SigilSummonContext.createServerContext()
        
        context.registerNode(MeshData(id = "mesh"))
        context.registerNode(GroupData(id = "group", children = emptyList()))
        context.registerNode(LightData(id = "light"))
        context.registerNode(CameraData(id = "camera"))
        
        assertEquals(4, context.nodes.size)
        assertTrue(context.nodes[0] is MeshData)
        assertTrue(context.nodes[1] is GroupData)
        assertTrue(context.nodes[2] is LightData)
        assertTrue(context.nodes[3] is CameraData)
    }

    // ===== Group Context Tests =====

    @Test
    fun enterGroup_exitGroup_createsNestedStructure() {
        val context = SigilSummonContext.createServerContext()
        
        // Register a group with children
        val groupChildren = mutableListOf<io.github.codeyousef.sigil.schema.SigilNodeData>()
        context.enterGroup(groupChildren)
        context.registerNode(MeshData(id = "child-mesh"))
        context.exitGroup()
        
        // Register the group itself
        context.registerNode(GroupData(id = "parent-group", children = groupChildren))
        
        assertEquals(1, context.nodes.size)
        val group = context.nodes[0] as GroupData
        assertEquals(1, group.children.size)
        assertEquals("child-mesh", group.children[0].id)
    }

    @Test
    fun nestedGroups_createDeepHierarchy() {
        val context = SigilSummonContext.createServerContext()
        
        // Build nested groups manually
        val innerChildren = mutableListOf<io.github.codeyousef.sigil.schema.SigilNodeData>()
        context.enterGroup(innerChildren)
        context.registerNode(MeshData(id = "deep-mesh"))
        context.exitGroup()
        
        val outerChildren = mutableListOf<io.github.codeyousef.sigil.schema.SigilNodeData>()
        context.enterGroup(outerChildren)
        context.registerNode(GroupData(id = "inner-group", children = innerChildren))
        context.exitGroup()
        
        context.registerNode(GroupData(id = "outer-group", children = outerChildren))
        
        assertEquals(1, context.nodes.size)
        val outer = context.nodes[0] as GroupData
        assertEquals("outer-group", outer.id)
        
        val inner = outer.children[0] as GroupData
        assertEquals("inner-group", inner.id)
        
        val deepMesh = inner.children[0] as MeshData
        assertEquals("deep-mesh", deepMesh.id)
    }

    @Test
    fun exitGroup_withEmptyStack_doesNotThrow() {
        val context = SigilSummonContext.createServerContext()
        
        // Should not throw, just do nothing
        context.exitGroup()
        context.exitGroup()
        context.exitGroup()
        
        // Context should still be usable
        context.registerNode(MeshData(id = "mesh"))
        assertEquals(1, context.nodes.size)
    }

    // ===== Settings Configuration Tests =====

    @Test
    fun configureSettings_updatesSettings() {
        val context = SigilSummonContext.createServerContext()
        
        context.configureSettings {
            copy(backgroundColor = 0xFF000000.toInt())
        }
        
        assertEquals(0xFF000000.toInt(), context.settings.backgroundColor)
    }

    @Test
    fun configureSettings_multipleProperties() {
        val context = SigilSummonContext.createServerContext()
        
        context.configureSettings {
            copy(
                backgroundColor = 0xFF333333.toInt(),
                fogEnabled = true,
                fogNear = 20f,
                fogFar = 200f,
                shadowsEnabled = false,
                toneMapping = ToneMappingMode.REINHARD,
                exposure = 1.5f
            )
        }
        
        assertEquals(0xFF333333.toInt(), context.settings.backgroundColor)
        assertTrue(context.settings.fogEnabled)
        assertEquals(20f, context.settings.fogNear)
        assertEquals(200f, context.settings.fogFar)
        assertFalse(context.settings.shadowsEnabled)
        assertEquals(ToneMappingMode.REINHARD, context.settings.toneMapping)
        assertEquals(1.5f, context.settings.exposure)
    }

    @Test
    fun configureSettings_calledMultipleTimes_lastWins() {
        val context = SigilSummonContext.createServerContext()
        
        context.configureSettings { copy(backgroundColor = 0xFF111111.toInt()) }
        context.configureSettings { copy(backgroundColor = 0xFF222222.toInt()) }
        context.configureSettings { copy(backgroundColor = 0xFF333333.toInt()) }
        
        assertEquals(0xFF333333.toInt(), context.settings.backgroundColor)
    }

    // ===== buildScene Tests =====

    @Test
    fun buildScene_emptyContext_createsEmptyScene() {
        val context = SigilSummonContext.createServerContext()
        
        val scene = context.buildScene()
        
        assertTrue(scene.rootNodes.isEmpty())
        assertEquals(SceneSettings(), scene.settings)
    }

    @Test
    fun buildScene_withNodes_createsCorrectScene() {
        val context = SigilSummonContext.createServerContext()
        
        context.registerNode(MeshData(id = "mesh-1"))
        context.registerNode(LightData(id = "light-1"))
        
        val scene = context.buildScene()
        
        assertEquals(2, scene.rootNodes.size)
        assertEquals("mesh-1", scene.rootNodes[0].id)
        assertEquals("light-1", scene.rootNodes[1].id)
    }

    @Test
    fun buildScene_withSettings_includesSettings() {
        val context = SigilSummonContext.createServerContext()
        
        context.configureSettings {
            copy(fogEnabled = true, shadowsEnabled = false)
        }
        
        val scene = context.buildScene()
        
        assertTrue(scene.settings.fogEnabled)
        assertFalse(scene.settings.shadowsEnabled)
    }

    @Test
    fun buildScene_sceneIsSerializable() {
        val context = SigilSummonContext.createServerContext()
        
        context.registerNode(MeshData(id = "mesh", geometryType = GeometryType.SPHERE))
        context.configureSettings { copy(backgroundColor = 0xFF000000.toInt()) }
        
        val scene = context.buildScene()
        val json = scene.toJson()
        val restored = io.github.codeyousef.sigil.schema.SigilScene.fromJson(json)
        
        assertEquals(scene, restored)
    }

    // ===== clear Tests =====

    @Test
    fun clear_removesAllNodes() {
        val context = SigilSummonContext.createServerContext()
        
        context.registerNode(MeshData(id = "mesh-1"))
        context.registerNode(MeshData(id = "mesh-2"))
        
        context.clear()
        
        assertTrue(context.nodes.isEmpty())
    }

    @Test
    fun clear_resetsSettings() {
        val context = SigilSummonContext.createServerContext()
        
        context.configureSettings { copy(fogEnabled = true) }
        
        context.clear()
        
        assertEquals(SceneSettings(), context.settings)
    }

    @Test
    fun clear_allowsReuse() {
        val context = SigilSummonContext.createServerContext()
        
        context.registerNode(MeshData(id = "old-mesh"))
        context.clear()
        context.registerNode(MeshData(id = "new-mesh"))
        
        assertEquals(1, context.nodes.size)
        assertEquals("new-mesh", context.nodes[0].id)
    }

    // ===== withContext Tests =====

    @Test
    fun withContext_setsCurrentContext() {
        val context = SigilSummonContext.createServerContext()
        
        SigilSummonContext.withContext(context) {
            val current = SigilSummonContext.current()
            assertEquals(context, current)
        }
    }

    @Test
    fun withContext_removesContextAfterBlock() {
        val context = SigilSummonContext.createServerContext()
        
        SigilSummonContext.withContext(context) {
            assertNotNull(SigilSummonContext.currentOrNull())
        }
        
        assertNull(SigilSummonContext.currentOrNull())
    }

    @Test
    fun withContext_returnsBlockResult() {
        val context = SigilSummonContext.createServerContext()
        
        val result = SigilSummonContext.withContext(context) {
            context.registerNode(MeshData(id = "mesh"))
            "result-value"
        }
        
        assertEquals("result-value", result)
    }

    @Test
    fun withContext_nestedContexts_restoresPrevious() {
        val outer = SigilSummonContext.createServerContext()
        val inner = SigilSummonContext.createClientContext()
        
        SigilSummonContext.withContext(outer) {
            assertTrue(SigilSummonContext.current().isServer)
            
            SigilSummonContext.withContext(inner) {
                assertFalse(SigilSummonContext.current().isServer)
            }
            
            // Should be back to outer
            assertTrue(SigilSummonContext.current().isServer)
        }
        
        assertNull(SigilSummonContext.currentOrNull())
    }

    @Test
    fun withContext_exceptionInBlock_stillRemovesContext() {
        val context = SigilSummonContext.createServerContext()
        
        try {
            SigilSummonContext.withContext(context) {
                throw RuntimeException("Test exception")
            }
        } catch (_: RuntimeException) {
            // Expected
        }
        
        assertNull(SigilSummonContext.currentOrNull())
    }

    // ===== current / currentOrNull Tests =====

    @Test
    fun current_withoutContext_throws() {
        // Ensure no context is active
        assertNull(SigilSummonContext.currentOrNull())
        
        assertFailsWith<IllegalStateException> {
            SigilSummonContext.current()
        }
    }

    @Test
    fun currentOrNull_withoutContext_returnsNull() {
        assertNull(SigilSummonContext.currentOrNull())
    }

    @Test
    fun currentOrNull_withContext_returnsContext() {
        val context = SigilSummonContext.createServerContext()
        
        SigilSummonContext.withContext(context) {
            val current = SigilSummonContext.currentOrNull()
            assertNotNull(current)
            assertEquals(context, current)
        }
    }

    // ===== Complex Scenario Tests =====

    @Test
    fun complexScene_buildsCorrectly() {
        val context = SigilSummonContext.createServerContext()
        
        // Configure settings
        context.configureSettings {
            copy(
                backgroundColor = 0xFF87CEEB.toInt(),
                shadowsEnabled = true
            )
        }
        
        // Add a ground plane
        context.registerNode(
            MeshData(
                id = "ground",
                geometryType = GeometryType.PLANE,
                position = listOf(0f, 0f, 0f)
            )
        )
        
        // Add a group of objects
        val objectsChildren = mutableListOf<io.github.codeyousef.sigil.schema.SigilNodeData>()
        context.enterGroup(objectsChildren)
        context.registerNode(MeshData(id = "cube", geometryType = GeometryType.BOX, position = listOf(-2f, 0.5f, 0f)))
        context.registerNode(MeshData(id = "sphere", geometryType = GeometryType.SPHERE, position = listOf(0f, 0.5f, 0f)))
        context.registerNode(MeshData(id = "cylinder", geometryType = GeometryType.CYLINDER, position = listOf(2f, 0.5f, 0f)))
        context.exitGroup()
        context.registerNode(GroupData(id = "objects", children = objectsChildren))
        
        // Add lights
        context.registerNode(LightData(id = "ambient", lightType = LightType.AMBIENT, intensity = 0.3f))
        context.registerNode(LightData(id = "sun", lightType = LightType.DIRECTIONAL, position = listOf(10f, 20f, 10f)))
        
        // Add camera
        context.registerNode(CameraData(id = "main-camera", position = listOf(0f, 5f, 10f)))
        
        val scene = context.buildScene()
        
        assertEquals(5, scene.rootNodes.size) // ground, objects, ambient, sun, camera
        assertEquals(0xFF87CEEB.toInt(), scene.settings.backgroundColor)
        
        // Verify the objects group
        val objectsGroup = scene.findNodeById("objects") as GroupData
        assertEquals(3, objectsGroup.children.size)
        
        // Verify round-trip serialization
        val json = scene.toJson()
        val restored = io.github.codeyousef.sigil.schema.SigilScene.fromJson(json)
        assertEquals(scene, restored)
    }

    @Test
    fun nodes_isImmutableSnapshot() {
        val context = SigilSummonContext.createServerContext()
        
        context.registerNode(MeshData(id = "mesh-1"))
        val snapshot1 = context.nodes
        
        context.registerNode(MeshData(id = "mesh-2"))
        val snapshot2 = context.nodes
        
        // First snapshot should not be affected by later changes
        assertEquals(1, snapshot1.size)
        assertEquals(2, snapshot2.size)
    }
}
