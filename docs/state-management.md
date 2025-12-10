# State Management in Sigil

Sigil leverages Jetpack Compose's state management system to drive the 3D scene. If you know how to manage state in a standard Compose application, you already know how to manage a Sigil scene.

## Reactive Properties

Every property of a Sigil Composable (position, rotation, color, etc.) is reactive. When you update a state variable that is passed to a Composable, Sigil automatically updates the underlying 3D node without recreating it.

### Example: Interactive Color Change

```kotlin
@Composable
fun ColorChanger() {
    // Standard Compose State
    var isRed by remember { mutableStateOf(true) }

    Column {
        Button(onClick = { isRed = !isRed }) {
            Text("Toggle Color")
        }

        MateriaCanvas(modifier = Modifier.fillMaxSize()) {
            Box(
                // The color updates automatically when isRed changes
                color = if (isRed) 0xFFFF0000.toInt() else 0xFF0000FF.toInt()
            )
        }
    }
}
```

## Animation

### Continuous Animation (Game Loop)

For continuous animation (like rotation), use `LaunchedEffect` with `withFrameNanos`.

```kotlin
@Composable
fun RotatingCube() {
    var rotationY by remember { mutableStateOf(0f) }

    // Animation Loop
    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { time ->
                val delta = (time - startTime) / 1_000_000_000f
                rotationY = (time / 10_000_000L) % 360f // Rotate based on time
            }
        }
    }

    MateriaCanvas {
        Box(
            rotation = Vector3(0f, rotationY, 0f)
        )
    }
}
```

### Tween Animation

You can also use standard Compose animation APIs like `animateFloatAsState`.

```kotlin
@Composable
fun AnimatedScale() {
    var expanded by remember { mutableStateOf(false) }
    
    // Smoothly animate the scale value
    val scale by animateFloatAsState(
        targetValue = if (expanded) 2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Column {
        Button(onClick = { expanded = !expanded }) { Text("Expand") }
        
        MateriaCanvas {
            Box(
                scale = Vector3(scale, scale, scale)
            )
        }
    }
}
```

## State Hoisting

Just like in standard Compose, it is best practice to hoist state out of your 3D components to keep them stateless and reusable.

```kotlin
@Composable
fun MyScene(
    rotation: Float,
    onSceneClick: () -> Unit // Note: Click handling requires custom implementation
) {
    MateriaCanvas {
        Group(rotation = Vector3(0f, rotation, 0f)) {
            // ... contents
        }
    }
}
```
