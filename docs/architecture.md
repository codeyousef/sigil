# Architecture

Sigil is built on a modular architecture designed to separate the declarative UI (Compose) from the underlying data model and the rendering engine. This separation allows for flexibility across different platforms.

## Module Structure

### 1. `sigil-compose` (The Frontend)
- **Role**: Provides the Jetpack Compose API (`MateriaCanvas`, `Box`, `Sphere`).
- **Function**: It builds and manages a tree of **Nodes** using Compose's `Applier` system. It does *not* perform rendering directly.
- **Dependency**: Depends on `sigil-schema` and `sigil-summon`.

### 2. `sigil-schema` (The Data Model)
- **Role**: Defines the platform-agnostic, serializable data structures for the scene graph.
- **Key Classes**: `SigilNodeData`, `MeshData`, `LightData`.
- **Function**: Acts as the "Contract" between the Compose layer and the Engine layer. This allows the scene to be serialized (e.g., to JSON) or transmitted over a network.

### 3. `sigil-summon` (The Engine)
- **Role**: The core logic responsible for interpreting the scene graph and rendering it.
- **Function**:
    - **Summoning**: Converts `SigilNodeData` into actual GPU resources (Meshes, Shaders).
    - **Rendering**: Manages the render loop (Vulkan/WebGPU/Metal).
    - **Context**: `SigilSummonContext` holds the state of the active engine.

## The Render Loop

1. **Composition**: You write Compose code (`Box`, `Sphere`).
2. **Reconciliation**: Compose detects changes (e.g., color changed from Red to Blue).
3. **Update**: The `sigil-compose` nodes update their internal properties.
4. **Sync**: These property updates are pushed to the `sigil-summon` engine objects.
5. **Draw**: The engine renders the updated frame to the canvas.

## Platform Support

- **JVM (Desktop)**: Uses AWT/Swing Canvas with Vulkan backing.
- **JS (Web)**: Uses HTML5 Canvas with WebGPU (or WebGL2 fallback).
- **Android/iOS**: (Planned) SurfaceView and Metal layers.
