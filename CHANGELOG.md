# Changelog

All notable changes to this project will be documented in this file.

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
