package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.effects.*
import io.materia.effects.BlendMode as MateriaBlendMode
import io.materia.effects.FullScreenEffectPass
import io.materia.renderer.webgpu.WebGPUEffectComposer
import kotlinx.coroutines.await
import org.w3c.dom.HTMLCanvasElement

/**
 * Handles WebGPU initialization and rendering for Sigil effects.
 */
class WebGPURenderer(
    private val canvas: HTMLCanvasElement,
    private val composerData: EffectComposerData,
    private val config: SigilCanvasConfig,
    private val interactionHandler: InteractionHandler
) {
    private var webGPUComposer: WebGPUEffectComposer? = null
    private var webGPUDevice: dynamic = null
    private var webGPUContext: dynamic = null
    private val effectPasses = mutableMapOf<String, FullScreenEffectPass>()
    private val declaredUniformsByEffectId = mutableMapOf<String, Set<String>>()
    private val loggedMissingUniforms = mutableSetOf<String>()
    private val renderLoop = RenderLoop()

    /**
     * Initialize WebGPU rendering.
     */
    suspend fun initialize(): Boolean {
        try {
            // Request WebGPU adapter and device
            val navigator = js("navigator")
            val gpu = navigator.gpu
            if (gpu == null || gpu == undefined) {
                console.error("WebGPURenderer: WebGPU not available")
                return false
            }
            
            val adapterPromise: kotlin.js.Promise<dynamic> = gpu.requestAdapter().unsafeCast<kotlin.js.Promise<dynamic>>()
            val adapter: dynamic = adapterPromise.await()
            if (adapter == null) {
                console.error("WebGPURenderer: Failed to get WebGPU adapter")
                return false
            }
            
            val devicePromise: kotlin.js.Promise<dynamic> = adapter.requestDevice().unsafeCast<kotlin.js.Promise<dynamic>>()
            val device: dynamic = devicePromise.await()
            if (device == null) {
                console.error("WebGPURenderer: Failed to get WebGPU device")
                return false
            }
            
            // Configure canvas context for WebGPU
            val context = canvas.getContext("webgpu")
            if (context == null) {
                console.error("WebGPURenderer: Failed to get WebGPU canvas context")
                return false
            }
            
            val format = gpu.getPreferredCanvasFormat()
            // Cast context to dynamic first, then call configure
            val gpuContext: dynamic = context
            gpuContext.configure(js("{device: device, format: format, alphaMode: 'premultiplied'}"))
            
            // Store device and context for render loop
            webGPUDevice = device
            webGPUContext = context
            
            // Create WebGPUEffectComposer (cast device to expected type)
            webGPUComposer = WebGPUEffectComposer(
                device = device.unsafeCast<io.materia.renderer.webgpu.GPUDevice>(),
                width = canvas.width,
                height = canvas.height
            )
            
            // Create FullScreenEffectPass for each effect in the composer data
            composerData.effects.forEach { effectData ->
                if (effectData.enabled) {
                    val pass = createEffectPass(effectData)
                    effectPasses[effectData.id] = pass
                    webGPUComposer?.addPass(pass)
                }
            }
            
            console.log("WebGPURenderer: Initialized WebGPU with ${effectPasses.size} effect passes")
            return true
        } catch (e: Exception) {
            console.error("WebGPURenderer: Failed to initialize WebGPU: ${e.message}")
            return false
        }
    }
    
    /**
     * Start the render loop.
     */
    fun startRenderLoop() {
        renderLoop.start { totalTimeSeconds, deltaTimeSeconds ->
            try {
                updateEffectsWithTime(totalTimeSeconds.toFloat(), deltaTimeSeconds.toFloat())
                webGPUComposer?.render(webGPUContext.getCurrentTexture().createView())
            } catch (e: Exception) {
                console.error("WebGPURenderer: Error in render loop: ${e.message}")
                stopRenderLoop()
            }
        }
    }
    
    /**
     * Stop the render loop.
     */
    fun stopRenderLoop() {
        renderLoop.stop()
    }
    
    /**
     * Resize the renderer.
     */
    fun resize(width: Double, height: Double) {
        webGPUComposer?.setSize(width.toInt(), height.toInt())
    }
    
    /**
     * Update all effects with current frame and interaction data.
     */
    private fun updateEffectsWithTime(totalTime: Float, deltaTime: Float) {
        composerData.effects.forEach { effectData ->
            val pass = effectPasses[effectData.id] ?: return@forEach
            val declared = declaredUniformsByEffectId[effectData.id].orEmpty()
            
            pass.updateUniforms {
                // Standard uniforms
                if (declared.contains("time")) set("time", totalTime * effectData.timeScale)
                if (declared.contains("deltaTime")) set("deltaTime", deltaTime)
                if (declared.contains("resolution")) set("resolution", canvas.width.toFloat(), canvas.height.toFloat())
                if (declared.contains("scroll")) set("scroll", 0f)
                if (declared.contains("_padding")) set("_padding", 0f)

                // Optional interaction uniforms (only if present in the shader struct)
                if (declared.contains("mouse")) {
                    if (effectData.enableMouseInteraction) {
                        set("mouse", interactionHandler.mouseX, interactionHandler.mouseY)
                    } else {
                        set("mouse", 0.5f, 0.5f)
                    }
                }
                if (declared.contains("mouseDown")) {
                    set("mouseDown", if (effectData.enableMouseInteraction && interactionHandler.isMouseDown) 1f else 0f)
                }
                
                // Effect-specific uniforms (skip standard uniforms already set above)
                val standardUniforms = setOf("time", "deltaTime", "resolution", "mouse", "mouseDown", "scroll", "_padding")
                effectData.uniforms.forEach { (name, value) ->
                    if (name in standardUniforms) return@forEach
                    if (!declared.contains(name)) {
                        val key = "${effectData.id}:$name"
                        if (loggedMissingUniforms.add(key)) {
                            console.warn("WebGPURenderer: Uniform '$name' not declared in shader for effect ${effectData.id}")
                        }
                        return@forEach
                    }

                    applyUniformValue(
                        name = name,
                        value = value,
                        setFloat = { uniform, v -> set(uniform, v) },
                        setVec2 = { uniform, x, y -> set(uniform, x, y) },
                        setVec3 = { uniform, x, y, z -> set(uniform, x, y, z) },
                        setVec4 = { uniform, x, y, z, w -> set(uniform, x, y, z, w) },
                        setMat3 = { uniform, mat -> setMat3(uniform, mat) },
                        setMat4 = { uniform, mat -> setMat4(uniform, mat) }
                    )
                }
            }
        }
    }

    /**
     * Create a FullScreenEffectPass from shader effect data.
     */
    private fun createEffectPass(effectData: ShaderEffectData): FullScreenEffectPass {
        val uniformStructFields = extractWgslUniformStructFields(effectData.fragmentShader)
        val structFieldNames = uniformStructFields.map { it.name }.toSet()
        val declaredFieldNames = (structFieldNames + effectData.uniforms.keys).toSet()
        declaredUniformsByEffectId[effectData.id] = declaredFieldNames

        return FullScreenEffectPass.create {
            fragmentShader = effectData.fragmentShader
            
            // Map blend modes to Materia equivalents.
            blendMode = when (effectData.blendMode) {
                BlendMode.NORMAL -> MateriaBlendMode.ALPHA_BLEND
                BlendMode.ADDITIVE -> MateriaBlendMode.ADDITIVE
                BlendMode.MULTIPLY -> MateriaBlendMode.MULTIPLY
                BlendMode.SCREEN -> MateriaBlendMode.SCREEN
                BlendMode.OVERLAY -> MateriaBlendMode.OVERLAY
            }
            
            // Build uniforms block
            uniforms {
                val declared = mutableSetOf<String>()

                if (uniformStructFields.isNotEmpty()) {
                    for (field in uniformStructFields) {
                        val name = field.name
                        declared.add(name)
                        val schemaValue = effectData.uniforms[name]
                        if (schemaValue != null) {
                            declareFromSchemaValue(name, schemaValue)
                        } else {
                            declareFromWgslType(name, field.wgslType)
                        }
                    }
                }

                // Declare any remaining schema-provided uniforms
                for ((name, value) in effectData.uniforms.entries.sortedBy { it.key }) {
                    if (declared.contains(name)) continue
                    declared.add(name)
                    declareFromSchemaValue(name, value)
                }
            }
        }
    }

    private data class WgslUniformField(
        val name: String,
        val wgslType: String
    )

    /**
     * Extract field order from the WGSL struct bound to `var<uniform> uniforms: <StructName>;`.
     */
    private fun extractWgslUniformStructFields(wgsl: String): List<WgslUniformField> {
        val uniformVarRegex = Regex(
            """var<uniform>\s+([A-Za-z_][A-Za-z0-9_]*)\s*:\s*([A-Za-z_][A-Za-z0-9_]*)\s*;"""
        )
        val match = uniformVarRegex.find(wgsl) ?: return emptyList()
        val structName = match.groupValues.getOrNull(2) ?: return emptyList()

        val structHeaderRegex = Regex("""\bstruct\s+${Regex.escape(structName)}\b""")
        val headerMatch = structHeaderRegex.find(wgsl) ?: return emptyList()
        
        val openBraceIndex = wgsl.indexOf('{', startIndex = headerMatch.range.last + 1)
        if (openBraceIndex < 0) return emptyList()

        val closeBraceIndex = findMatchingBrace(wgsl, openBraceIndex)
        if (closeBraceIndex <= openBraceIndex) return emptyList()

        val body = wgsl.substring(openBraceIndex + 1, closeBraceIndex)

        val bodyNoComments = body.lines().joinToString("\n") { line ->
            val idx = line.indexOf("//")
            if (idx >= 0) line.substring(0, idx) else line
        }

        val fields = mutableListOf<WgslUniformField>()
        var currentToken = StringBuilder()
        var bracketDepth = 0
        
        for (char in bodyNoComments) {
            when (char) {
                '<' -> {
                    bracketDepth++
                    currentToken.append(char)
                }
                '>' -> {
                    bracketDepth--
                    currentToken.append(char)
                }
                ',' -> {
                    if (bracketDepth == 0) {
                        parseField(currentToken.toString())?.let { fields.add(it) }
                        currentToken.clear()
                    } else {
                        currentToken.append(char)
                    }
                }
                else -> currentToken.append(char)
            }
        }
        if (currentToken.isNotEmpty()) {
            parseField(currentToken.toString())?.let { fields.add(it) }
        }
        
        return fields
    }

    private fun parseField(token: String): WgslUniformField? {
        val trimmed = token.trim()
        if (trimmed.isBlank()) return null
        val parts = trimmed.split(':')
        if (parts.size < 2) return null
        val name = parts[0].trim()
        val type = parts.subList(1, parts.size).joinToString(":").trim()
        if (name.isBlank() || type.isBlank()) return null
        return WgslUniformField(name, type)
    }

    private fun findMatchingBrace(source: String, openIndex: Int): Int {
        if (openIndex !in source.indices || source[openIndex] != '{') return -1
        var depth = 0
        var i = openIndex
        while (i < source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
            i++
        }
        return -1
    }

    private fun io.materia.effects.UniformBlockBuilder.declareFromSchemaValue(name: String, value: UniformValue) {
        when (value) {
            is UniformValue.FloatValue -> float(name)
            is UniformValue.IntValue -> int(name)
            is UniformValue.Vec2Value -> vec2(name)
            is UniformValue.Vec3Value -> vec3(name)
            is UniformValue.Vec4Value -> vec4(name)
            is UniformValue.Mat3Value -> mat3(name)
            is UniformValue.Mat4Value -> mat4(name)
        }
    }

    private fun io.materia.effects.UniformBlockBuilder.declareFromWgslType(name: String, wgslType: String) {
        val t = wgslType.replace(" ", "")
        when {
            t.startsWith("mat4") -> mat4(name)
            t.startsWith("mat3") -> mat3(name)
            t.startsWith("vec4") -> vec4(name)
            t.startsWith("vec3") -> vec3(name)
            t.startsWith("vec2") -> vec2(name)
            t.startsWith("i32") -> int(name)
            t.startsWith("u32") -> int(name)
            else -> float(name)
        }
    }
}
