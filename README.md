# Sigil 🔮

**Declarative 3D for Kotlin Multiplatform & Jetpack Compose**

[![Maven Central](https://img.shields.io/maven-central/v/codes.yousef.sigil/sigil-compose)](https://central.sonatype.com/artifact/codes.yousef.sigil/sigil-compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7f52ff.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)

---

**Sigil** is a powerful library that brings 3D rendering to your Compose Multiplatform applications using a familiar, declarative syntax. Built on top of the **Materia** engine, Sigil bridges the gap between the reactivity of Compose and the performance of low-level graphics APIs (Vulkan, WebGPU, Metal).

## ✨ Features

- **Declarative Syntax**: Build scenes with `Box`, `Sphere`, `Group`, and `Light` composables.
- **Reactive State**: Drive animations and scene updates using standard Compose state (`mutableStateOf`, `animate*AsState`).
- **Multiplatform**:
    - 🖥️ **JVM (Desktop)**: Vulkan-backed rendering.
    - 🌐 **Web (JS/Wasm)**: WebGPU/WebGL2 support.
    - 📱 **Android/iOS**: (Coming Soon)
- **PBR Materials**: Physically Based Rendering for realistic lighting and materials.
- **Zero Boilerplate**: No manual loop management or context handling required.

## 📦 Installation

Add Sigil to your `commonMain` dependencies in `build.gradle.kts`:

```kotlin
implementation("codes.yousef.sigil:sigil-compose:0.2.7.10")
implementation("codes.yousef.sigil:sigil-schema:0.2.7.10")

// For SSR with Ktor, Spring Boot, or Quarkus:
implementation("codes.yousef.sigil:sigil-summon:0.2.7.10")
```

## 🚀 Quick Start

Create a stunning 3D scene in just a few lines of code:

```kotlin
import codes.yousef.sigil.compose.canvas.MateriaCanvas
import codes.yousef.sigil.compose.composition.*
import io.materia.core.math.Vector3

@Composable
fun RotatingCube() {
    var rotationY by remember { mutableStateOf(0f) }

    // Your animation logic here...

    MateriaCanvas(modifier = Modifier.fillMaxSize()) {
        // Lighting
        AmbientLight(intensity = 0.5f)
        DirectionalLight(position = Vector3(5f, 10f, 5f))

        // Objects
        Group(rotation = Vector3(0f, rotationY, 0f)) {
            Box(
                color = 0xFF4488FF.toInt(),
                metalness = 0.5f,
                roughness = 0.1f
            )
        }
    }
}
```

## 📚 Documentation

Explore the full guides and API reference:

- [**Quickstart Guide**](docs/quickstart.md)
- [**Components Guide**](docs/components.md)
- [**State Management**](docs/state-management.md)
- [**Materials & PBR**](docs/materials.md)
- [**SSR & Hydration**](docs/ssr-hydration.md)
- [**Architecture**](docs/architecture.md)

## 🤝 Contributing

Contributions are welcome! Please check our [Contributing Guidelines](CONTRIBUTING.md) before submitting a PR.

## 📄 License

This project is licensed under the MIT License.

---

*Built with ❤️ by [CodeYousef](https://github.com/codeyousef)*
