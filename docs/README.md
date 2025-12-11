# Sigil Documentation

Welcome to the Sigil documentation! Sigil is a library for building 3D scenes using a declarative Compose Multiplatform API.

## Table of Contents

### Getting Started
- [Quickstart Guide](quickstart.md) - Learn how to set up and run your first Sigil application.

### Guides
- [Components](components.md) - A visual guide to Geometries, Lights, and other 3D objects.
- [State Management](state-management.md) - Learn how to drive animations and updates using Compose state.
- [Materials](materials.md) - Understanding PBR materials (Color, Metalness, Roughness).
- [SSR & Hydration](ssr-hydration.md) - Server-side rendering with Ktor and client-side hydration.
- [Architecture](architecture.md) - Deep dive into Sigil's internal modular design.

### API Reference
- [Compose API](api-reference/compose.md) - Documentation for `MateriaCanvas`, Geometries, Lights, and other Composables.
- [Schema API](api-reference/schema.md) - The underlying data model (`SigilNodeData`, `SigilScene`).
- [Summon Core API](api-reference/summon.md) - Core logic and rendering components.

## Key Features

- **Declarative 3D**: Build 3D scenes using familiar Jetpack Compose syntax.
- **Multiplatform**: Designed for Kotlin Multiplatform (JVM, JS, etc.).
- **Scene Graph**: Managed automatically via Compose structure.
- **Lighting & Materials**: Built-in support for ambient and directional lights, and basic materials.
- **Geometry Primitives**: Ready-to-use shapes like Box, Sphere, Plane.

## Modules

- **sigil-compose**: The Compose Multiplatform integration layer.
- **sigil-schema**: The shared data model representing the scene.
- **sigil-summon**: The core logic for processing and rendering the scene.
