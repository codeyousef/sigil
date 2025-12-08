package io.github.codeyousef.sigil.sample.cmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.codeyousef.sigil.compose.AmbientLight3D
import io.github.codeyousef.sigil.compose.DirectionalLight3D
import io.github.codeyousef.sigil.compose.Group3D
import io.github.codeyousef.sigil.compose.MateriaCanvas
import io.github.codeyousef.sigil.compose.Mesh3D
import io.github.codeyousef.sigil.schema.GeometrySpec
import io.github.codeyousef.sigil.schema.MaterialSpec
import io.github.codeyousef.sigil.schema.Transform

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
        // 3D View
        MateriaCanvas(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize()
        ) {
            // Ambient light for base illumination
            AmbientLight3D(
                color = 0x404040,
                intensity = 0.3f
            )

            // Main directional light
            DirectionalLight3D(
                color = 0xFFFFFF,
                intensity = lightIntensity,
                transform = Transform(
                    position = listOf(5f, 10f, 7.5f)
                )
            )

            // Rotating group containing meshes
            Group3D(
                transform = Transform(
                    rotation = listOf(0f, rotationY, 0f)
                )
            ) {
                // Central cube
                Mesh3D(
                    geometry = GeometrySpec.Box(
                        width = 1f * cubeScale,
                        height = 1f * cubeScale,
                        depth = 1f * cubeScale
                    ),
                    material = MaterialSpec.Standard(
                        color = 0x4488FF,
                        metalness = 0.3f,
                        roughness = 0.4f
                    ),
                    transform = Transform(
                        position = listOf(0f, 0f, 0f)
                    )
                )

                // Orbiting sphere (conditionally rendered)
                if (showSphere) {
                    Mesh3D(
                        geometry = GeometrySpec.Sphere(
                            radius = 0.4f,
                            widthSegments = 32,
                            heightSegments = 32
                        ),
                        material = MaterialSpec.Standard(
                            color = 0xFF4488,
                            metalness = 0.7f,
                            roughness = 0.2f
                        ),
                        transform = Transform(
                            position = listOf(2f, 0f, 0f)
                        )
                    )
                }

                // Ground plane
                Mesh3D(
                    geometry = GeometrySpec.Plane(
                        width = 10f,
                        height = 10f
                    ),
                    material = MaterialSpec.Standard(
                        color = 0x888888,
                        metalness = 0.0f,
                        roughness = 0.8f
                    ),
                    transform = Transform(
                        position = listOf(0f, -1.5f, 0f),
                        rotation = listOf(-90f, 0f, 0f)
                    )
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
            Text(text = "Cube Scale: ${String.format("%.2f", cubeScale)}")
            Slider(
                value = cubeScale,
                onValueChange = { cubeScale = it },
                valueRange = 0.5f..2f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Light intensity control
            Text(text = "Light Intensity: ${String.format("%.2f", lightIntensity)}")
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

            Spacer(modifier = Modifier.height(8.dp))

            // Reset button
            Button(
                onClick = {
                    rotationY = 0f
                    cubeScale = 1f
                    lightIntensity = 1f
                    showSphere = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Scene")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Sigil - Declarative 3D for Kotlin",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
