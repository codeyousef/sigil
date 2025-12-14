# Changelog

## [0.2.8.11] - 2025-12-14

### Fixed

- **Time uniform not updating in WebGPU render loop**: Fixed animation by unifying the render loop
  - Previously had TWO separate `requestAnimationFrame` loops: Materia's `RenderLoop` for uniform updates and our `animate()` for rendering
  - Uniform updates and rendering were desynchronized, causing `uniforms.time` to stay at 0
  - Now uses a single unified render loop that:
    1. Calculates elapsed time from `requestAnimationFrame` timestamp
    2. Updates uniforms with current time/deltaTime BEFORE rendering
    3. Renders the frame
  - Removed unused `RenderLoop` import and field

## [0.2.8.10] - 2025-12-14

### Fixed

- **Race condition in hydration guards**: Fixed race condition where concurrent `hydrateFromDOM()` calls could both pass guards and create two GPUDevice instances
  - Moved all guard checks to run **synchronously** before `scope.launch`
  - Added `hydrationInProgress` set to block concurrent hydrations immediately
  - The `hydratedCanvases` set now tracks completed hydrations; `hydrationInProgress` tracks in-flight ones

### Added

- **Hydration guard unit tests**: New `HydrationGuardLogicTest` with 9 tests verifying:
  - Single and double hydration blocking
  - Race condition prevention (concurrent calls blocked by in-progress check)
  - Force reinitialization bypass
  - Failed hydration cleanup and retry
  - Multiple canvas independence

## [0.2.8.9] - 2025-12-14

### Fixed

- **WebGPU device mismatch error**: Fixed "TextureView of Texture is associated with [Device]" error caused by double initialization creating two separate GPUDevice instances
  - Added guard to prevent `hydrateFromDOM()` from initializing the same canvas twice
  - Canvas IDs are now tracked in a set; DOM marker `data-sigil-hydrated` provides cross-script detection
  - Hydrator reference stored on canvas element (`canvas.__sigilHydrator`) for cleanup

### Added

- **Hot reload support**: New `forceReinitialize` parameter allows re-hydrating a canvas (disposes existing hydrator first)
- **Cleanup API**: New methods exposed to JavaScript:
  - `SigilEffectHydrator.hydrateWithOptions(canvasId, forceReinitialize)` - Hydrate with hot reload option
  - `SigilEffectHydrator.dispose(canvasId)` - Clean up GPU resources
  - `SigilEffectHydrator.isHydrated(canvasId)` - Check hydration status

## [0.2.8.8] - 2025-12-14

### Fixed

- **WebGPU context configuration**: Fixed `asDynamic is not a function` error by properly casting the WebGPU canvas context to `dynamic` before calling `configure()`

## [0.2.8.7] - 2025-12-13

### Fixed

- **WebGPU rendering now works**: Updated to Materia 0.3.4.3 which removes redundant `.asDynamic()` calls on `js({})` results that were causing runtime errors

## [0.2.8.6] - 2025-12-13

### Fixed

- **Rebuilt JS bundle**: Ensured the compiled `sigil-hydration.js` bundle includes the fix from 0.2.8.5

## [0.2.8.5] - 2025-12-13

### Fixed

- **Removed asDynamic() workaround**: Now that Materia 0.3.4.2 exports `render()` via `@JsExport`, removed the `asDynamic()` call that was causing `asDynamic is not a function` error

## [0.2.8.4] - 2025-12-13

### Fixed

- **WebGPU effect rendering**: Updated to Materia 0.3.4.2 which adds `@JsExport` to `WebGPUEffectComposer`, fixing the `i.render is not a function` error caused by Kotlin/JS IR backend name mangling

## [0.2.8.3] - 2025-12-13

### Fixed

- **WebGPU effect rendering**: Updated to Materia 0.3.4.1 which fixes the `WebGPUEffectComposer.render()` method (0.3.4.0 had broken ping-pong texture logic)

## [0.2.8.2] - 2025-12-13

### Fixed

- **WebGPU render loop dynamic dispatch**: Fixed `asDynamic is not a function` error by using `asDynamic()` on the composer object rather than type-casting the texture view parameter

## [0.2.8.1] - 2025-12-13

### Fixed

- **WebGPU render loop type casting**: Fixed `asDynamic is not a function` error in WebGPU render loop by properly casting `GPUTextureView` for `WebGPUEffectComposer.render()`

