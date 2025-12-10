# Summon Core API Reference

The `sigil-summon` module is the core engine (or "summoner") responsible for interpreting the scene graph and managing the rendering lifecycle.

**Note**: Most users will interact with Sigil via `sigil-compose` and do not need to use this API directly.

## Components

### `SigilSummonContext`

A context class that holds the current state of the engine, including the active scene, renderer, and camera.

### `MateriaCanvas` (Core)

The platform-agnostic implementation of the canvas handling logic. It bridges the gap between the abstract scene graph and the platform-specific graphics context (JVM AWT, Android Surface, WebGL/WebGPU).

## Logic

Sigil uses a "Summon" concept where nodes are summoned into existence.
- **Components**: Functions that create and configure specific types of nodes (Geometry, Lights, Camera).
- **Context**: Manages the dependency injection and state for the rendering pipeline.
