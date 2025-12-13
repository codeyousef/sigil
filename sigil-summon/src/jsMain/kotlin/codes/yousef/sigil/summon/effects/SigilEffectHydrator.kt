package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.effects.*
import io.materia.effects.fullScreenEffect
import io.materia.effects.uniformBlock
import io.materia.effects.BlendMode as MateriaBlendMode
import io.materia.effects.RenderLoop
import io.materia.effects.FrameInfo
import io.materia.effects.FullScreenEffectPass
import io.materia.renderer.webgpu.WebGPUEffectComposer
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

private val scope = MainScope()

/**
 * External declaration for ResizeObserver.
 */
external class ResizeObserver(callback: (Array<dynamic>) -> Unit) {
    fun observe(target: Element)
    fun unobserve(target: Element)
    fun disconnect()
}

/**
 * Setup a ResizeObserver on an element with a Kotlin callback.
 */
private fun setupResizeObserver(element: Element, onResize: (Double, Double) -> Unit) {
    val observer = ResizeObserver { entries ->
        entries.forEach { entry ->
            val rect = entry.contentRect
            val width = rect.width as Double
            val height = rect.height as Double
            onResize(width, height)
        }
    }
    observer.observe(element)
}

/**
 * Renderer type detected or selected for effect rendering.
 */
enum class RendererType {
    WEBGPU,
    WEBGL,
    CSS_FALLBACK,
    NONE
}

/**
 * Hydrator class for screen-space effects.
 * 
 * Automatically detects browser capabilities and uses the best available renderer:
 * - WebGPU (preferred): Uses WGSL shaders via Materia's EffectComposer
 * - WebGL (fallback): Uses GLSL shaders via WebGLEffectComposer
 * - CSS (last resort): Shows static fallback content
 * 
 * Uses Materia's EffectComposer and FullScreenEffectPass for WebGPU rendering,
 * or WebGLEffectComposer and WebGLEffectPass for WebGL fallback.
 */