## [0.2.8.0] - 2025-12-13

### Added

- **WebGPU Effect Rendering**: Now uses Materia's new `WebGPUEffectComposer` for native WebGPU fullscreen effects!
  - WGSL shaders (`fragmentShader`) are now rendered directly via WebGPU when available
  - Automatic fallback to WebGL with GLSL shaders when WebGPU unavailable
  - Ping-pong texture system for multi-pass rendering
  - Full blend mode support

### Changed

- **Renderer Detection**: Now prefers WebGPU over WebGL when browser supports it
- Updated to Materia 0.3.4.0 (adds `WebGPUEffectComposer`)

### Usage

```kotlin
// WebGPU-first with WebGL fallback
CustomShaderEffect(
    id = "aurora",
    fragmentShader = wgslShaderCode,        // Used by WebGPU (preferred)
    glslFragmentShader = glslShaderCode,    // Used by WebGL (fallback for Firefox)
    uniforms = mapOf(...)
)

// WebGPU-only (no fallback for browsers without WebGPU)
CustomShaderEffect(
    id = "aurora",
    fragmentShader = wgslShaderCode,
    uniforms = mapOf(...)
)
```

## [0.2.7.12] - 2025-12-13

### Fixed

- **`CustomShaderEffect()` now accepts `glslFragmentShader` parameter**: The helper function was missing the GLSL shader parameter. Since Sigil uses WebGL for effects (Materia lacks `WebGPUEffectComposer`), effects need `glslFragmentShader` to render.

### Important: Effects Require GLSL Shaders

The WebGPU effect path was **never functional** - it created passes but never called `render()`. Materia's `EffectComposer` (WebGPU) is just a pass manager without rendering. Only `WebGLEffectComposer` has a `render()` method.

**To make effects work**, you must provide a GLSL shader:

```kotlin
CustomShaderEffect(
    id = "aurora",
    fragmentShader = wgslShaderCode,        // WGSL (stored for future WebGPU support)
    glslFragmentShader = glslShaderCode,    // GLSL (required for current WebGL rendering)
    uniforms = mapOf(...)
)
```

### Changed

- Improved console logging: now warns when effects have WGSL but no GLSL fallback

## [0.2.7.11] - 2025-12-13

### Changed

- **Effects now use WebGL exclusively**: Materia 0.3.3.1 clarified that only `WebGLEffectComposer` has a `render()` method. The base `EffectComposer` is just a pass manager. Sigil now always uses WebGL for fullscreen effects regardless of WebGPU availability.

### Fixed

- Effects now actually render! The previous "WebGPU" path was non-functional because `EffectComposer.render()` doesn't exist.
- Updated documentation to reflect Materia's effect system architecture.

## [0.2.7.10] - 2025-12-12

### Fixed

- Improved WGSL uniform struct parser to support any uniform variable name (not just `uniforms`) and handle compact/minified shader formatting.
- Fixed uniform type mapping for `i32`/`u32` types in WebGPU shaders.

## [0.2.7.9] - 2025-12-12

### Fixed

- Fixed a browser crash in the shipped `sigil-hydration.js` (`SyntaxError: invalid regexp group`) by removing JS-incompatible regex usage in WGSL parsing.

## [0.2.7.8] - 2025-12-12

### Fixed

- WebGPU effect uniforms now follow the WGSL uniform struct field order (single `@group(0) @binding(0)` uniform buffer), fixing custom uniforms reading as `0`.
- Improved runtime error surfacing for WebGPU per-frame uniform updates.

---

## [0.2.7.7] - 2025-12-12

### Fixed

- SSR inline hydration loader now reads effect/config/interaction JSON from `data-*` attributes (no legacy `"$canvasId-effects"` script element).
- WebGPU custom uniform declarations are ordered to match WGSL `@binding(n)` indices to ensure bindings 1..N are wired correctly.

---

## [0.2.7.6] - 2025-12-12

### Fixed

#### data-sigil-effects Attribute Content
- **Fixed**: `data-sigil-effects` attribute now contains the actual serialized `EffectComposerData` JSON instead of just `"true"`
- The attribute now includes the full effect data: shader code (WGSL/GLSL), uniforms, blend modes, etc.
- Example: `data-sigil-effects='{"id":"canvas-id","effects":[{"id":"aurora-effect",...}]}'`
- Updated both `SigilEffectHydrator` and `WebGLEffectHydrator` to read effect data from the attribute
- Fixed HTML entity decoding (`&#39;`, `&lt;`, `&gt;`, `&amp;`) when reading from attributes
- Removed legacy `<script id="$canvasId-effects">` element - no longer needed

