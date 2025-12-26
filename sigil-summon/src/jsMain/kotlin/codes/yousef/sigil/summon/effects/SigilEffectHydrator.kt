package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.effects.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement

private val scope = MainScope()

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
    private var rendererType: RendererType = RendererType.NONE
    
    // Renderers
    private var webGPURenderer: WebGPURenderer? = null
    private var webGLHydrator: WebGLEffectHydrator? = null
    
    // Interaction handler
    private val interactionHandler = InteractionHandler(canvas, interactions)
    
    /**
     * Initialize the effect hydrator.
     * Automatically detects browser capabilities and selects the best renderer.
     */
    suspend fun initialize(): Boolean {
        // Sync canvas buffer size with CSS display size
        syncCanvasSize()
        
        // Setup interaction listeners
        if (hasMouseInteraction()) {
            interactionHandler.setupMouseListeners()
        }
        
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
     * Initialize WebGPU rendering.
     */
    private suspend fun initializeWebGPU(): Boolean {
        webGPURenderer = WebGPURenderer(canvas, composerData, config, interactionHandler)
        if (webGPURenderer?.initialize() == true) {
            return true
        }
        return fallbackToWebGL()
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
     * Check if any effect requires mouse interaction.
     */
    private fun hasMouseInteraction(): Boolean {
        return composerData.effects.any { it.enableMouseInteraction } ||
               interactions.enableMouseMove
    }
    
    /**
     * Start the render loop.
     */
    fun startRenderLoop() {
        when (rendererType) {
            RendererType.WEBGPU -> webGPURenderer?.startRenderLoop()
            RendererType.WEBGL -> webGLHydrator?.startRenderLoop()
            RendererType.CSS_FALLBACK, RendererType.NONE -> {
                // No render loop needed for CSS fallback
            }
        }
    }
    
    /**
     * Stop the render loop.
     */
    fun stop() {
        webGPURenderer?.stopRenderLoop()
        webGLHydrator?.stop()
    }
    
    /**
     * Clean up resources.
     */
    fun dispose() {
        stop()
        interactionHandler.dispose()
        webGPURenderer = null
        webGLHydrator?.dispose()
        webGLHydrator = null
    }
    
    /**
     * Sync canvas buffer size with CSS display size.
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
            RendererType.WEBGPU -> webGPURenderer?.resize(bufferWidth.toDouble(), bufferHeight.toDouble())
            RendererType.WEBGL -> webGLHydrator?.resize(bufferWidth, bufferHeight)
            RendererType.CSS_FALLBACK, RendererType.NONE -> {}
        }
    }
    
    /**
     * Setup resize observer for this hydrator.
     */
    fun setupResizeObserver() {
        interactionHandler.setupResizeObserver(canvas) { width, height ->
            resize(width.toInt(), height.toInt())
        }
    }
    
    companion object {
        // Track hydrated canvases to prevent double initialization
        private val hydratedCanvases = mutableSetOf<String>()
        private val hydrationInProgress = mutableSetOf<String>()
        
        /**
         * Hydrate effects from DOM data.
         */
        fun hydrateFromDOM(canvasId: String, forceReinitialize: Boolean = false) {
            // SYNCHRONOUS checks BEFORE launching coroutine to prevent race conditions
            
            // Check if already fully hydrated
            if (canvasId in hydratedCanvases && !forceReinitialize) {
                console.warn("SigilEffect: Canvas $canvasId already hydrated, skipping (use forceReinitialize=true to reinitialize)")
                return
            }
            
            // Check if hydration is already in progress (prevents concurrent hydrations)
            if (canvasId in hydrationInProgress && !forceReinitialize) {
                console.warn("SigilEffect: Canvas $canvasId hydration already in progress, skipping")
                return
            }
            
            // Check DOM marker (for cross-script detection)
            val canvas = document.getElementById(canvasId) as? HTMLCanvasElement
            if (canvas != null && canvas.getAttribute("data-sigil-hydrated") == "true" && !forceReinitialize) {
                console.warn("SigilEffect: Canvas $canvasId already hydrated (DOM marker), skipping")
                hydratedCanvases.add(canvasId) // Sync our tracking
                return
            }
            
            // Handle force reinitialization
            if (forceReinitialize) {
                val existingHydrator = canvas?.asDynamic()?.__sigilHydrator as? SigilEffectHydrator
                if (existingHydrator != null) {
                    console.log("SigilEffect: Reinitializing $canvasId (hot reload)")
                    existingHydrator.dispose()
                }
                hydratedCanvases.remove(canvasId)
                hydrationInProgress.remove(canvasId)
                canvas?.removeAttribute("data-sigil-hydrated")
            }
            
            // Mark as in-progress SYNCHRONOUSLY before launching
            hydrationInProgress.add(canvasId)
            
            scope.launch {
                val canvasElement = document.getElementById(canvasId) as? HTMLCanvasElement
                
                if (canvasElement == null) {
                    console.error("SigilEffect: Canvas not found for $canvasId")
                    hydrationInProgress.remove(canvasId)
                    return@launch
                }
                
                // Get effect data from the data-sigil-effects attribute
                val effectJson = canvasElement.getAttribute("data-sigil-effects")
                    ?.replace("&#39;", "'")
                    ?.replace("&lt;", "<")
                    ?.replace("&gt;", ">")
                    ?.replace("&amp;", "&")
                
                if (effectJson == null || !effectJson.startsWith("{")) {
                    console.error("SigilEffect: No valid effect data found in data-sigil-effects attribute for $canvasId")
                    hydrationInProgress.remove(canvasId)
                    return@launch
                }
                
                val configJson = canvasElement.getAttribute("data-sigil-config")
                    ?.replace("&#39;", "'")
                    ?.replace("&lt;", "<")
                    ?.replace("&gt;", ">")
                    ?.replace("&amp;", "&") ?: "{}"
                val interactionsJson = canvasElement.getAttribute("data-sigil-interactions")
                    ?.replace("&#39;", "'")
                    ?.replace("&lt;", "<")
                    ?.replace("&gt;", ">")
                    ?.replace("&amp;", "&") ?: "{}"
                
                try {
                    val composerData = SigilJson.decodeFromString<EffectComposerData>(effectJson)
                    val config = SigilJson.decodeFromString<SigilCanvasConfig>(configJson)
                    val interactions = SigilJson.decodeFromString<InteractionConfig>(interactionsJson)
                    
                    val hydrator = SigilEffectHydrator(canvasElement, composerData, config, interactions)
                    if (hydrator.initialize()) {
                        hydrator.startRenderLoop()
                        
                        // Store hydrator reference on canvas for cleanup and hot reload
                        canvasElement.asDynamic().__sigilHydrator = hydrator
                        
                        // Mark as hydrated in DOM for cross-script detection
                        canvasElement.setAttribute("data-sigil-hydrated", "true")
                        
                        // Move from in-progress to completed
                        hydrationInProgress.remove(canvasId)
                        hydratedCanvases.add(canvasId)
                        
                        // Setup resize observer
                        hydrator.setupResizeObserver()
                        
                    } else {
                        hydrationInProgress.remove(canvasId)
                    }
                } catch (e: Exception) {
                    console.error("SigilEffect: Failed to hydrate $canvasId: ${e.message}")
                    hydrationInProgress.remove(canvasId)
                }
            }
        }
        
        /**
         * Get the hydrator instance for a canvas, if it exists.
         */
        fun getHydrator(canvasId: String): SigilEffectHydrator? {
            val canvas = document.getElementById(canvasId) as? HTMLCanvasElement ?: return null
            return canvas.asDynamic().__sigilHydrator as? SigilEffectHydrator
        }
        
        /**
         * Dispose the hydrator for a canvas and clean up resources.
         */
        fun disposeHydrator(canvasId: String) {
            val canvas = document.getElementById(canvasId) as? HTMLCanvasElement ?: return
            val hydrator = canvas.asDynamic().__sigilHydrator as? SigilEffectHydrator
            hydrator?.dispose()
            canvas.asDynamic().__sigilHydrator = null
            canvas.removeAttribute("data-sigil-hydrated")
            hydratedCanvases.remove(canvasId)
            hydrationInProgress.remove(canvasId)
        }
        
        /**
         * Check if a canvas is currently hydrated or hydration is in progress.
         */
        fun isHydrated(canvasId: String): Boolean {
            return canvasId in hydratedCanvases
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
    /**
     * Hydrate a canvas with effect data from its data-sigil-effects attribute.
     */
    @JsName("hydrate")
    fun hydrate(canvasId: String) {
        SigilEffectHydrator.hydrateFromDOM(canvasId)
    }
    
    /**
     * Hydrate a canvas, optionally forcing reinitialization (for hot reload).
     * 
     * @param canvasId The ID of the canvas element to hydrate
     * @param forceReinitialize If true, disposes existing hydrator and reinitializes
     */
    @JsName("hydrateWithOptions")
    fun hydrateWithOptions(canvasId: String, forceReinitialize: Boolean) {
        SigilEffectHydrator.hydrateFromDOM(canvasId, forceReinitialize)
    }
    
    /**
     * Dispose the hydrator for a canvas and clean up all GPU resources.
     */
    @JsName("dispose")
    fun dispose(canvasId: String) {
        SigilEffectHydrator.disposeHydrator(canvasId)
    }
    
    /**
     * Check if a canvas is currently hydrated.
     */
    @JsName("isHydrated")
    fun isHydrated(canvasId: String): Boolean {
        return SigilEffectHydrator.isHydrated(canvasId)
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
