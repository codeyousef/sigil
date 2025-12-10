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