---

## [0.2.7.5] - 2025-12-12

### Fixed

#### Auto-Hydration Attribute Mismatch
- Fixed canvas not being found by auto-hydration
- Changed `data-sigil-effect="true"` to `data-sigil-effects="true"` (plural)
- Auto-hydration looks for `canvas[data-sigil-effects]` selector
- Now auto-hydration correctly finds and hydrates effect canvases

---

## [0.2.7.4] - 2025-12-12

### Fixed

#### Canvas Buffer Size Sync
- Fixed invisible/distorted rendering caused by canvas buffer size mismatch
- Canvas buffer size (default 300x150) is now synced with CSS display size before WebGPU/WebGL initialization
- Added `syncCanvasSize()` that uses `getBoundingClientRect()` and `devicePixelRatio` for proper HiDPI support
- Updated `resize()` to also sync buffer size with DPR scaling

---

## [0.2.7.3] - 2025-12-12

### Fixed

#### ResizeObserver Callback in Hydrator
- Fixed `TypeError: can't access property, this is undefined` in ResizeObserver callback
- Replaced inline `js()` call with proper Kotlin external class declaration for `ResizeObserver`
- The Kotlin callback is now properly captured and invoked when the canvas resizes

---

## [0.2.7.2] - 2025-12-12

### Fixed

#### Proper JSON Escaping for Different HTML Contexts
- Fixed "expected property name or '}'" JSON parse error
- Script tag content (`<script type="application/json">`) now uses minimal escaping (only `</` and `<!--`)
- HTML attributes (`data-sigil-config='...'`) use HTML entity escaping (`&#39;`, `&lt;`, etc.)
- Previously, double quotes were being escaped to `&quot;` inside script tags which broke JSON parsing

---

## [0.2.7.1] - 2025-12-12

### Fixed

#### JSON Parsing in Hydration Script
- Fixed `SyntaxError: JSON.parse: bad escaped character` error
- Changed JSON escaping for HTML attributes to use HTML entities (`&#39;`, `&quot;`, etc.)
- Browser automatically decodes HTML entities, so no JS unescaping needed
- Removed broken `.replace(/\\'/g, "'")` calls from hydration script

---

## [0.2.7.0] - 2025-12-11

### Added

#### Auto-Loading Hydration Bundle
- **Zero-config client setup**: The embedded hydration script now automatically loads `/sigil-hydration.js`
- End users no longer need to manually add `<script>` tags - just call `sigilStaticAssets()` in Ktor routing
- The hydration script:
  - Checks if `SigilEffectHydrator`/`SigilHydrator` is already loaded
  - If not, dynamically injects a script tag to load the bundle
  - Waits for the bundle to load before hydrating
  - Handles errors gracefully with console logging

### Usage
```kotlin
// Server setup - just add this once:
routing {
    sigilStaticAssets()  // Serves /sigil-hydration.js
    // ... your routes
}

// That's it! SigilEffectCanvas output auto-loads the bundle
```

---

## [0.2.6.0] - 2025-12-11

### Fixed

#### SSR Canvas Rendering - Works in Both Contexts
- **`SigilEffectCanvas.jvm.kt` and `MateriaCanvas.jvm.kt`** now work in both scenarios:
  - **Summon SSR pipeline**: Calls `RawHtml(html)` to inject into renderer (wrapped in try-catch)
  - **Manual/direct use**: Returns HTML string for caller to inject (e.g., kotlinx.html `unsafe { raw() }`)
- This dual approach ensures canvas renders whether used through Summon's composable tree or called directly

---

## [0.2.5.1] - 2025-12-11 [YANKED]

### Fixed

#### SSR HTML Output
- **Fixed `SigilEffectCanvas.jvm.kt` and `MateriaCanvas.jvm.kt`**: Removed non-functional `RawHtml()` calls
  - `RawHtml()` only works within Summon's SSR rendering pipeline with an active `LocalPlatformRenderer`
  - When called directly (as in typical SSR usage), the renderer is not available
  - The functions now simply return the HTML string as intended
  - Callers inject the HTML using their framework's method (e.g., kotlinx.html's `unsafe { raw(html) }`)

