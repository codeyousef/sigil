# Changelog

All notable changes to this project will be documented in this file.

## [0.2.5.1] - 2025-12-11

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
