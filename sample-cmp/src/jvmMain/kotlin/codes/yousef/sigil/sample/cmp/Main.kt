package codes.yousef.sigil.sample.cmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import codes.yousef.sigil.compose.canvas.MateriaCanvas
import codes.yousef.sigil.compose.composition.Box
import codes.yousef.sigil.compose.composition.Sphere
import codes.yousef.sigil.compose.composition.Plane
import codes.yousef.sigil.compose.composition.Group
import codes.yousef.sigil.compose.composition.AmbientLight
import codes.yousef.sigil.compose.composition.DirectionalLight
import io.materia.core.math.Vector3

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Sigil - Compose Multiplatform Sample"
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                SampleApp()
            }
        }
    }
}

@Composable
fun SampleApp() {
    var rotationY by remember { mutableStateOf(0f) }
    var cubeScale by remember { mutableStateOf(1f) }
    var lightIntensity by remember { mutableStateOf(1f) }
    var showSphere by remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        // 3D View - Note: MateriaCanvas needs a proper Compose Modifier
        MateriaCanvas(
            modifier = Modifier.weight(2f).fillMaxSize(),
            backgroundColor = 0xFF1A1A2E.toInt()
        ) {
            // Ambient light for base illumination
            AmbientLight(
                color = 0x404040,
                intensity = 0.3f
            )

            // Main directional light
            DirectionalLight(
                color = 0xFFFFFFFF.toInt(),
                intensity = lightIntensity,
                position = Vector3(5f, 10f, 7.5f)
            )

            // Rotating group containing meshes
            Group(
                rotation = Vector3(0f, rotationY, 0f)
            ) {
                // Central cube
                Box(
                    width = 1f * cubeScale,
                    height = 1f * cubeScale,
                    depth = 1f * cubeScale,
                    color = 0xFF4488FF.toInt(),
                    position = Vector3.ZERO
                )

                // Orbiting sphere (conditionally rendered)
                if (showSphere) {
                    Sphere(
                        radius = 0.4f,
                        widthSegments = 32,
                        heightSegments = 32,
                        color = 0xFFFF4488.toInt(),
                        position = Vector3(2f, 0f, 0f)
                    )
                }

                // Ground plane
                Plane(
                    width = 10f,
                    height = 10f,
                    color = 0xFF888888.toInt(),
                    position = Vector3(0f, -1.5f, 0f),
                    rotation = Vector3(-90f, 0f, 0f)
                )
            }
        }

        // Control Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Scene Controls",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Rotation control
            Text(text = "Rotation Y: ${rotationY.toInt()}°")
            Slider(
                value = rotationY,
                onValueChange = { rotationY = it },
                valueRange = 0f..360f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scale control
            Text(text = "Cube Scale: ${"%.1f".format(cubeScale)}x")
            Slider(
                value = cubeScale,
                onValueChange = { cubeScale = it },
                valueRange = 0.5f..3f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Light intensity control
            Text(text = "Light Intensity: ${"%.1f".format(lightIntensity)}")
            Slider(
                value = lightIntensity,
                onValueChange = { lightIntensity = it },
                valueRange = 0f..2f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle sphere button
            Button(
                onClick = { showSphere = !showSphere },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showSphere) "Hide Sphere" else "Show Sphere")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-rotate button
            var autoRotate by remember { mutableStateOf(false) }
            Button(
                onClick = { autoRotate = !autoRotate },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (autoRotate) "Stop Rotation" else "Auto Rotate")
            }

            // Handle auto-rotation in a LaunchedEffect if needed
        }
    }
}
