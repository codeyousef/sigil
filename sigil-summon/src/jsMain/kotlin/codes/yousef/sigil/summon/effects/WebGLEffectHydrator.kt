package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.SigilJson
import codes.yousef.sigil.schema.effects.*
import io.materia.renderer.webgl.WebGLEffectPass
import io.materia.renderer.webgl.WebGLEffectComposer
import io.materia.effects.BlendMode as MateriaBlendMode
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext

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
    private val renderLoop = RenderLoop()
    private val interactionHandler = InteractionHandler(
        canvas = canvas,
        config = interactions,
        normalizeCoordinates = true
    )
    
    // WebGL context and effect pipeline
    private var gl: WebGLRenderingContext? = null
    private var effectComposer: WebGLEffectComposer? = null
    private val effectPasses = mutableMapOf<String, WebGLEffectPass>()
    
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
                interactionHandler.setupMouseListeners()
            }
            interactionHandler.setupResizeObserver(canvas) { width, height ->
                resize(width.toInt(), height.toInt())
            }
            
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
        
        console.log("WebGLEffectHydrator: Creating effect pass for ${effectData.id}")
        console.log("WebGLEffectHydrator: GLSL shader length: ${glslShader.length}")
        
        val pass = WebGLEffectPass.create {
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
                float("deltaTime")
                
                // Effect-specific uniforms (skip standard uniforms to avoid duplicates)
                val standardUniforms = setOf("time", "resolution", "mouse", "mouseDown", "deltaTime")
                effectData.uniforms.entries
                    .filter { it.key !in standardUniforms }
                    .sortedBy { it.key }
                    .forEach { (name, value) ->
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
        
        console.log("WebGLEffectHydrator: Pass created, uniforms layout size: ${pass.effect.uniforms.layout.size}")
        pass.effect.uniforms.layout.forEach { field ->
            console.log("WebGLEffectHydrator:   Uniform '${field.name}' type=${field.type}")
        }
        
        return pass
    }
    
    private var debugLoggedOnce = false
    
    /**
     * Update all effects with current frame data.
     */
    private fun updateEffects(totalTimeSeconds: Double, deltaTimeSeconds: Double) {
        val totalTime = totalTimeSeconds.toFloat()
        val deltaTime = deltaTimeSeconds.toFloat()
        
        composerData.effects.forEach { effectData ->
            val pass = effectPasses[effectData.id] ?: return@forEach
            
            if (!debugLoggedOnce) {
                console.log("WebGLEffectHydrator: First updateEffects call")
                console.log("WebGLEffectHydrator: pass.effect.uniforms.layout.size = ${pass.effect.uniforms.layout.size}")
                console.log("WebGLEffectHydrator: pass.effect.uniformBuffer.size = ${pass.effect.uniformBuffer.size}")
                debugLoggedOnce = true
            }
            
            pass.updateUniforms {
                set("time", totalTime * effectData.timeScale)
                set("deltaTime", deltaTime)
                set("resolution", canvas.width.toFloat(), canvas.height.toFloat())
                
                if (effectData.enableMouseInteraction) {
                    set("mouse", interactionHandler.mouseX, interactionHandler.mouseY)
                    set("mouseDown", if (interactionHandler.isMouseDown) 1f else 0f)
                } else {
                    set("mouse", 0.5f, 0.5f)
                    set("mouseDown", 0f)
                }
                
                val standardUniforms = setOf("time", "deltaTime", "resolution", "mouse", "mouseDown")
                effectData.uniforms.entries
                    .filter { it.key !in standardUniforms }
                    .sortedBy { it.key }
                    .forEach { (name, value) ->
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
        renderLoop.start { totalTime, deltaTime ->
            updateEffects(totalTime, deltaTime)
            effectComposer?.render()
        }
        console.log("WebGLEffectHydrator: Render loop started with ${effectPasses.size} passes")
    }
    
    /**
     * Stop the render loop.
     */
    fun stop() {
        renderLoop.stop()
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
                    }
                } catch (e: Exception) {
                    console.error("WebGLEffect: Failed to hydrate $canvasId: ${e.message}")
                }
            }
        }
    }
}
