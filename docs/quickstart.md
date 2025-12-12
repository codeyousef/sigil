# Quickstart Guide

This guide will help you set up a basic Sigil project and render a simple 3D scene.

## Prerequisites

- JDK 17
- Kotlin Multiplatform project setup
- Jetpack Compose (Multiplatform)

## Installation

Add the following dependencies to your `build.gradle.kts` (commonMain source set):

```kotlin
implementation("codes.yousef.sigil:sigil-compose:0.2.7.7")
implementation("codes.yousef.sigil:sigil-schema:0.2.7.7")
```

Ensure you have the correct repositories configured (e.g., Maven Local or a snapshot repository if not on Maven Central).

## Basic Usage

The core entry point is `MateriaCanvas`. Inside it, you can define lights, groups, and geometry.

### Example: A Rotating Cube

```kotlin
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import codes.yousef.sigil.compose.canvas.MateriaCanvas
import codes.yousef.sigil.compose.composition.Box
import codes.yousef.sigil.compose.composition.AmbientLight
import codes.yousef.sigil.compose.composition.DirectionalLight
import codes.yousef.sigil.compose.composition.Group
import io.materia.core.math.Vector3

@Composable
fun SimpleScene() {
    var rotationY by remember { mutableStateOf(0f) }

    // Update rotation (animation loop omitted for brevity, use LaunchedEffect withFrameNanos)

    MateriaCanvas(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = 0xFF1A1A2E.toInt()
    ) {
        // 1. Add Lighting
        AmbientLight(
            color = 0x404040,
            intensity = 0.3f
        )
        DirectionalLight(
            color = 0xFFFFFFFF.toInt(),
            intensity = 1.0f,
            position = Vector3(5f, 10f, 7.5f)
        )

        // 2. Add Objects
        Group(
            rotation = Vector3(0f, rotationY, 0f)
        ) {
            Box(
                width = 1f,
                height = 1f,
                depth = 1f,
                color = 0xFF4488FF.toInt(),
                position = Vector3.ZERO
            )
        }
    }
}
```

### Key Components

- **`MateriaCanvas`**: The root composable that initializes the 3D context.
- **`AmbientLight` / `DirectionalLight`**: Lighting sources.
- **`Group`**: A container for transforming multiple objects together.
- **`Box`**: A primitive 3D geometry.
- **`Vector3`**: Used for positioning, rotation, and scaling.

## Next Steps

Explore the [Compose API Reference](api-reference/compose.md) to see all available components.
