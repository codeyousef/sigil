package io.github.codeyousef.sigil.schema

import kotlinx.serialization.Serializable

/**
 * Root container for a complete Sigil 3D scene.
 * This is the top-level structure serialized for transmission between JVM and JS.
 */
@Serializable
data class SigilScene(
    /**
     * Top-level nodes in the scene graph.
     * All nodes can have nested children through GroupData.
     */
    val rootNodes: List<SigilNodeData> = emptyList(),

    /**
     * Optional scene-level settings
     */
    val settings: SceneSettings = SceneSettings()
) {
    /**
     * Serialize this scene to JSON string for transmission.
     */
    fun toJson(): String = SigilJson.encodeToString(serializer(), this)

    /**
     * Find a node by ID through recursive traversal.
     */
    fun findNodeById(id: String): SigilNodeData? = findInNodes(rootNodes, id)

    /**
     * Collect all nodes in the scene into a flat list.
     */
    fun flattenNodes(): List<SigilNodeData> = collectNodes(rootNodes)

    private fun findInNodes(nodes: List<SigilNodeData>, id: String): SigilNodeData? {
        for (node in nodes) {
            if (node.id == id) return node
            if (node is GroupData) {
                findInNodes(node.children, id)?.let { return it }
            }
        }
        return null
    }

    private fun collectNodes(nodes: List<SigilNodeData>): List<SigilNodeData> {
        val result = mutableListOf<SigilNodeData>()
        for (node in nodes) {
            result.add(node)
            if (node is GroupData) {
                result.addAll(collectNodes(node.children))
            }
        }
        return result
    }

    companion object {
        /**
         * Deserialize a scene from JSON string.
         */
        fun fromJson(json: String): SigilScene = SigilJson.decodeFromString(serializer(), json)
    }
}

/**
 * Scene-level rendering settings.
 */
@Serializable
data class SceneSettings(
    /**
     * Background color in ARGB hex format
     */
    val backgroundColor: Int = 0xFF1A1A2E.toInt(),

    /**
     * Whether to enable fog
     */
    val fogEnabled: Boolean = false,

    /**
     * Fog color in ARGB hex format
     */
    val fogColor: Int = 0xFFFFFFFF.toInt(),

    /**
     * Fog near distance (linear fog)
     */
    val fogNear: Float = 10f,

    /**
     * Fog far distance (linear fog)
     */
    val fogFar: Float = 100f,

    /**
     * Enable shadow mapping
     */
    val shadowsEnabled: Boolean = true,

    /**
     * Tone mapping mode
     */
    val toneMapping: ToneMappingMode = ToneMappingMode.ACES_FILMIC,

    /**
     * Exposure for tone mapping
     */
    val exposure: Float = 1f
)

/**
 * Supported tone mapping modes.
 */
@Serializable
enum class ToneMappingMode {
    NONE,
    LINEAR,
    REINHARD,
    CINEON,
    ACES_FILMIC
}
