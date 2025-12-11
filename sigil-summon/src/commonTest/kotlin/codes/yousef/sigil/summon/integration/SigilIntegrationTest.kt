package codes.yousef.sigil.summon.integration

import codes.yousef.sigil.schema.*
import codes.yousef.sigil.summon.context.SigilSummonContext
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Integration tests for the complete Sigil-Summon workflow:
 * Scene creation → Node registration → Scene building → Serialization → Deserialization
 *
 * These tests verify that the entire SSR pipeline works correctly.
 */
class SigilIntegrationTest {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    // ========== Full Pipeline Tests ==========

    @Test
    fun testCompleteScenePipelineWithSingleMesh() {
        // Simulate server-side: create context and register nodes
        val ctx = SigilSummonContext.createServerContext()
        ctx.registerNode(
            MeshData(
                id = "mesh-1",
                geometryType = GeometryType.BOX,
                position = listOf(0f, 1f, 2f),
                rotation = listOf(0f, 45f, 0f),
                scale = listOf(1f, 1f, 1f),
                materialColor = 0xFF0000FF.toInt(),
                geometryParams = GeometryParams(width = 2f, height = 2f, depth = 2f)
            )
        )
        val scene = ctx.buildScene()

        // Serialize to JSON (what would be embedded in HTML)
        val jsonString = json.encodeToString(SigilScene.serializer(), scene)

        // Deserialize (simulating client-side hydration reading the JSON)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        // Verify the scene survived the round-trip
        assertEquals(1, hydratedScene.rootNodes.size)
        val node = hydratedScene.rootNodes[0] as MeshData
        assertEquals(GeometryType.BOX, node.geometryType)
        assertEquals(listOf(0f, 1f, 2f), node.position)
        assertEquals(listOf(0f, 45f, 0f), node.rotation)
        assertEquals(0xFF0000FF.toInt(), node.materialColor)
    }

    @Test
    fun testCompleteScenePipelineWithMultipleLights() {
        val ctx = SigilSummonContext.createServerContext()
        ctx.registerNode(
            LightData(
                id = "light-ambient",
                lightType = LightType.AMBIENT,
                color = 0xFFFFFF,
                intensity = 0.5f
            )
        )
        ctx.registerNode(
            LightData(
                id = "light-directional",
                lightType = LightType.DIRECTIONAL,
                color = 0xFFFFCC,
                intensity = 1.0f,
                position = listOf(10f, 10f, 10f)
            )
        )
        ctx.registerNode(
            LightData(
                id = "light-point",
                lightType = LightType.POINT,
                color = 0xFF0000,
                intensity = 0.8f,
                position = listOf(0f, 5f, 0f),
                distance = 20f,
                decay = 2f
            )
        )
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        assertEquals(3, hydratedScene.rootNodes.size)
        assertTrue(hydratedScene.rootNodes.all { it is LightData })

        val lights = hydratedScene.rootNodes.map { it as LightData }
        assertEquals(LightType.AMBIENT, lights[0].lightType)
        assertEquals(LightType.DIRECTIONAL, lights[1].lightType)
        assertEquals(LightType.POINT, lights[2].lightType)
    }

    @Test
    fun testCompleteScenePipelineWithNestedGroups() {
        val ctx = SigilSummonContext.createServerContext()
        // Create a group with children
        val children = listOf(
            MeshData(
                id = "sphere-child",
                geometryType = GeometryType.SPHERE,
                position = listOf(0f, 0f, 0f)
            ),
            MeshData(
                id = "box-child",
                geometryType = GeometryType.BOX,
                position = listOf(2f, 0f, 0f)
            )
        )

        ctx.registerNode(
            GroupData(
                id = "parent-group",
                position = listOf(5f, 0f, 0f),
                children = children
            )
        )
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        assertEquals(1, hydratedScene.rootNodes.size)
        val group = hydratedScene.rootNodes[0] as GroupData
        assertEquals(2, group.children.size)
        assertEquals(listOf(5f, 0f, 0f), group.position)

        val sphere = group.children[0] as MeshData
        val box = group.children[1] as MeshData
        assertEquals(GeometryType.SPHERE, sphere.geometryType)
        assertEquals(GeometryType.BOX, box.geometryType)
    }

    @Test
    fun testCompleteScenePipelineWithSceneSettings() {
        val ctx = SigilSummonContext.createServerContext()
        ctx.configureSettings {
            copy(
                backgroundColor = 0x1a1a2e,
                shadowsEnabled = true
            )
        }
        ctx.registerNode(
            MeshData(id = "plane-mesh", geometryType = GeometryType.PLANE)
        )
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        assertEquals(0x1a1a2e, hydratedScene.settings.backgroundColor)
        assertEquals(true, hydratedScene.settings.shadowsEnabled)
    }

