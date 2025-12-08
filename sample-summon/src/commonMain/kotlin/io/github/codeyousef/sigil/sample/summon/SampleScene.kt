package io.github.codeyousef.sigil.sample.summon

import codes.yousef.summon.annotation.Composable
import io.github.codeyousef.sigil.schema.GeometrySpec
import io.github.codeyousef.sigil.schema.MaterialSpec
import io.github.codeyousef.sigil.schema.SigilScene
import io.github.codeyousef.sigil.schema.Transform
import io.github.codeyousef.sigil.summon.AmbientLight3D
import io.github.codeyousef.sigil.summon.DirectionalLight3D
import io.github.codeyousef.sigil.summon.Group3D
import io.github.codeyousef.sigil.summon.MateriaCanvas
import io.github.codeyousef.sigil.summon.Mesh3D

/**
 * Sample 3D scene demonstrating Sigil with Summon.
 * This scene is rendered on the server and hydrated on the client.
 */
@Composable
fun Sample3DScene() {
    MateriaCanvas(
        canvasId = "sigil-scene",
        width = 800,
        height = 600
    ) {
        buildSampleScene()
    }
}

/**
 * Builds the sample scene content.
 * This is called within the MateriaCanvas context.
 */
fun buildSampleScene(): SigilScene {
    return SigilScene {
        // Lighting setup
        ambientLight(
            color = 0x404040,
            intensity = 0.4f
        )

        directionalLight(
            color = 0xFFFFFF,
            intensity = 1.2f,
            position = listOf(10f, 15f, 10f)
        )

        // Main showcase group
        group(
            transform = Transform(
                position = listOf(0f, 0f, 0f)
            )
        ) {
            // Central metallic sphere
            mesh(
                geometry = GeometrySpec.Sphere(
                    radius = 1.0f,
                    widthSegments = 64,
                    heightSegments = 64
                ),
                material = MaterialSpec.Standard(
                    color = 0xCCCCCC,
                    metalness = 0.9f,
                    roughness = 0.1f
                ),
                transform = Transform(
                    position = listOf(0f, 1f, 0f)
                )
            )

            // Surrounding cubes
            val cubePositions = listOf(
                listOf(-3f, 0.5f, -3f),
                listOf(3f, 0.5f, -3f),
                listOf(-3f, 0.5f, 3f),
                listOf(3f, 0.5f, 3f)
            )

            val cubeColors = listOf(0xFF4444, 0x44FF44, 0x4444FF, 0xFFFF44)

            cubePositions.forEachIndexed { index, position ->
                mesh(
                    geometry = GeometrySpec.Box(
                        width = 1f,
                        height = 1f,
                        depth = 1f
                    ),
                    material = MaterialSpec.Standard(
                        color = cubeColors[index],
                        metalness = 0.3f,
                        roughness = 0.6f
                    ),
                    transform = Transform(
                        position = position,
                        rotation = listOf(0f, 45f, 0f)
                    )
                )
            }

            // Ground plane
            mesh(
                geometry = GeometrySpec.Plane(
                    width = 20f,
                    height = 20f
                ),
                material = MaterialSpec.Standard(
                    color = 0x333333,
                    metalness = 0.0f,
                    roughness = 0.9f
                ),
                transform = Transform(
                    position = listOf(0f, 0f, 0f),
                    rotation = listOf(-90f, 0f, 0f)
                )
            )
        }

        // Camera configuration
        camera(
            position = listOf(8f, 6f, 8f),
            target = listOf(0f, 0.5f, 0f),
            fov = 60f,
            near = 0.1f,
            far = 1000f
        )

        // Environment settings
        environment(
            backgroundColor = 0x111122,
            fogEnabled = false
        )
    }
}
