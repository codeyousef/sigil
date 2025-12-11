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
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.MouseEvent

private val scope = MainScope()

/**
 * Hydrator class for screen-space effects.
 * Creates and manages WebGPU shader passes from serialized effect data.
 * 
 * Uses Materia's EffectComposer and FullScreenEffectPass for rendering.
 */
class SigilEffectHydrator(
    private val canvas: HTMLCanvasElement,
    private val composerData: EffectComposerData,
    private val config: SigilCanvasConfig,
    private val interactions: InteractionConfig
) {
    private var running = false
    private var animationFrameId: Int = 0
    
    // Materia effect pipeline components
    private lateinit var effectComposer: EffectComposer
    private val effectPasses = mutableMapOf<String, FullScreenEffectPass>()
    private lateinit var renderLoop: RenderLoop
    
    // Interaction state
    private var mouseX = 0f
    private var mouseY = 0f
    private var isMouseDown = false
    
    /**
     * Initialize the effect hydrator.
     * Creates Materia FullScreenEffectPass instances for each shader effect.
     */
    suspend fun initialize(): Boolean {
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
                    effectComposer.addPass(pass)
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
            
            console.log("SigilEffectHydrator: Initialized with ${effectPasses.size} effect passes")
            return true
        } catch (e: Exception) {
            console.error("SigilEffectHydrator: Failed to initialize: ${e.message}")
            return false
        }
    }
    
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
        running = true
        renderLoop.start()
        
        // Animation frame loop for GPU rendering.
        // The RenderLoop updates uniforms via its callback,
        // and EffectComposer manages the pass chain and GPU submission.
        fun animate() {
            if (!running) return
            animationFrameId = window.requestAnimationFrame { animate() }
        }
        
        animate()
        console.log("SigilEffectHydrator: Render loop started with ${effectPasses.size} passes")
    }
    
    /**
     * Stop the render loop.
     */
    fun stop() {
        running = false
        renderLoop.stop()
        
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
        effectComposer.dispose()
        effectPasses.clear()
    }
    
    /**
     * Handle canvas resize.
     */
    fun resize(width: Int, height: Int) {
        effectComposer.setSize(width, height)
    }
    
    companion object {
        /**
         * Hydrate effects from DOM data.
         * Called by the hydration script embedded in the HTML.
         */
        fun hydrateFromDOM(canvasId: String) {
            scope.launch {
                val canvas = document.getElementById(canvasId) as? HTMLCanvasElement
                val dataElement = document.getElementById("$canvasId-effects")
                
                if (canvas == null || dataElement == null) {
                    console.error("SigilEffect: Canvas or data element not found for $canvasId")
                    return@launch
                }
                
                val effectJson = dataElement.textContent ?: return@launch
                val configJson = canvas.getAttribute("data-sigil-config")?.replace("\\'", "'") ?: "{}"
                val interactionsJson = canvas.getAttribute("data-sigil-interactions")?.replace("\\'", "'") ?: "{}"
                
                try {
                    val composerData = SigilJson.decodeFromString<EffectComposerData>(effectJson)
                    val config = SigilJson.decodeFromString<SigilCanvasConfig>(configJson)
                    val interactions = SigilJson.decodeFromString<InteractionConfig>(interactionsJson)
                    
                    val hydrator = SigilEffectHydrator(canvas, composerData, config, interactions)
                    if (hydrator.initialize()) {
                        hydrator.startRenderLoop()
                        
                        // Setup resize observer
                        val resizeObserver = js("new ResizeObserver(function(entries) { entries.forEach(function(entry) { hydrator.resize(entry.contentRect.width, entry.contentRect.height); }); })")
                        resizeObserver.observe(canvas)
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
}