    @Test
    fun testCompleteScenePipelineWithCamera() {
        val ctx = SigilSummonContext.createServerContext()
        ctx.registerNode(
            CameraData(
                id = "main-camera",
                cameraType = CameraType.PERSPECTIVE,
                position = listOf(0f, 5f, 10f),
                lookAt = listOf(0f, 0f, 0f),
                fov = 75f,
                near = 0.1f,
                far = 1000f
            )
        )
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        assertEquals(1, hydratedScene.rootNodes.size)
        val camera = hydratedScene.rootNodes[0] as CameraData
        assertEquals(CameraType.PERSPECTIVE, camera.cameraType)
        assertEquals(listOf(0f, 5f, 10f), camera.position)
        assertEquals(listOf(0f, 0f, 0f), camera.lookAt)
        assertEquals(75f, camera.fov)
    }

    // ========== Complex Scene Tests ==========

    @Test
    fun testCompleteScenePipelineWithComplexScene() {
        val ctx = SigilSummonContext.createServerContext()
        
        // Scene settings
        ctx.configureSettings {
            copy(
                backgroundColor = 0x0a0a0f,
                shadowsEnabled = true
            )
        }

        // Camera
        ctx.registerNode(
            CameraData(
                id = "camera-1",
                cameraType = CameraType.PERSPECTIVE,
                position = listOf(0f, 10f, 20f),
                lookAt = listOf(0f, 0f, 0f),
                fov = 60f
            )
        )

        // Lights
        ctx.registerNode(
            LightData(
                id = "ambient-light",
                lightType = LightType.AMBIENT,
                color = 0x404040,
                intensity = 0.4f
            )
        )
        ctx.registerNode(
            LightData(
                id = "directional-light",
                lightType = LightType.DIRECTIONAL,
                color = 0xffffff,
                intensity = 0.8f,
                position = listOf(5f, 10f, 7f)
            )
        )

        // Ground plane
        ctx.registerNode(
            MeshData(
                id = "ground-plane",
                geometryType = GeometryType.PLANE,
                position = listOf(0f, 0f, 0f),
                rotation = listOf(-90f, 0f, 0f),
                scale = listOf(50f, 50f, 1f),
                materialColor = 0x2d2d2d
            )
        )

        // Objects group
        val objects = listOf(
            MeshData(
                id = "red-box",
                geometryType = GeometryType.BOX,
                position = listOf(-3f, 1f, 0f),
                materialColor = 0xff6b6b
            ),
            MeshData(
                id = "teal-sphere",
                geometryType = GeometryType.SPHERE,
                position = listOf(0f, 1f, 0f),
                materialColor = 0x4ecdc4
            ),
            MeshData(
                id = "yellow-cylinder",
                geometryType = GeometryType.CYLINDER,
                position = listOf(3f, 1f, 0f),
                materialColor = 0xffe66d,
                geometryParams = GeometryParams(
                    radiusTop = 0.5f,
                    radiusBottom = 0.5f,
                    height = 2f
                )
            )
        )

        ctx.registerNode(
            GroupData(
                id = "objects-group",
                position = listOf(0f, 0f, 0f),
                children = objects
            )
        )

        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        // Verify structure
        assertEquals(5, hydratedScene.rootNodes.size) // camera, 2 lights, plane, group

        // Verify settings
        assertEquals(0x0a0a0f, hydratedScene.settings.backgroundColor)
        assertTrue(hydratedScene.settings.shadowsEnabled)

        // Count node types
        val cameras = hydratedScene.rootNodes.filterIsInstance<CameraData>()
        val lights = hydratedScene.rootNodes.filterIsInstance<LightData>()
        val meshes = hydratedScene.rootNodes.filterIsInstance<MeshData>()
        val groups = hydratedScene.rootNodes.filterIsInstance<GroupData>()

        assertEquals(1, cameras.size)
        assertEquals(2, lights.size)
        assertEquals(1, meshes.size) // ground plane
        assertEquals(1, groups.size)

        // Verify group children
        assertEquals(3, groups[0].children.size)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun testEmptySceneSerialization() {
        val ctx = SigilSummonContext.createServerContext()
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        assertTrue(hydratedScene.rootNodes.isEmpty())
    }

    @Test
    fun testContextClearResetsState() {
        val ctx = SigilSummonContext.createServerContext()
        ctx.registerNode(MeshData(id = "mesh-1", geometryType = GeometryType.BOX))
        ctx.registerNode(MeshData(id = "mesh-2", geometryType = GeometryType.SPHERE))

        assertEquals(2, ctx.nodes.size)

        ctx.clear()

        assertEquals(0, ctx.nodes.size)
    }

    @Test
    fun testNestedGroupsPreserveHierarchy() {
        val ctx = SigilSummonContext.createServerContext()
        
        // Outer group
        val innerChildren = listOf(
            MeshData(id = "inner-box", geometryType = GeometryType.BOX, position = listOf(0f, 0f, 0f))
        )

        val innerGroup = GroupData(
            id = "inner-group",
            position = listOf(1f, 1f, 1f),
            children = innerChildren
        )

        val outerChildren = listOf(
            innerGroup,
            MeshData(id = "outer-sphere", geometryType = GeometryType.SPHERE, position = listOf(2f, 2f, 2f))
        )

        ctx.registerNode(
            GroupData(
                id = "outer-group",
                position = listOf(0f, 0f, 0f),
                children = outerChildren
            )
        )
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        assertEquals(1, hydratedScene.rootNodes.size)
        val outerGroup = hydratedScene.rootNodes[0] as GroupData
        assertEquals(2, outerGroup.children.size)

        val nestedInnerGroup = outerGroup.children[0] as GroupData
        assertEquals(1, nestedInnerGroup.children.size)
        assertEquals(listOf(1f, 1f, 1f), nestedInnerGroup.position)

        val innerBox = nestedInnerGroup.children[0] as MeshData
        assertEquals(GeometryType.BOX, innerBox.geometryType)
    }

    @Test
    fun testAllLightTypesRoundTrip() {
        val lightTypes = listOf(LightType.AMBIENT, LightType.DIRECTIONAL, LightType.POINT, LightType.SPOT, LightType.HEMISPHERE)

        val ctx = SigilSummonContext.createServerContext()
        lightTypes.forEachIndexed { index, type ->
            ctx.registerNode(
                LightData(
                    id = "light-$index",
                    lightType = type,
                    color = 0xffffff,
                    intensity = 1.0f,
                    position = if (type != LightType.AMBIENT) listOf(0f, 5f, 0f) else listOf(0f, 0f, 0f)
                )
            )
        }
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        assertEquals(lightTypes.size, hydratedScene.rootNodes.size)
        val hydratedTypes = hydratedScene.rootNodes
            .filterIsInstance<LightData>()
            .map { it.lightType }

        assertEquals(lightTypes, hydratedTypes)
    }

    @Test
    fun testGeometryParamsPreservation() {
        val ctx = SigilSummonContext.createServerContext()
        ctx.registerNode(
            MeshData(
                id = "torus-mesh",
                geometryType = GeometryType.TORUS,
                geometryParams = GeometryParams(
                    radius = 5f,
                    tube = 1.5f,
                    radialSegments = 16,
                    tubularSegments = 48
                )
            )
        )
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        val mesh = hydratedScene.rootNodes[0] as MeshData
        assertEquals(5f, mesh.geometryParams.radius)
        assertEquals(1.5f, mesh.geometryParams.tube)
        assertEquals(16, mesh.geometryParams.radialSegments)
        assertEquals(48, mesh.geometryParams.tubularSegments)
    }

    @Test
    fun testCameraTypesRoundTrip() {
        val ctx = SigilSummonContext.createServerContext()
        ctx.registerNode(
            CameraData(
                id = "perspective-camera",
                cameraType = CameraType.PERSPECTIVE,
                position = listOf(0f, 5f, 10f),
                fov = 75f
            )
        )
        ctx.registerNode(
            CameraData(
                id = "orthographic-camera",
                cameraType = CameraType.ORTHOGRAPHIC,
                position = listOf(0f, 10f, 0f),
                orthoBounds = listOf(-10f, 10f, 10f, -10f) // left, right, top, bottom
            )
        )
        val scene = ctx.buildScene()

        val jsonString = json.encodeToString(SigilScene.serializer(), scene)
        val hydratedScene = json.decodeFromString(SigilScene.serializer(), jsonString)

        assertEquals(2, hydratedScene.rootNodes.size)

        val perspective = hydratedScene.rootNodes[0] as CameraData
        val orthographic = hydratedScene.rootNodes[1] as CameraData

        assertEquals(CameraType.PERSPECTIVE, perspective.cameraType)
        assertEquals(75f, perspective.fov)

        assertEquals(CameraType.ORTHOGRAPHIC, orthographic.cameraType)
        assertEquals(-10f, orthographic.orthoBounds[0]) // left
        assertEquals(10f, orthographic.orthoBounds[1])  // right
    }
}
