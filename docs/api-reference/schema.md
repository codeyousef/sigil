# Schema API Reference

The `sigil-schema` module contains the platform-agnostic, serializable data model for the scene graph. These classes are typically created automatically by the `sigil-compose` layer, but they can be used directly for low-level scene manipulation or serialization.

## Base Class

### `SigilNodeData`

The sealed interface representing a node in the scene graph.

Properties common to all nodes:
- `id`: String (Unique identifier)
- `position`: `List<Float>` [x, y, z]
- `rotation`: `List<Float>` [x, y, z] (Euler angles)
- `scale`: `List<Float>` [x, y, z]
- `visible`: Boolean
- `name`: String?

## Node Types

### `MeshData`
Represents a geometric object.
- `geometryType`: `GeometryType` (BOX, SPHERE, PLANE, etc.)
- `geometryParams`: `GeometryParams` (dimensions, segments)
- `materialColor`: Int
- `metalness`: Float
- `roughness`: Float
- `castShadow`: Boolean
- `receiveShadow`: Boolean

### `GroupData`
A container for other nodes.
- `children`: `List<SigilNodeData>`

### `LightData`
Represents a light source.
- `lightType`: `LightType` (AMBIENT, DIRECTIONAL, POINT, SPOT, HEMISPHERE)
- `color`: Int
- `intensity`: Float
- `distance`, `decay`, `angle`, `penumbra` (Light specific)
- `castShadow`: Boolean
- `target`: `List<Float>`

### `CameraData`
Represents a camera.
- `cameraType`: `CameraType` (PERSPECTIVE, ORTHOGRAPHIC)
- `fov`, `aspect`, `near`, `far`
- `orthoBounds`: `List<Float>`
- `lookAt`: `List<Float>?`

## Enums & Helpers

### `GeometryType`
Supported primitives: `BOX`, `SPHERE`, `PLANE`, `CYLINDER`, `CONE`, `TORUS`, `CIRCLE`, `RING`, etc.

### `GeometryParams`
A data class holding all potential geometry parameters (width, height, radius, segments, etc.).

### `LightType`
`AMBIENT`, `DIRECTIONAL`, `POINT`, `SPOT`, `HEMISPHERE`.

---

## Screen-Space Effects

### `ShaderEffectData`

Data class for a single shader effect.

```kotlin
@Serializable
data class ShaderEffectData(
    val id: String,
    val name: String? = null,
    val fragmentShader: String,           // WGSL shader code
    val glslFragmentShader: String? = null, // Optional GLSL fallback
    val blendMode: BlendMode = BlendMode.NORMAL,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    val uniforms: Map<String, UniformValue> = emptyMap(),
    val enableMouseInteraction: Boolean = false,
    val timeScale: Float = 1f
)
```

### `EffectComposerData`

Container for multiple effects to be rendered in sequence.

```kotlin
@Serializable
data class EffectComposerData(
    val id: String,
    val effects: List<ShaderEffectData> = emptyList()
)
```

### `SigilCanvasConfig`

Configuration for a SigilCanvas effect rendering.

```kotlin
@Serializable
data class SigilCanvasConfig(
    val id: String = "sigil-canvas",
    val respectDevicePixelRatio: Boolean = true,
    val powerPreference: PowerPreference = PowerPreference.HIGH_PERFORMANCE,
    val fallbackToWebGL: Boolean = true,
    val fallbackToCSS: Boolean = true
)
```

### `InteractionConfig`

Configuration for user interaction handling.

```kotlin
@Serializable
data class InteractionConfig(
    val enableMouse: Boolean = true,
    val enableTouch: Boolean = true,
    val smoothing: Float = 0.1f
)
```

### `UniformValue`

Sealed class for shader uniform values.

```kotlin
@Serializable
sealed class UniformValue {
    data class FloatValue(val value: Float) : UniformValue()
    data class Vec2Value(val value: Vec2) : UniformValue()
    data class Vec3Value(val value: Vec3) : UniformValue()
    data class Vec4Value(val value: Vec4) : UniformValue()
    data class IntValue(val value: Int) : UniformValue()
    data class ColorValue(val value: Int) : UniformValue()
}
```

### `BlendMode`

Blend modes for compositing effects.

```kotlin
enum class BlendMode {
    NORMAL, ADD, MULTIPLY, SCREEN, OVERLAY
}
```
