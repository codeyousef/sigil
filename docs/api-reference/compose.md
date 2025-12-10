# Compose API Reference

This module (`sigil-compose`) provides the Jetpack Compose integration for Sigil. It allows you to build 3D scenes using a declarative tree of Composables.

## Core Components

### `MateriaCanvas`

The root entry point for any Sigil scene. It initializes the rendering context (e.g., Vulkan on Desktop, WebGPU on Web) and manages the scene graph.

```kotlin
@Composable
fun MateriaCanvas(
    modifier: Modifier = Modifier,
    backgroundColor: Int = 0xFF1A1A2E.toInt(),
    camera: PerspectiveCamera? = null,
    content: @Composable () -> Unit
)
```

- **`modifier`**: Compose modifier for layout sizing and positioning.
- **`backgroundColor`**: The background color of the canvas (ARGB integer).
- **`camera`**: Optional `PerspectiveCamera`. If null, a default camera is created.
- **`content`**: The scope where you add 3D objects (Nodes, Lights).

## Geometry

Sigil provides several primitive shapes. Common parameters for all meshes:

- **`position`**: `Vector3` (default: ZERO)
- **`rotation`**: `Vector3` (Euler angles in degrees)
- **`scale`**: `Vector3` (default: ONE)
- **`color`**: `Int` (ARGB)
- **`metalness`**: `Float` (0.0 - 1.0)
- **`roughness`**: `Float` (0.0 - 1.0)
- **`visible`**: `Boolean`
- **`castShadow`** / **`receiveShadow`**: `Boolean`

### `Box`

Creates a cuboid mesh.

```kotlin
@Composable
fun Box(
    width: Float = 1f,
    height: Float = 1f,
    depth: Float = 1f,
    // ... common mesh params
)
```

### `Sphere`

Creates a spherical mesh.

```kotlin
@Composable
fun Sphere(
    radius: Float = 1f,
    widthSegments: Int = 32,
    heightSegments: Int = 16,
    // ... common mesh params
)
```

### `Plane`

Creates a flat plane.

```kotlin
@Composable
fun Plane(
    width: Float = 1f,
    height: Float = 1f,
    // ... common mesh params
)
```

## Grouping & Transformation

### `Group`

A container that groups multiple objects. Useful for applying transforms (position, rotation, scale) to a collection of objects.

```kotlin
@Composable
fun Group(
    position: Vector3 = Vector3.ZERO,
    rotation: Vector3 = Vector3.ZERO,
    scale: Vector3 = Vector3.ONE,
    visible: Boolean = true,
    content: @Composable () -> Unit
)
```

## Lighting

Sigil supports several types of lights. Note that lights are generally added to the scene via `MateriaCanvas` content, but they affect the entire scene via the lighting context.

### `AmbientLight`
Illuminates all objects equally.

```kotlin
@Composable
fun AmbientLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f
)
```

### `DirectionalLight`
Emits parallel rays (e.g., sunlight).

```kotlin
@Composable
fun DirectionalLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3,
    castShadow: Boolean = false
)
```

### `PointLight`
Emits light from a single point in all directions.

```kotlin
@Composable
fun PointLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3,
    distance: Float = 0f,
    decay: Float = 2f
)
```

### `SpotLight`
Emits a cone of light.

```kotlin
@Composable
fun SpotLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3,
    angle: Float = 0.523f,
    penumbra: Float = 0f
)
```

### `HemisphereLight`
Simulates sky and ground lighting.

```kotlin
@Composable
fun HemisphereLight(
    skyColor: Int,
    groundColor: Int,
    intensity: Float = 1f
)
```