### Technical Details
- The JVM implementations return HTML strings that callers must inject into the page
- This matches the pattern used in `sample-summon/Server.kt`
- Removed unused `RawHtml` import from both files

---

## [0.2.5.0] - 2025-12-11 [YANKED]

### Note
This version incorrectly added `RawHtml()` calls that don't function outside Summon's rendering pipeline.
Use 0.2.5.1 instead.

---

## [0.2.4.0] - 2025-12-11

### Changed

#### Ktor 3.x Compatibility
- **Upgraded Ktor from 2.3.12 to 3.0.3** for compatibility with modern Ktor applications
- Updated `SigilKtorIntegration` to use Ktor 3.x API:
  - Changed from `ApplicationCall` receiver to `RoutingContext` receiver
  - Routes now use `RoutingContext.call` instead of implicit `call`
- No breaking changes for library users - `sigilStaticAssets()` API unchanged

---

## [0.2.3.0] - 2025-12-11

### Fixed

#### Complete Hydration Bundle
- **Compiled JS hydration bundle**: `sigil-hydration.js` now contains the real compiled Kotlin/JS code
  - Previously was a placeholder that only logged messages to console
  - Now includes full `SigilEffectHydrator` with WebGPU and WebGL support
  - Bundle includes all dependencies: Materia engine, kotlinx-coroutines, kotlinx-serialization
  - Automatically exposes `window.SigilEffectHydrator` for manual hydration
  - Auto-hydrates all canvases with `data-sigil-effects` attribute on DOMContentLoaded

### Added

#### Multi-Framework Server Integration
- **`SigilAssets`**: Core asset loading utility shared across all framework integrations
  - Caching for raw and gzip-compressed assets
  - Loads assets from JAR resources with classloader fallbacks
- **`SigilSpringIntegration`**: Spring Boot / Spring MVC integration
  - `serveHydrationJs()` and `serveHydrationJsMap()` for easy controller setup
  - Works with `HttpServletRequest` / `HttpServletResponse`
- **`SigilQuarkusIntegration`**: Quarkus / JAX-RS integration
  - Servlet-based and reactive endpoint support
  - `buildResponseBytes()` for manual Response building
- Refactored `SigilKtorIntegration` to use shared `SigilAssets`

### Technical Details
- Added `binaries.executable()` to JS target for webpack bundling
- New `Main.kt` entry point exports hydration API to window global
- Gradle task `copyJsBundleToJvmResources` copies compiled bundle to JVM resources
- JVM JAR now includes ~640KB compiled JS bundle (minified)
- Added Jakarta Servlet API 6.0 as compileOnly dependency

---

## [0.2.2.0] - 2025-12-11

### Added

#### Ktor Integration for Static Assets
- **`SigilKtorIntegration`**: Ktor route extension for serving Sigil hydration assets
  - `Route.sigilStaticAssets()` - Serves `/sigil-hydration.js` from library JAR
  - `ApplicationCall.respondSigilAsset()` - Low-level asset serving function
  - Automatic gzip compression for compatible clients
  - Cache headers for optimal performance (1 year immutable)
  - Zero-configuration: JavaScript embedded in JAR, no external files needed
- **Static hydration script**: `sigil-hydration.js` for client-side effect initialization
  - Detects and logs Sigil effect canvases with `data-sigil-effects` attribute
  - Exports `window.Sigil` object for external access

### Technical Details
- New `SigilKtorIntegration.kt` in `sigil-summon` jvmMain
- Static resource at `static/sigil-hydration.js` embedded in JVM JAR
- Ktor server-core as compileOnly dependency

---

## [0.2.1.0] - 2025-12-11

### Added

#### WebGL Effect Fallback System
- **`GLSLLib`**: Complete GLSL shader snippet library mirroring WGSLLib for WebGL compatibility
  - **Hash**: `HASH_21`, `HASH_22`, `HASH_31`, `HASH_33` - pseudo-random functions
  - **Noise**: `VALUE_2D`, `PERLIN_2D`, `SIMPLEX_2D`, `WORLEY_2D` - procedural noise
  - **Fractal**: `FBM`, `TURBULENCE`, `RIDGED` - multi-octave noise
  - **Color**: `COSINE_PALETTE`, `HSV_TO_RGB`, `RGB_TO_HSV`, `HSL_TO_RGB`, `SRGB_TO_LINEAR`, `LINEAR_TO_SRGB`, `GRAYSCALE`
  - **Math**: `REMAP`, `SMOOTHSTEP_CUBIC`, `SMOOTHSTEP_QUINTIC`, `ROTATION_2D`, `PI`, `TAU`
  - **SDF**: `CIRCLE`, `BOX`, `ROUNDED_BOX`, `LINE`, `TRIANGLE`, `RING`, `SMOOTH_MIN`
  - **UV**: `CENTER`, `ROTATE`, `SCALE`, `TILE`
  - **Effects**: `VIGNETTE`, `FILM_GRAIN`, `CHROMATIC_ABERRATION`, `SCANLINES`, `CRT_CURVATURE`, `BARREL_DISTORTION`
  - **Presets**: `FRAGMENT_HEADER`, `FRAGMENT_HEADER_WITH_UNIFORMS`, `SIMPLE_GRADIENT`, `ANIMATED_NOISE`
