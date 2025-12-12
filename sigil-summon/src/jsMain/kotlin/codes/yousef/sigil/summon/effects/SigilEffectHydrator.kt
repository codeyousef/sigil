package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.effects.*
import io.materia.effects.fullScreenEffect
import io.materia.effects.uniformBlock
import io.materia.effects.BlendMode as MateriaBlendMode
import io.materia.effects.RenderLoop
import io.materia.effects.FrameInfo
import io.materia.effects.FullScreenEffectPass
import io.materia.effects.EffectComposer
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
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
    private var effectComposer: EffectComposer? = null
    private val effectPasses = mutableMapOf<String, FullScreenEffectPass>()
    private var renderLoop: RenderLoop? = null
    
    // WebGL fallback hydrator
    private var webGLHydrator: WebGLEffectHydrator? = null
    
    // Interaction state
    private var mouseX = 0f
    private var mouseY = 0f
    private var isMouseDown = false
    
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
     */
    private fun detectRendererType(): RendererType {
        // Check if WebGPU is available
        val hasWebGPU = isWebGPUAvailable()
        val hasWebGL = WebGLEffectHydrator.isWebGLAvailable()
        
        // Check if effects have the required shaders
        val hasWGSLShaders = composerData.effects.any { it.enabled && it.fragmentShader.isNotBlank() }
        val hasGLSLShaders = composerData.effects.any { it.enabled && it.glslFragmentShader != null }
        
        console.log("SigilEffectHydrator: WebGPU=$hasWebGPU, WebGL=$hasWebGL, WGSL=$hasWGSLShaders, GLSL=$hasGLSLShaders")
        
        return when {
            hasWebGPU && hasWGSLShaders -> RendererType.WEBGPU
            hasWebGL && hasGLSLShaders && config.fallbackToWebGL -> RendererType.WEBGL
            config.fallbackToCSS -> RendererType.CSS_FALLBACK
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
     * Initialize WebGPU rendering.
     */
    private suspend fun initializeWebGPU(): Boolean {
        try {
            // Create Materia EffectComposer
            effectComposer = EffectComposer(
                width = canvas.width,
                height = canvas.height
            )
            
            // Create FullScreenEffectPass for each effect in the composer data
            composerData.effects.forEach { effectData ->
                if (effectData.enabled) {
                    val pass = createEffectPass(effectData)
                    effectPasses[effectData.id] = pass
                    effectComposer?.addPass(pass)
                }
            }
            
            // Create render loop
            renderLoop = RenderLoop { frame: FrameInfo ->
                updateEffects(frame)
            }
            
            // Setup interaction listeners
            if (hasMouseInteraction()) {
                setupMouseListeners()
            }
            
            console.log("SigilEffectHydrator: Initialized WebGPU with ${effectPasses.size} effect passes")
            return true
        } catch (e: Exception) {
            console.error("SigilEffectHydrator: Failed to initialize WebGPU: ${e.message}")
            
            // Try WebGL fallback if available
            if (config.fallbackToWebGL) {
                console.log("SigilEffectHydrator: Attempting WebGL fallback")
                rendererType = RendererType.WEBGL
                return initializeWebGL()
            }
            
            return false
        }
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
                // Always include standard uniforms
                float("time")
                vec2("resolution")
                vec2("mouse")
                float("mouseDown")
                
                // Add effect-specific uniforms
                effectData.uniforms.forEach { (name, value) ->
                    when (value) {
                        is UniformValue.FloatValue -> float(name)
                        is UniformValue.IntValue -> float(name) // WGSL uses f32 for uniform scalars
                        is UniformValue.Vec2Value -> vec2(name)
                        is UniformValue.Vec3Value -> vec3(name)
                        is UniformValue.Vec4Value -> vec4(name)
                        is UniformValue.Mat3Value -> mat3(name)
                        is UniformValue.Mat4Value -> mat4(name)
                    }
                }
            }
        }
    }
    
    /**
     * Update all effects with current frame and interaction data.
     */
    private fun updateEffects(frame: FrameInfo) {
        composerData.effects.forEach { effectData ->
            val pass = effectPasses[effectData.id] ?: return@forEach
            
            pass.updateUniforms {
                // Standard uniforms
                set("time", frame.totalTime * effectData.timeScale)
                set("resolution", canvas.width.toFloat(), canvas.height.toFloat())
                
                if (effectData.enableMouseInteraction) {
                    set("mouse", mouseX, mouseY)
                    set("mouseDown", if (isMouseDown) 1f else 0f)
                } else {
                    set("mouse", 0.5f, 0.5f)
                    set("mouseDown", 0f)
                }
                
                // Effect-specific uniforms
                effectData.uniforms.forEach { (name, value) ->
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
     * Start WebGPU render loop.
     */
    private fun startWebGPURenderLoop() {
        running = true
        renderLoop?.start()
        
        // Animation frame loop for GPU rendering.
        // The RenderLoop updates uniforms via its callback,
        // and EffectComposer manages the pass chain and GPU submission.
        fun animate() {
            if (!running) return
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
        effectComposer?.dispose()
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
            RendererType.WEBGPU -> effectComposer?.setSize(bufferWidth, bufferHeight)
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
