package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.effects.*
import io.materia.renderer.webgl.WebGLEffectPass
import io.materia.renderer.webgl.WebGLEffectComposer
import io.materia.effects.BlendMode as MateriaBlendMode
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext
import org.w3c.dom.events.MouseEvent

private val scope = MainScope()

/**
 * WebGL-based hydrator for screen-space effects.
 * 
 * This is the fallback renderer for browsers that don't support WebGPU (e.g., Firefox).
 * Uses Materia's WebGL effect system with GLSL shaders.
 */
class WebGLEffectHydrator(
    private val canvas: HTMLCanvasElement,
    private val composerData: EffectComposerData,
    private val config: SigilCanvasConfig,
    private val interactions: InteractionConfig
) {
    private var running = false
    private var animationFrameId: Int = 0
    
    // WebGL context and effect pipeline
    private var gl: WebGLRenderingContext? = null
    private var effectComposer: WebGLEffectComposer? = null
    private val effectPasses = mutableMapOf<String, WebGLEffectPass>()
    
    // Time tracking
    private var startTime: Double = 0.0
    private var lastFrameTime: Double = 0.0
    
    // Interaction state
    private var mouseX = 0f
    private var mouseY = 0f
    private var isMouseDown = false
    
    /**
     * Initialize the WebGL effect hydrator.
     */
    fun initialize(): Boolean {
        try {
            // Get WebGL context
            gl = canvas.getContext("webgl") as? WebGLRenderingContext
                ?: canvas.getContext("experimental-webgl") as? WebGLRenderingContext
            
            val glContext = gl
            if (glContext == null) {
                console.error("WebGLEffectHydrator: WebGL not supported")
                return false
            }
            
            // Create WebGL EffectComposer
            effectComposer = WebGLEffectComposer(
                gl = glContext,
                width = canvas.width,
                height = canvas.height
            )
            
            // Create WebGLEffectPass for each effect that has GLSL shader
            var passCount = 0
            composerData.effects.forEach { effectData ->
                if (effectData.enabled && effectData.glslFragmentShader != null) {
                    val pass = createEffectPass(effectData)
                    if (pass != null) {
                        effectPasses[effectData.id] = pass
                        effectComposer?.addPass(pass)
                        passCount++
                    }
                }
            }
            
            if (passCount == 0) {
                console.warn("WebGLEffectHydrator: No effects with GLSL shaders found")
                return false
            }
            
            // Setup interaction listeners
            if (hasMouseInteraction()) {
                setupMouseListeners()
            }
            
            startTime = window.performance.now()
            lastFrameTime = startTime
            
            console.log("WebGLEffectHydrator: Initialized with $passCount effect passes")
            return true
        } catch (e: Exception) {
            console.error("WebGLEffectHydrator: Failed to initialize: ${e.message}")
            return false
        }
    }
    
    /**
     * Create a WebGLEffectPass from shader effect data.
     */
    private fun createEffectPass(effectData: ShaderEffectData): WebGLEffectPass? {
        val glslShader = effectData.glslFragmentShader ?: return null
        
        return WebGLEffectPass.create {
            fragmentShader = glslShader
            
            // Map blend modes
            blendMode = when (effectData.blendMode) {
                BlendMode.NORMAL -> MateriaBlendMode.ALPHA_BLEND
                BlendMode.ADDITIVE -> MateriaBlendMode.ADDITIVE
                BlendMode.MULTIPLY -> MateriaBlendMode.MULTIPLY
                BlendMode.SCREEN -> MateriaBlendMode.SCREEN
                BlendMode.OVERLAY -> MateriaBlendMode.OVERLAY
            }
            
            // Build uniforms
            uniforms {
                // Standard uniforms
                float("time")
                vec2("resolution")
                vec2("mouse")
                float("mouseDown")
                
                // Effect-specific uniforms
                effectData.uniforms.forEach { (name, value) ->
                    when (value) {
                        is UniformValue.FloatValue -> float(name)
                        is UniformValue.IntValue -> float(name)
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
     * Update all effects with current frame data.
     */
    private fun updateEffects(currentTime: Double) {
        val totalTime = ((currentTime - startTime) / 1000.0).toFloat()
        val deltaTime = ((currentTime - lastFrameTime) / 1000.0).toFloat()
        lastFrameTime = currentTime
        
        composerData.effects.forEach { effectData ->
            val pass = effectPasses[effectData.id] ?: return@forEach
            
            pass.updateUniforms {
                set("time", totalTime * effectData.timeScale)
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
     * Setup mouse event listeners.
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
        running = true
        
        fun animate(currentTime: Double) {
            if (!running) return
            
            updateEffects(currentTime)
            effectComposer?.render()
            
            animationFrameId = window.requestAnimationFrame { animate(it) }
        }
        
        animationFrameId = window.requestAnimationFrame { animate(it) }
        console.log("WebGLEffectHydrator: Render loop started with ${effectPasses.size} passes")
    }
    
    /**
     * Stop the render loop.
     */
    fun stop() {
        running = false
        
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
        gl = null
    }
    
    /**
     * Handle canvas resize.
     */
    fun resize(width: Int, height: Int) {
        canvas.width = width
        canvas.height = height
        gl?.viewport(0, 0, width, height)
        effectComposer?.setSize(width, height)
    }
    
    companion object {
        /**
         * Check if WebGL is available.
         */
        fun isWebGLAvailable(): Boolean {
            return try {
                val testCanvas = document.createElement("canvas") as HTMLCanvasElement
                val gl = testCanvas.getContext("webgl") 
                    ?: testCanvas.getContext("experimental-webgl")
                gl != null
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Hydrate effects from DOM data using WebGL.
         * 
         * Reads effect data from the data-sigil-effects attribute on the canvas.
         */
        fun hydrateFromDOM(canvasId: String) {
            scope.launch {
                val canvas = document.getElementById(canvasId) as? HTMLCanvasElement
                
                if (canvas == null) {
                    console.error("WebGLEffect: Canvas not found for $canvasId")
                    return@launch
                }
                
                // Get effect data from the data-sigil-effects attribute
                val effectJson = canvas.getAttribute("data-sigil-effects")
                    ?.replace("&#39;", "'")
                    ?.replace("&lt;", "<")
                    ?.replace("&gt;", ">")
                    ?.replace("&amp;", "&")
                
                if (effectJson == null || !effectJson.startsWith("{")) {
                    console.error("WebGLEffect: No valid effect data found in data-sigil-effects attribute for $canvasId")
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
                    
                    val hydrator = WebGLEffectHydrator(canvas, composerData, config, interactions)
                    if (hydrator.initialize()) {
                        hydrator.startRenderLoop()
                        
                        // Setup resize observer
                        val resizeObserver = js("new ResizeObserver(function(entries) { entries.forEach(function(entry) { hydrator.resize(entry.contentRect.width, entry.contentRect.height); }); })")
                        resizeObserver.observe(canvas)
                    }
                } catch (e: Exception) {
                    console.error("WebGLEffect: Failed to hydrate $canvasId: ${e.message}")
                }
            }
        }
    }
}
