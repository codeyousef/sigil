# Materials

Sigil uses a physically-based rendering (PBR) workflow to create realistic materials. The appearance of an object is primarily defined by its `color`, `metalness`, and `roughness`.

## PBR Properties

### Color
The base diffuse color of the material.
- **Type**: `Int` (ARGB)
- **Example**: `0xFFFF0000.toInt()` (Red)

### Metalness
How metallic the material looks.
- **Range**: `0.0` (Dielectric/Plastic) to `1.0` (Metal)
- **0.0**: Plastic, wood, stone, skin.
- **1.0**: Gold, silver, copper, steel.

### Roughness
How rough the surface is.
- **Range**: `0.0` (Smooth/Mirror-like) to `1.0` (Matte/Diffuse)
- **0.0**: Polished mirror.
- **1.0**: Chalk, dry earth.

## Examples

### Plastic (Shiny)
```kotlin
Box(
    color = 0xFF0000FF.toInt(),
    metalness = 0.0f,
    roughness = 0.1f // Smooth, shiny reflection
)
```

### Gold
```kotlin
Sphere(
    color = 0xFFFFD700.toInt(),
    metalness = 1.0f, // Fully metallic
    roughness = 0.2f  // Polished but not perfect mirror
)
```

### Matte Rubber
```kotlin
Box(
    color = 0xFF333333.toInt(),
    metalness = 0.0f,
    roughness = 0.9f  // Very rough, almost no reflection
)
```

## Lighting Interaction

Materials interact with the lights in your scene.
- **AmbientLight**: Raises the base brightness of all materials.
- **Directional/Point/Spot Lights**: Create highlights (specular) on shiny materials and define the diffuse shading.

**Note:** If `metalness` > 0, the material will appear very dark if there are no lights or environment maps to reflect. Ensure you have adequate lighting.
