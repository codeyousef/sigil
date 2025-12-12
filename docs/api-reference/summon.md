# Summon Core API Reference

The `sigil-summon` module is the core engine (or "summoner") responsible for interpreting the scene graph and managing the rendering lifecycle. It also provides SSR (Server-Side Rendering) support for screen-space effects.

## Screen-Space Effects (SSR)

### `SigilEffectCanvas`

The primary composable for rendering screen-space shader effects with SSR support.

```kotlin
@Composable
fun SigilEffectCanvas(
    id: String = "sigil-effect-canvas",
    width: String = "100%",
    height: String = "100%",
    config: SigilCanvasConfig = SigilCanvasConfig(),
    interactions: InteractionConfig = InteractionConfig(),
    fallback: @Composable () -> String = { "" },
    content: @Composable () -> String
): String
```

**Parameters:**
- `id`: Unique ID for the canvas element
- `width`: CSS width of the canvas
- `height`: CSS height of the canvas
- `config`: Canvas configuration (device pixel ratio, power preference, fallbacks)
- `interactions`: Interaction configuration (mouse tracking, etc.)
- `fallback`: Composable to render for noscript/no-WebGPU fallback
- `content`: Composable lambda containing `SigilEffect()` calls

**Server Output:**
On the JVM, renders an HTML canvas with effect data embedded in the `data-sigil-effects` attribute as JSON.

### `SigilEffect`

Registers a shader effect within a `SigilEffectCanvas`.

```kotlin
@Composable
fun SigilEffect(effect: ShaderEffectData): String
```

### `CustomShaderEffect`

Convenience function to create and register a custom shader effect.

```kotlin
@Composable
fun CustomShaderEffect(
    id: String,
    fragmentShader: String,           // WGSL shader code
    name: String? = null,
    timeScale: Float = 1f,
    enableMouseInteraction: Boolean = false,
    uniforms: Map<String, UniformValue> = emptyMap()
): String
```

### `EffectSummonContext`

Context registry for Sigil effect composition. Tracks registered effects during composition for serialization (server) or hydration (client).

```kotlin
class EffectSummonContext {
    val effects: List<ShaderEffectData>    // Collected effects
    val canvasConfig: SigilCanvasConfig
    val interactionConfig: InteractionConfig
    
    fun registerEffect(effect: ShaderEffectData)
    fun buildComposerData(id: String): EffectComposerData
    
    companion object {
        fun current(): EffectSummonContext
        fun createServerContext(): EffectSummonContext
        fun createClientContext(): EffectSummonContext
        fun <R> withContext(context: EffectSummonContext, block: () -> R): R
    }
}
```

---

## 3D Scene Components

### `SigilSummonContext`

A context class that holds the current state of the engine, including the active scene, renderer, and camera.

### `MateriaCanvas` (Core)

The platform-agnostic implementation of the canvas handling logic. It bridges the gap between the abstract scene graph and the platform-specific graphics context (JVM AWT, Android Surface, WebGL/WebGPU).

---

## Client-Side Hydration

### `SigilEffectHydrator`

Handles client-side hydration of effect canvases rendered on the server.

```kotlin
object SigilEffectHydratorJs {
    fun hydrate(canvasId: String)
    fun isWebGPUAvailable(): Boolean
    fun isWebGLAvailable(): Boolean
    fun getAvailableRenderer(): String  // "webgpu" | "webgl" | "css"
}
```

The hydrator:
1. Reads effect data from the `data-sigil-effects` attribute
2. Parses config from `data-sigil-config` and `data-sigil-interactions`
3. Creates WebGPU effect passes (or WebGL fallback)
4. Starts the render loop with resize handling

---

## Logic

Sigil uses a "Summon" concept where nodes are summoned into existence.
- **Components**: Functions that create and configure specific types of nodes (Geometry, Lights, Camera).
- **Context**: Manages the dependency injection and state for the rendering pipeline.
