package io.github.codeyousef.sigil.schema

/**
 * Generate a unique node ID for scene graph nodes.
 * Platform-specific implementations use appropriate random ID generation.
 */
expect fun generateNodeId(): String

/**
 * Builder DSL for creating Sigil scenes programmatically.
 */
class SigilSceneBuilder {
    private val nodes = mutableListOf<SigilNodeData>()
    private var settings = SceneSettings()

    /**
     * Add a mesh node to the scene
     */
    fun mesh(
        id: String = generateNodeId(),
        geometryType: GeometryType = GeometryType.BOX,
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        color: Int = 0xFFFFFFFF.toInt(),
        metalness: Float = 0f,
        roughness: Float = 1f,
        geometryParams: GeometryParams = GeometryParams(),
        visible: Boolean = true,
        name: String? = null,
        castShadow: Boolean = true,
        receiveShadow: Boolean = true
    ): MeshData {
        val mesh = MeshData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            geometryType = geometryType,
            geometryParams = geometryParams,
            materialColor = color,
            metalness = metalness,
            roughness = roughness,
            castShadow = castShadow,
            receiveShadow = receiveShadow
        )
        nodes.add(mesh)
        return mesh
    }

    /**
     * Add a light node to the scene
     */
    fun light(
        id: String = generateNodeId(),
        lightType: LightType = LightType.POINT,
        position: List<Float> = listOf(0f, 5f, 0f),
        color: Int = 0xFFFFFFFF.toInt(),
        intensity: Float = 1f,
        distance: Float = 0f,
        decay: Float = 2f,
        angle: Float = 0.523599f,
        penumbra: Float = 0f,
        castShadow: Boolean = false,
        target: List<Float> = listOf(0f, 0f, 0f),
        visible: Boolean = true,
        name: String? = null
    ): LightData {
        val light = LightData(
            id = id,
            position = position,
            rotation = listOf(0f, 0f, 0f),
            scale = listOf(1f, 1f, 1f),
            visible = visible,
            name = name,
            lightType = lightType,
            color = color,
            intensity = intensity,
            distance = distance,
            decay = decay,
            angle = angle,
            penumbra = penumbra,
            castShadow = castShadow,
            target = target
        )
        nodes.add(light)
        return light
    }

    /**
     * Add a camera node to the scene
     */
    fun camera(
        id: String = generateNodeId(),
        cameraType: CameraType = CameraType.PERSPECTIVE,
        position: List<Float> = listOf(0f, 0f, 5f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        fov: Float = 75f,
        aspect: Float = 1.777778f,
        near: Float = 0.1f,
        far: Float = 1000f,
        lookAt: List<Float>? = null,
        visible: Boolean = true,
        name: String? = null
    ): CameraData {
        val camera = CameraData(
            id = id,
            position = position,
            rotation = rotation,
            scale = listOf(1f, 1f, 1f),
            visible = visible,
            name = name,
            cameraType = cameraType,
            fov = fov,
            aspect = aspect,
            near = near,
            far = far,
            lookAt = lookAt
        )
        nodes.add(camera)
        return camera
    }

    /**
     * Add a group of nodes
     */
    fun group(
        id: String = generateNodeId(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        content: SigilGroupBuilder.() -> Unit
    ): GroupData {
        val builder = SigilGroupBuilder()
        builder.content()
        val group = GroupData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            children = builder.build()
        )
        nodes.add(group)
        return group
    }

    /**
     * Configure scene settings
     */
    fun settings(
        backgroundColor: Int = 0xFF1A1A2E.toInt(),
        fogEnabled: Boolean = false,
        fogColor: Int = 0xFFFFFFFF.toInt(),
        fogNear: Float = 10f,
        fogFar: Float = 100f,
        shadowsEnabled: Boolean = true,
        toneMapping: ToneMappingMode = ToneMappingMode.ACES_FILMIC,
        exposure: Float = 1f
    ) {
        settings = SceneSettings(
            backgroundColor = backgroundColor,
            fogEnabled = fogEnabled,
            fogColor = fogColor,
            fogNear = fogNear,
            fogFar = fogFar,
            shadowsEnabled = shadowsEnabled,
            toneMapping = toneMapping,
            exposure = exposure
        )
    }

    /**
     * Build the final scene
     */
    fun build(): SigilScene = SigilScene(
        rootNodes = nodes.toList(),
        settings = settings
    )
}

/**
 * Builder for group children
 */
class SigilGroupBuilder {
    private val children = mutableListOf<SigilNodeData>()

    fun mesh(
        id: String = generateNodeId(),
        geometryType: GeometryType = GeometryType.BOX,
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        color: Int = 0xFFFFFFFF.toInt(),
        metalness: Float = 0f,
        roughness: Float = 1f,
        geometryParams: GeometryParams = GeometryParams(),
        visible: Boolean = true,
        name: String? = null,
        castShadow: Boolean = true,
        receiveShadow: Boolean = true
    ): MeshData {
        val mesh = MeshData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            geometryType = geometryType,
            geometryParams = geometryParams,
            materialColor = color,
            metalness = metalness,
            roughness = roughness,
            castShadow = castShadow,
            receiveShadow = receiveShadow
        )
        children.add(mesh)
        return mesh
    }

    fun light(
        id: String = generateNodeId(),
        lightType: LightType = LightType.POINT,
        position: List<Float> = listOf(0f, 5f, 0f),
        color: Int = 0xFFFFFFFF.toInt(),
        intensity: Float = 1f,
        distance: Float = 0f,
        decay: Float = 2f,
        visible: Boolean = true,
        name: String? = null
    ): LightData {
        val light = LightData(
            id = id,
            position = position,
            rotation = listOf(0f, 0f, 0f),
            scale = listOf(1f, 1f, 1f),
            visible = visible,
            name = name,
            lightType = lightType,
            color = color,
            intensity = intensity,
            distance = distance,
            decay = decay
        )
        children.add(light)
        return light
    }

    fun group(
        id: String = generateNodeId(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        content: SigilGroupBuilder.() -> Unit
    ): GroupData {
        val builder = SigilGroupBuilder()
        builder.content()
        val group = GroupData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            children = builder.build()
        )
        children.add(group)
        return group
    }

    internal fun build(): List<SigilNodeData> = children.toList()
}

/**
 * DSL entry point for building Sigil scenes
 */
fun sigilScene(builder: SigilSceneBuilder.() -> Unit): SigilScene {
    val sceneBuilder = SigilSceneBuilder()
    sceneBuilder.builder()
    return sceneBuilder.build()
}