class SigilEffectHydrator(
    private val canvas: HTMLCanvasElement,
    private val composerData: EffectComposerData,
    private val config: SigilCanvasConfig,
    private val interactions: InteractionConfig
) {
    private var running = false
    private var animationFrameId: Int = 0
    private var rendererType: RendererType = RendererType.NONE
    
    // Materia WebGPU effect pipeline components
    private var webGPUComposer: WebGPUEffectComposer? = null
    private var webGPUDevice: dynamic = null
    private var webGPUContext: dynamic = null
    private val effectPasses = mutableMapOf<String, FullScreenEffectPass>()
    private val declaredUniformsByEffectId = mutableMapOf<String, Set<String>>()
    private val loggedMissingUniforms = mutableSetOf<String>()
    private var renderLoop: RenderLoop? = null
    
    // WebGL fallback hydrator
    private var webGLHydrator: WebGLEffectHydrator? = null
    
    // Interaction state
    private var mouseX = 0f
    private var mouseY = 0f
    private var isMouseDown = false

    private var lastFrameTotalTime = 0f
    
    /**
     * Initialize the effect hydrator.
     * Automatically detects browser capabilities and selects the best renderer.
     */
    suspend fun initialize(): Boolean {
        // Sync canvas buffer size with CSS display size
        syncCanvasSize()
        
        // Detect best available renderer
        rendererType = detectRendererType()
        
        return when (rendererType) {
            RendererType.WEBGPU -> initializeWebGPU()
            RendererType.WEBGL -> initializeWebGL()
            RendererType.CSS_FALLBACK -> {
                console.log("SigilEffectHydrator: Using CSS fallback")
                showCSSFallback()
                true
            }
            RendererType.NONE -> {
                console.error("SigilEffectHydrator: No rendering method available")
                false
            }
        }
    }
    
    /**
     * Detect the best available renderer type based on browser capabilities
     * and effect shader availability.
     * 
     * As of Materia 0.3.4.0, both WebGPU and WebGL effect composers are available:
     * - WebGPUEffectComposer: Uses WGSL shaders (preferred when WebGPU available)
     * - WebGLEffectComposer: Uses GLSL shaders (fallback for Firefox, Safari)
     */
    private fun detectRendererType(): RendererType {
        val hasWebGPU = isWebGPUAvailable()
        val hasWebGL = WebGLEffectHydrator.isWebGLAvailable()
        
        // Check if effects have the required shaders
        val hasGLSLShaders = composerData.effects.any { it.enabled && it.glslFragmentShader != null }
        val hasWGSLShaders = composerData.effects.any { it.enabled && it.fragmentShader.isNotBlank() }
        
        console.log("SigilEffectHydrator: WebGPU=$hasWebGPU, WebGL=$hasWebGL, WGSL=$hasWGSLShaders, GLSL=$hasGLSLShaders")
        
        return when {
            // Prefer WebGPU with WGSL shaders
            hasWebGPU && hasWGSLShaders -> {
                console.log("SigilEffectHydrator: Using WebGPU with WGSL shaders")
                RendererType.WEBGPU
            }
            // Fall back to WebGL with GLSL shaders
            hasWebGL && hasGLSLShaders && config.fallbackToWebGL -> {
                console.log("SigilEffectHydrator: Using WebGL with GLSL shaders")
                RendererType.WEBGL
            }
            // CSS fallback
            config.fallbackToCSS -> {
                if (hasWGSLShaders && !hasWebGPU) {
                    console.warn("SigilEffectHydrator: WebGPU not available. Provide glslFragmentShader for WebGL fallback.")
                }
                RendererType.CSS_FALLBACK
            }
            else -> RendererType.NONE
        }
    }
    
    /**
     * Check if WebGPU is available.
     */
    private fun isWebGPUAvailable(): Boolean {
        return try {
            val navigator = js("navigator")
            navigator.gpu != undefined && navigator.gpu != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Initialize WebGPU rendering using Materia's WebGPUEffectComposer.
     */
    private suspend fun initializeWebGPU(): Boolean {
        try {
            // Request WebGPU adapter and device
            val navigator = js("navigator")
            val gpu = navigator.gpu
            if (gpu == null || gpu == undefined) {
                console.error("SigilEffectHydrator: WebGPU not available")
                return fallbackToWebGL()
            }
            
            val adapterPromise: kotlin.js.Promise<dynamic> = gpu.requestAdapter().unsafeCast<kotlin.js.Promise<dynamic>>()
            val adapter: dynamic = adapterPromise.await()
            if (adapter == null) {
                console.error("SigilEffectHydrator: Failed to get WebGPU adapter")
                return fallbackToWebGL()
            }
            
            val devicePromise: kotlin.js.Promise<dynamic> = adapter.requestDevice().unsafeCast<kotlin.js.Promise<dynamic>>()
            val device: dynamic = devicePromise.await()
            if (device == null) {
                console.error("SigilEffectHydrator: Failed to get WebGPU device")
                return fallbackToWebGL()
            }
            
            // Configure canvas context for WebGPU
            val context = canvas.getContext("webgpu")
            if (context == null) {
                console.error("SigilEffectHydrator: Failed to get WebGPU canvas context")
                return fallbackToWebGL()
            }
            
            val format = gpu.getPreferredCanvasFormat()
            context.asDynamic().configure(js("{device: device, format: format, alphaMode: 'premultiplied'}"))
            
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
            
            // Create render loop for uniform updates
            renderLoop = RenderLoop { frame: FrameInfo ->
                try {
                    updateEffects(frame)
                } catch (t: Throwable) {
                    console.error("SigilEffectHydrator: WebGPU frame update failed: ${t.message}")
                    console.error(t)
                }
            }
            
            // Setup interaction listeners
            if (hasMouseInteraction()) {
                setupMouseListeners()
            }
            
            console.log("SigilEffectHydrator: Initialized WebGPU with ${effectPasses.size} effect passes")
            return true
        } catch (e: Exception) {
            console.error("SigilEffectHydrator: Failed to initialize WebGPU: ${e.message}")
            return fallbackToWebGL()
        }
    }
    
    /**
     * Fallback to WebGL if WebGPU initialization fails.
     */
    private fun fallbackToWebGL(): Boolean {
        if (config.fallbackToWebGL) {
            console.log("SigilEffectHydrator: Attempting WebGL fallback")
            rendererType = RendererType.WEBGL
            return initializeWebGL()
        }
        return false
    }
    
    /**
     * Initialize WebGL fallback rendering.
     */
    private fun initializeWebGL(): Boolean {
        webGLHydrator = WebGLEffectHydrator(canvas, composerData, config, interactions)
        val success = webGLHydrator?.initialize() ?: false
        
        if (!success && config.fallbackToCSS) {
            console.log("SigilEffectHydrator: WebGL failed, using CSS fallback")
            rendererType = RendererType.CSS_FALLBACK
            showCSSFallback()
            return true
        }
        
        return success
    }
    
    /**
     * Show CSS fallback content.
     */
    private fun showCSSFallback() {
        val fallbackElement = document.getElementById("${canvas.id}-fallback")
        if (fallbackElement != null) {
            fallbackElement.asDynamic().style.display = "block"
            canvas.asDynamic().style.display = "none"
        }
    }
    
    /**
     * Get the current renderer type being used.
     */
    fun getRendererType(): RendererType = rendererType
    
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
            // Note: OVERLAY maps to MULTIPLY as an approximation since true
            // overlay blending requires shader-based implementation.
            blendMode = when (effectData.blendMode) {
                BlendMode.NORMAL -> MateriaBlendMode.ALPHA_BLEND
                BlendMode.ADDITIVE -> MateriaBlendMode.ADDITIVE
                BlendMode.MULTIPLY -> MateriaBlendMode.MULTIPLY
                BlendMode.SCREEN -> MateriaBlendMode.SCREEN
                BlendMode.OVERLAY -> MateriaBlendMode.OVERLAY
            }
            
            // Build uniforms block
            uniforms {
                // WebGPU: Materia packs uniforms into a single uniform buffer (binding 0).
                // To ensure correct offsets, declare fields in the exact order used by the WGSL
                // struct bound to `var<uniform> uniforms: ...`.

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

                // Declare any remaining schema-provided uniforms that were not present in the WGSL
                // struct (won't affect offsets of the struct fields).
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
     * This is required to make the CPU-side uniform packing match the shader's expected offsets.
     */
    private fun extractWgslUniformStructFields(wgsl: String): List<WgslUniformField> {
        // 1. Find the struct name used in 'var<uniform> ... : StructName;'
        // Allow any variable name, and handle potential attributes before 'var'
        val uniformVarRegex = Regex(
            """var<uniform>\s+([A-Za-z_][A-Za-z0-9_]*)\s*:\s*([A-Za-z_][A-Za-z0-9_]*)\s*;"""
        )
        val match = uniformVarRegex.find(wgsl) ?: return emptyList()
        val structName = match.groupValues.getOrNull(2) ?: return emptyList()

        // 2. Find the struct definition: struct StructName { ... }
        val structHeaderRegex = Regex("""\bstruct\s+${Regex.escape(structName)}\b""")
        val headerMatch = structHeaderRegex.find(wgsl) ?: return emptyList()
        
        val openBraceIndex = wgsl.indexOf('{', startIndex = headerMatch.range.last + 1)
        if (openBraceIndex < 0) return emptyList()

        val closeBraceIndex = findMatchingBrace(wgsl, openBraceIndex)
        if (closeBraceIndex <= openBraceIndex) return emptyList()

        val body = wgsl.substring(openBraceIndex + 1, closeBraceIndex)

        // 3. Parse fields. WGSL fields are comma-separated.
        // Remove comments first
        val bodyNoComments = body.lines().joinToString("\n") { line ->
            val idx = line.indexOf("//")
            if (idx >= 0) line.substring(0, idx) else line
        }

        // Split by comma, respecting brackets
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
        // Last token
        if (currentToken.isNotEmpty()) {
            parseField(currentToken.toString())?.let { fields.add(it) }
        }
        
        return fields
    }

    private fun parseField(token: String): WgslUniformField? {
        val trimmed = token.trim()
        if (trimmed.isBlank()) return null
        // Match "name : type"
        // Type can contain < >
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
            t.startsWith("u32") -> int(name) // Treat u32 as int for layout
            else -> float(name)
        }
    }
    
    /**
     * Update all effects with current frame and interaction data.
     */
    private fun updateEffects(frame: FrameInfo) {
        val totalTime = frame.totalTime
        val deltaTime = (totalTime - lastFrameTotalTime).coerceAtLeast(0f)
        lastFrameTotalTime = totalTime

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
                        set("mouse", mouseX, mouseY)
                    } else {
                        set("mouse", 0.5f, 0.5f)
                    }
                }
                if (declared.contains("mouseDown")) {
                    set("mouseDown", if (effectData.enableMouseInteraction && isMouseDown) 1f else 0f)
                }
                
                // Effect-specific uniforms
                effectData.uniforms.forEach { (name, value) ->
                    if (!declared.contains(name)) {
                        val key = "${effectData.id}:$name"
                        if (loggedMissingUniforms.add(key)) {
                            console.warn("SigilEffectHydrator: Uniform '$name' not declared in shader for effect ${effectData.id}")
                        }
                        return@forEach
                    }
                    when (value) {
                        is UniformValue.FloatValue -> set(name, value.value)
                        is UniformValue.IntValue -> set(name, value.value.toFloat())
                        is UniformValue.Vec2Value -> set(name, value.value.x, value.value.y)
                        is UniformValue.Vec3Value -> set(name, value.value.x, value.value.y, value.value.z)
                        is UniformValue.Vec4Value -> set(name, value.value.x, value.value.y, value.value.z, value.value.w)
                        is UniformValue.Mat3Value -> setMat3(name, value.values.toFloatArray())
                        is UniformValue.Mat4Value -> setMat4(name, value.values.toFloatArray())
                    }
                }
            }
        }
    }
    
    /**
     * Check if any effect requires mouse interaction.
     */
    private fun hasMouseInteraction(): Boolean {
        return composerData.effects.any { it.enableMouseInteraction } ||
               interactions.enableMouseMove
    }
    
    /**
     * Setup mouse event listeners for interaction.
     */
    private fun setupMouseListeners() {
        canvas.addEventListener("mousemove", { event ->
            val e = event as MouseEvent
            val rect = canvas.getBoundingClientRect()
            mouseX = ((e.clientX - rect.left) / rect.width).toFloat()
            mouseY = ((e.clientY - rect.top) / rect.height).toFloat()
        })
        
        canvas.addEventListener("mousedown", { isMouseDown = true })
        canvas.addEventListener("mouseup", { isMouseDown = false })
        canvas.addEventListener("mouseleave", { isMouseDown = false })
        
        // Touch support
        canvas.addEventListener("touchmove", { event ->
            val e = event.asDynamic()
            if (e.touches.length > 0) {
                val touch = e.touches[0]
                val rect = canvas.getBoundingClientRect()
                mouseX = ((touch.clientX - rect.left) / rect.width).toFloat()
                mouseY = ((touch.clientY - rect.top) / rect.height).toFloat()
            }
        })
        
        canvas.addEventListener("touchstart", { isMouseDown = true })
        canvas.addEventListener("touchend", { isMouseDown = false })
    }
    
    /**
     * Start the render loop.
     */
    fun startRenderLoop() {
        when (rendererType) {
            RendererType.WEBGPU -> startWebGPURenderLoop()
            RendererType.WEBGL -> webGLHydrator?.startRenderLoop()
            RendererType.CSS_FALLBACK, RendererType.NONE -> {
                // No render loop needed for CSS fallback
            }
        }
    }
    
    /**
     * Start WebGPU render loop using WebGPUEffectComposer.
     */
    private fun startWebGPURenderLoop() {
        running = true
        renderLoop?.start()
        
        fun animate() {
            if (!running) return
            
            try {
                // Get current swapchain texture from canvas context
                val currentTexture = webGPUContext.getCurrentTexture()
                if (currentTexture != null && currentTexture != undefined) {
                    val textureView = currentTexture.createView()
                    
                    // Render all effect passes to the swapchain texture
                    // Materia 0.3.4.2+ exports render() via @JsExport
                    webGPUComposer?.render(textureView)
                }
            } catch (e: dynamic) {
                console.error("SigilEffectHydrator: WebGPU render error: $e")
            }
            
            animationFrameId = window.requestAnimationFrame { animate() }
        }
        
        animate()
        console.log("SigilEffectHydrator: WebGPU render loop started with ${effectPasses.size} passes")
    }
    
    /**
     * Stop the render loop.
     */
    fun stop() {
        running = false
        renderLoop?.stop()
        webGLHydrator?.stop()
        
        if (animationFrameId != 0) {
            window.cancelAnimationFrame(animationFrameId)
            animationFrameId = 0
        }
    }
    
    /**
     * Clean up resources.
     */
    fun dispose() {
        stop()
        webGPUComposer?.dispose()
        webGPUComposer = null
        webGPUDevice = null
        webGPUContext = null
        effectPasses.clear()
        webGLHydrator?.dispose()
        webGLHydrator = null
    }
    
    /**
     * Sync canvas buffer size with CSS display size.
     * This ensures the WebGPU/WebGL buffer matches the actual display size.
     */
    private fun syncCanvasSize() {
        val rect = canvas.getBoundingClientRect()
        val dpr = window.devicePixelRatio
        val displayWidth = (rect.width * dpr).toInt()
        val displayHeight = (rect.height * dpr).toInt()
        
        if (canvas.width != displayWidth || canvas.height != displayHeight) {
            canvas.width = displayWidth
            canvas.height = displayHeight
            console.log("SigilEffectHydrator: Synced canvas size to ${displayWidth}x${displayHeight} (DPR: $dpr)")
        }
    }
    
    /**
     * Handle canvas resize.
     */
    fun resize(width: Int, height: Int) {
        // Update canvas buffer size
        val dpr = window.devicePixelRatio
        val bufferWidth = (width * dpr).toInt()
        val bufferHeight = (height * dpr).toInt()
        canvas.width = bufferWidth
        canvas.height = bufferHeight
        
        when (rendererType) {
            RendererType.WEBGPU -> webGPUComposer?.setSize(bufferWidth, bufferHeight)
            RendererType.WEBGL -> webGLHydrator?.resize(bufferWidth, bufferHeight)
            RendererType.CSS_FALLBACK, RendererType.NONE -> {}
        }
    }
    
    companion object {
        /**
         * Hydrate effects from DOM data.
         * Called by the hydration script embedded in the HTML.
         * 
         * Reads effect data from the data-sigil-effects attribute on the canvas.
         */
        fun hydrateFromDOM(canvasId: String) {
            scope.launch {
                val canvas = document.getElementById(canvasId) as? HTMLCanvasElement
                
                if (canvas == null) {
                    console.error("SigilEffect: Canvas not found for $canvasId")
                    return@launch
                }
                
                // Get effect data from the data-sigil-effects attribute
                val effectJson = canvas.getAttribute("data-sigil-effects")
                    ?.replace("&#39;", "'")
                    ?.replace("&lt;", "<")
                    ?.replace("&gt;", ">")
                    ?.replace("&amp;", "&")
                
                if (effectJson == null || !effectJson.startsWith("{")) {
                    console.error("SigilEffect: No valid effect data found in data-sigil-effects attribute for $canvasId")
                    return@launch
                }
                
                val configJson = canvas.getAttribute("data-sigil-config")
                    ?.replace("&#39;", "'")
                    ?.replace("&lt;", "<")
                    ?.replace("&gt;", ">")
                    ?.replace("&amp;", "&") ?: "{}"
                val interactionsJson = canvas.getAttribute("data-sigil-interactions")
                    ?.replace("&#39;", "'")
                    ?.replace("&lt;", "<")
                    ?.replace("&gt;", ">")
                    ?.replace("&amp;", "&") ?: "{}"
                
                try {
                    val composerData = SigilJson.decodeFromString<EffectComposerData>(effectJson)
                    val config = SigilJson.decodeFromString<SigilCanvasConfig>(configJson)
                    val interactions = SigilJson.decodeFromString<InteractionConfig>(interactionsJson)
                    
                    val hydrator = SigilEffectHydrator(canvas, composerData, config, interactions)
                    if (hydrator.initialize()) {
                        hydrator.startRenderLoop()
                        
                        // Setup resize observer using Kotlin callback
                        setupResizeObserver(canvas) { width, height ->
                            hydrator.resize(width.toInt(), height.toInt())
                        }
                    }
                } catch (e: Exception) {
                    console.error("SigilEffect: Failed to hydrate $canvasId: ${e.message}")
                }
            }
        }
    }
}

/**
 * Global hydration entry point exposed to JavaScript.
 */
@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("SigilEffectHydrator")
object SigilEffectHydratorJs {
    @JsName("hydrate")
    fun hydrate(canvasId: String) {
        SigilEffectHydrator.hydrateFromDOM(canvasId)
    }
    
    /**
     * Check if WebGPU is available in the current browser.
     */
    @JsName("isWebGPUAvailable")
    fun isWebGPUAvailable(): Boolean {
        return try {
            val navigator = js("navigator")
            navigator.gpu != undefined && navigator.gpu != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if WebGL is available in the current browser.
     */
    @JsName("isWebGLAvailable")
    fun isWebGLAvailable(): Boolean {
        return WebGLEffectHydrator.isWebGLAvailable()
    }
    
    /**
     * Get the renderer type that will be used based on browser capabilities.
     * Returns: "webgpu", "webgl", "css", or "none"
     */
    @JsName("getAvailableRenderer")
    fun getAvailableRenderer(): String {
        return when {
            isWebGPUAvailable() -> "webgpu"
            isWebGLAvailable() -> "webgl"
            else -> "css"
        }
    }
}