- **`WebGLEffectHydrator`**: WebGL-based effect renderer for browsers without WebGPU support (Firefox)
  - Uses Materia's new WebGL effect system (`WebGLEffectComposer`, `WebGLEffectPass`)
  - Same API and capabilities as WebGPU renderer with GLSL shaders
- **Automatic renderer detection**: `SigilEffectHydrator` now auto-detects browser capabilities
  - Prefers WebGPU when available (Chrome, Edge)
  - Falls back to WebGL when GLSL shaders provided (Firefox)
  - Falls back to CSS when neither GPU API available

### Changed
- **`ShaderEffectData`**: Added optional `glslFragmentShader` and `glslVertexShader` fields for WebGL fallback
- **`SigilCanvasConfig`**: Added `fallbackToWebGL` option (default: true), renamed `fallbackToCSS` for clarity
- **`SigilEffectHydrator`**: Refactored to support multiple renderer backends
  - New `RendererType` enum: `WEBGPU`, `WEBGL`, `CSS_FALLBACK`, `NONE`
  - New `getRendererType()` method to query active renderer
- **`SigilEffectHydratorJs`**: Added JS-exported utility methods
  - `isWebGPUAvailable()`: Check WebGPU browser support
  - `isWebGLAvailable()`: Check WebGL browser support
  - `getAvailableRenderer()`: Get best available renderer type

### Technical Details
- 25+ new unit tests for GLSLLib
- Updated to Materia 0.3.3.0 with WebGL effect system
- Firefox and Safari compatibility via GLSL shaders

---

## [0.2.0.0] - 2025-12-11

### Added

#### Screen-Space Effect System
- **`ScreenSpaceEffectData`**: Serializable data types for fullscreen shader effects
- **`ShaderEffectData`**: ConfiguraVtion for individual shader passes with WGSL fragment shaders
- **`EffectComposerData`**: Multi-pass effect pipeline configuration
- **`SigilCanvasConfig`**: Canvas configuration including resolution and device pixel ratio
- **`UniformValue`**: Sealed class supporting Float, Int, Vec2, Vec3, Vec4, Mat3, and Mat4 uniforms
- **`BlendMode`**: NORMAL, ADDITIVE, MULTIPLY, SCREEN, and OVERLAY blend modes
- **`InteractionConfig`**: Mouse and touch interaction configuration
- **`WGSLLib`**: Reusable WGSL shader snippets (noise, hash, fractal, easing, color utilities)

#### Built-in Effects
- **7 ready-to-use effects**: Aurora, Plasma, Tunnel, Waves, Particles, Gradient, and Solid color
- Each effect configurable via `BuiltInEffects` factory with sensible defaults

#### Effect Hydration (JS)
- **`SigilEffectHydrator`**: Client-side hydration of serialized effect data
- Integrates with Materia's `EffectComposer` and `FullScreenEffectPass`
- Mouse and touch interaction support
- Automatic canvas resize handling

### Changed
- Updated to Kotlin 2.2.21
- Updated to Materia 0.3.2.0 (adds SCREEN/OVERLAY blend modes and Mat3/Mat4 uniforms)
- Updated to kotlinx.serialization 1.9.0

### Technical Details
- 38 new unit tests for effect system
- Full support for Mat3 and Mat4 shader uniforms
- Native SCREEN blend mode support (previously approximated)

## [0.1.0.0] - 2024-12-10
### Added
- Initial project setup with Sigil Schema, Compose, and Summon modules.
- Basic geometry (Box, Sphere, Plane) and lighting (Ambient, Directional) components.
- Documentation and quickstart guide.
