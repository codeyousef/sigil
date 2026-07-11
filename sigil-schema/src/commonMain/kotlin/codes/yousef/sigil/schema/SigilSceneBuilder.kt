package codes.yousef.sigil.schema

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
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
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
            interaction = interaction,
            animations = animations,
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
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList()
    ): LightData {
        val light = LightData(
            id = id,
            position = position,
            rotation = listOf(0f, 0f, 0f),
            scale = listOf(1f, 1f, 1f),
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
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
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList()
    ): CameraData {
        val camera = CameraData(
            id = id,
            position = position,
            rotation = rotation,
            scale = listOf(1f, 1f, 1f),
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
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
     * Add a glTF model node to the scene
     */
    fun model(
        id: String = generateNodeId(),
        url: String,
        preloadUrls: List<String> = emptyList(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
        castShadow: Boolean = true,
        receiveShadow: Boolean = true,
        materialOverrides: List<ModelMaterialOverride> = emptyList()
    ): ModelData {
        val model = ModelData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            url = url,
            preloadUrls = preloadUrls,
            castShadow = castShadow,
            receiveShadow = receiveShadow,
            materialOverrides = materialOverrides
        )
        nodes.add(model)
        return model
    }

    /**
     * Add a mesh text node to the scene.
     */
    fun text(
        id: String = generateNodeId(),
        text: String,
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
        color: Int = 0xFFFFFFFF.toInt(),
        size: Float = 1f,
        depth: Float = 0.02f,
        curveSegments: Int = 12,
        letterSpacing: Float = 0f,
        lineHeight: Float = 1.2f,
        align: TextAlignMode = TextAlignMode.LEFT,
        baseline: TextBaselineMode = TextBaselineMode.ALPHABETIC,
        maxWidth: Float? = null,
        wordWrap: Boolean = false,
        facingMode: TextFacingMode = TextFacingMode.FIXED,
        fontUrl: String? = null,
        castShadow: Boolean = false,
        receiveShadow: Boolean = false
    ): TextData {
        val textNode = TextData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            text = text,
            color = color,
            size = size,
            depth = depth,
            curveSegments = curveSegments,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            align = align,
            baseline = baseline,
            maxWidth = maxWidth,
            wordWrap = wordWrap,
            facingMode = facingMode,
            fontUrl = fontUrl,
            castShadow = castShadow,
            receiveShadow = receiveShadow
        )
        nodes.add(textNode)
        return textNode
    }

    fun frameStatsText(
        id: String = generateNodeId(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
        prefix: String = "FPS ",
        decimalPlaces: Int = 0,
        updateIntervalMs: Int = 250,
        color: Int = 0xFFFFFFFF.toInt(),
        size: Float = 18f,
        depth: Float = 0.01f,
        align: TextAlignMode = TextAlignMode.LEFT,
        baseline: TextBaselineMode = TextBaselineMode.TOP,
        fontUrl: String? = null
    ): FrameStatsTextData = FrameStatsTextData(
        id = id,
        position = position,
        rotation = rotation,
        scale = scale,
        visible = visible,
        name = name,
        interaction = interaction,
        animations = animations,
        prefix = prefix,
        decimalPlaces = decimalPlaces,
        updateIntervalMs = updateIntervalMs,
        color = color,
        size = size,
        depth = depth,
        align = align,
        baseline = baseline,
        fontUrl = fontUrl
    ).also(nodes::add)

    fun audio(
        id: String = generateNodeId(),
        url: String? = null,
        procedural: ProceduralAudioData? = null,
        position: List<Float> = listOf(0f, 0f, 0f),
        volume: Float = 1f,
        loop: Boolean = false,
        autoplay: Boolean = false,
        bus: String = "master",
        positional: Boolean = false,
        refDistance: Float = 1f,
        maxDistance: Float = 10000f,
        rolloffFactor: Float = 1f,
        visible: Boolean = true,
        name: String? = null
    ): AudioData = AudioData(
        id = id,
        position = position,
        visible = visible,
        name = name,
        url = url,
        procedural = procedural,
        bus = bus,
        volume = volume,
        loop = loop,
        autoplay = autoplay,
        positional = positional,
        refDistance = refDistance,
        maxDistance = maxDistance,
        rolloffFactor = rolloffFactor
    ).also(nodes::add)

    fun audioBus(
        id: String = generateNodeId(),
        bus: String,
        volume: Float = 1f,
        storageKey: String? = null,
        storageBackend: StorageBackend = StorageBackend.LOCAL_STORAGE
    ): AudioBusData = AudioBusData(
        id = id,
        bus = bus,
        volume = volume,
        storageKey = storageKey,
        storageBackend = storageBackend
    ).also(nodes::add)

    /**
     * Add a camera controls node to the scene
     */
    fun controls(
        id: String = generateNodeId(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        controlsType: ControlsType = ControlsType.ORBIT,
        target: List<Float> = listOf(0f, 0f, 0f),
        enableDamping: Boolean = true,
        dampingFactor: Float = 0.05f,
        dampingTime: Float = 0.04f,
        settleEpsilon: Float = 0.0001f,
        maxDeltaTime: Float = 0.05f,
        minDistance: Float = 1f,
        maxDistance: Float = 1000f,
        minPolarAngle: Float = 0f,
        maxPolarAngle: Float = kotlin.math.PI.toFloat(),
        minAzimuthAngle: Float = -Float.MAX_VALUE,
        maxAzimuthAngle: Float = Float.MAX_VALUE,
        rotateSpeed: Float = 1f,
        zoomSpeed: Float = 1f,
        panSpeed: Float = 1f,
        keyboardSpeed: Float = 1f,
        enableRotate: Boolean = true,
        enableZoom: Boolean = true,
        enablePan: Boolean = true,
        enableKeys: Boolean = true,
        autoRotate: Boolean = false,
        autoRotateSpeed: Float = 2f,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList()
    ): ControlsData {
        val controls = ControlsData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            controlsType = controlsType,
            target = target,
            enableDamping = enableDamping,
            dampingFactor = dampingFactor,
            dampingTime = dampingTime,
            settleEpsilon = settleEpsilon,
            maxDeltaTime = maxDeltaTime,
            minDistance = minDistance,
            maxDistance = maxDistance,
            minPolarAngle = minPolarAngle,
            maxPolarAngle = maxPolarAngle,
            minAzimuthAngle = minAzimuthAngle,
            maxAzimuthAngle = maxAzimuthAngle,
            rotateSpeed = rotateSpeed,
            zoomSpeed = zoomSpeed,
            panSpeed = panSpeed,
            keyboardSpeed = keyboardSpeed,
            enableRotate = enableRotate,
            enableZoom = enableZoom,
            enablePan = enablePan,
            enableKeys = enableKeys,
            autoRotate = autoRotate,
            autoRotateSpeed = autoRotateSpeed
        )
        nodes.add(controls)
        return controls
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
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
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
            interaction = interaction,
            animations = animations,
            children = builder.build()
        )
        nodes.add(group)
        return group
    }

    fun screenLayer(
        id: String = generateNodeId(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
        desktop: ScreenLayoutData = ScreenLayoutData(),
        mobile: ScreenLayoutData = desktop,
        mobileBreakpoint: Int = 640,
        order: Int = 0,
        clearDepth: Boolean = true,
        content: SigilGroupBuilder.() -> Unit
    ): ScreenLayerData {
        val builder = SigilGroupBuilder().apply(content)
        return ScreenLayerData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            desktop = desktop,
            mobile = mobile,
            mobileBreakpoint = mobileBreakpoint,
            order = order,
            clearDepth = clearDepth,
            children = builder.build()
        ).also(nodes::add)
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
        exposure: Float = 1f,
        rendererPreference: RendererPreference = RendererPreference.AUTO,
        adaptiveResolution: AdaptiveResolutionData? = null
    ) {
        settings = SceneSettings(
            backgroundColor = backgroundColor,
            fogEnabled = fogEnabled,
            fogColor = fogColor,
            fogNear = fogNear,
            fogFar = fogFar,
            shadowsEnabled = shadowsEnabled,
            toneMapping = toneMapping,
            exposure = exposure,
            rendererPreference = rendererPreference,
            adaptiveResolution = adaptiveResolution
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
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
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
            interaction = interaction,
            animations = animations,
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
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList()
    ): LightData {
        val light = LightData(
            id = id,
            position = position,
            rotation = listOf(0f, 0f, 0f),
            scale = listOf(1f, 1f, 1f),
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            lightType = lightType,
            color = color,
            intensity = intensity,
            distance = distance,
            decay = decay
        )
        children.add(light)
        return light
    }

    fun model(
        id: String = generateNodeId(),
        url: String,
        preloadUrls: List<String> = emptyList(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
        castShadow: Boolean = true,
        receiveShadow: Boolean = true,
        materialOverrides: List<ModelMaterialOverride> = emptyList()
    ): ModelData {
        val model = ModelData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            url = url,
            preloadUrls = preloadUrls,
            castShadow = castShadow,
            receiveShadow = receiveShadow,
            materialOverrides = materialOverrides
        )
        children.add(model)
        return model
    }

    fun text(
        id: String = generateNodeId(),
        text: String,
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
        color: Int = 0xFFFFFFFF.toInt(),
        size: Float = 1f,
        depth: Float = 0.02f,
        curveSegments: Int = 12,
        letterSpacing: Float = 0f,
        lineHeight: Float = 1.2f,
        align: TextAlignMode = TextAlignMode.LEFT,
        baseline: TextBaselineMode = TextBaselineMode.ALPHABETIC,
        maxWidth: Float? = null,
        wordWrap: Boolean = false,
        facingMode: TextFacingMode = TextFacingMode.FIXED,
        fontUrl: String? = null,
        castShadow: Boolean = false,
        receiveShadow: Boolean = false
    ): TextData {
        val textNode = TextData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            text = text,
            color = color,
            size = size,
            depth = depth,
            curveSegments = curveSegments,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            align = align,
            baseline = baseline,
            maxWidth = maxWidth,
            wordWrap = wordWrap,
            facingMode = facingMode,
            fontUrl = fontUrl,
            castShadow = castShadow,
            receiveShadow = receiveShadow
        )
        children.add(textNode)
        return textNode
    }

    fun frameStatsText(
        id: String = generateNodeId(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
        prefix: String = "FPS ",
        decimalPlaces: Int = 0,
        updateIntervalMs: Int = 250,
        color: Int = 0xFFFFFFFF.toInt(),
        size: Float = 18f,
        depth: Float = 0.01f,
        align: TextAlignMode = TextAlignMode.LEFT,
        baseline: TextBaselineMode = TextBaselineMode.TOP,
        fontUrl: String? = null
    ): FrameStatsTextData = FrameStatsTextData(
        id = id,
        position = position,
        rotation = rotation,
        scale = scale,
        visible = visible,
        name = name,
        interaction = interaction,
        animations = animations,
        prefix = prefix,
        decimalPlaces = decimalPlaces,
        updateIntervalMs = updateIntervalMs,
        color = color,
        size = size,
        depth = depth,
        align = align,
        baseline = baseline,
        fontUrl = fontUrl
    ).also(children::add)

    fun audio(
        id: String = generateNodeId(),
        url: String? = null,
        procedural: ProceduralAudioData? = null,
        position: List<Float> = listOf(0f, 0f, 0f),
        volume: Float = 1f,
        loop: Boolean = false,
        autoplay: Boolean = false,
        bus: String = "master",
        positional: Boolean = false,
        refDistance: Float = 1f,
        maxDistance: Float = 10000f,
        rolloffFactor: Float = 1f,
        visible: Boolean = true,
        name: String? = null
    ): AudioData = AudioData(
        id = id,
        position = position,
        visible = visible,
        name = name,
        url = url,
        procedural = procedural,
        bus = bus,
        volume = volume,
        loop = loop,
        autoplay = autoplay,
        positional = positional,
        refDistance = refDistance,
        maxDistance = maxDistance,
        rolloffFactor = rolloffFactor
    ).also(children::add)

    fun audioBus(
        id: String = generateNodeId(),
        bus: String,
        volume: Float = 1f,
        storageKey: String? = null,
        storageBackend: StorageBackend = StorageBackend.LOCAL_STORAGE
    ): AudioBusData = AudioBusData(
        id = id,
        bus = bus,
        volume = volume,
        storageKey = storageKey,
        storageBackend = storageBackend
    ).also(children::add)

    fun controls(
        id: String = generateNodeId(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        controlsType: ControlsType = ControlsType.ORBIT,
        target: List<Float> = listOf(0f, 0f, 0f),
        enableDamping: Boolean = true,
        dampingFactor: Float = 0.05f,
        dampingTime: Float = 0.04f,
        settleEpsilon: Float = 0.0001f,
        maxDeltaTime: Float = 0.05f,
        minDistance: Float = 1f,
        maxDistance: Float = 1000f,
        minPolarAngle: Float = 0f,
        maxPolarAngle: Float = kotlin.math.PI.toFloat(),
        minAzimuthAngle: Float = -Float.MAX_VALUE,
        maxAzimuthAngle: Float = Float.MAX_VALUE,
        rotateSpeed: Float = 1f,
        zoomSpeed: Float = 1f,
        panSpeed: Float = 1f,
        keyboardSpeed: Float = 1f,
        enableRotate: Boolean = true,
        enableZoom: Boolean = true,
        enablePan: Boolean = true,
        enableKeys: Boolean = true,
        autoRotate: Boolean = false,
        autoRotateSpeed: Float = 2f,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList()
    ): ControlsData {
        val controls = ControlsData(
            id = id,
            position = position,
            rotation = rotation,
            scale = scale,
            visible = visible,
            name = name,
            interaction = interaction,
            animations = animations,
            controlsType = controlsType,
            target = target,
            enableDamping = enableDamping,
            dampingFactor = dampingFactor,
            dampingTime = dampingTime,
            settleEpsilon = settleEpsilon,
            maxDeltaTime = maxDeltaTime,
            minDistance = minDistance,
            maxDistance = maxDistance,
            minPolarAngle = minPolarAngle,
            maxPolarAngle = maxPolarAngle,
            minAzimuthAngle = minAzimuthAngle,
            maxAzimuthAngle = maxAzimuthAngle,
            rotateSpeed = rotateSpeed,
            zoomSpeed = zoomSpeed,
            panSpeed = panSpeed,
            keyboardSpeed = keyboardSpeed,
            enableRotate = enableRotate,
            enableZoom = enableZoom,
            enablePan = enablePan,
            enableKeys = enableKeys,
            autoRotate = autoRotate,
            autoRotateSpeed = autoRotateSpeed
        )
        children.add(controls)
        return controls
    }

    fun group(
        id: String = generateNodeId(),
        position: List<Float> = listOf(0f, 0f, 0f),
        rotation: List<Float> = listOf(0f, 0f, 0f),
        scale: List<Float> = listOf(1f, 1f, 1f),
        visible: Boolean = true,
        name: String? = null,
        interaction: InteractionMetadata? = null,
        animations: List<SceneAnimationData> = emptyList(),
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
            interaction = interaction,
            animations = animations,
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
