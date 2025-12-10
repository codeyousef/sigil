# Components

Sigil provides a set of Composable components to build your 3D scenes. These components map directly to Materia engine objects.

## Core Hierarchy

### `MateriaCanvas`

The root component of any Sigil scene.

```kotlin
MateriaCanvas(
    modifier = Modifier.fillMaxSize(),
    backgroundColor = 0xFF000000.toInt(),
    camera = myPerspectiveCamera
) {
    // Scene content goes here
}
```

### `Group`

A container used to transform multiple objects together.

```kotlin
Group(
    position = Vector3(1f, 0f, 0f),
    rotation = Vector3(0f, 45f, 0f)
) {
    Box(...)
    Sphere(...)
}
```

## Geometry

Sigil provides convenience Composables for common shapes, and a generic `Mesh` Composable for others.

### Convenience Shapes

#### `Box`
```kotlin
Box(
    width = 1f, height = 1f, depth = 1f,
    color = 0xFFFF0000.toInt() // Red
)
```

#### `Sphere`
```kotlin
Sphere(
    radius = 1f,
    widthSegments = 32, heightSegments = 16,
    color = 0xFF00FF00.toInt() // Green
)
```

#### `Plane`
```kotlin
Plane(
    width = 10f, height = 10f,
    color = 0xFF888888.toInt()
)
```

### Generic Mesh & Other Shapes

For shapes without a dedicated Composable, use the `Mesh` component with a `GeometryType`.

**Supported Types:** `CYLINDER`, `CONE`, `TORUS`, `CIRCLE`, `RING`, `ICOSAHEDRON`, `OCTAHEDRON`, `TETRAHEDRON`, `DODECAHEDRON`.

```kotlin
// Example: Creating a Cylinder
Mesh(
    geometryType = GeometryType.CYLINDER,
    geometryParams = GeometryParams(
        radiusTop = 1f,
        radiusBottom = 1f,
        height = 2f,
        radialSegments = 32
    ),
    color = 0xFF0000FF.toInt()
)
```

## Lights

Lights illuminate the scene. Note that `AmbientLight` and `DirectionalLight` affect the whole scene, while `PointLight` and `SpotLight` are positional.

### `AmbientLight`
Base illumination.
```kotlin
AmbientLight(color = 0x404040, intensity = 0.5f)
```

### `DirectionalLight`
Sun-like light source.
```kotlin
DirectionalLight(
    color = 0xFFFFFF,
    intensity = 1f,
    position = Vector3(5f, 10f, 5f),
    castShadow = true
)
```

### `PointLight`
Light from a specific point.
```kotlin
PointLight(
    color = 0xFF0000,
    intensity = 1f,
    position = Vector3(0f, 2f, 0f),
    distance = 10f,
    decay = 2f
)
```

### `SpotLight`
Cone of light.
```kotlin
SpotLight(
    color = 0xFFFFFF,
    position = Vector3(0f, 5f, 0f),
    angle = 0.5f
)
```
