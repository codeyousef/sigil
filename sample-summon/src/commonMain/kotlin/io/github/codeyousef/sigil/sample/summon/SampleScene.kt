package io.github.codeyousef.sigil.sample.summon

import codes.yousef.summon.annotation.Composable
import io.github.codeyousef.sigil.summon.components.SigilBox
import io.github.codeyousef.sigil.summon.components.SigilSphere
import io.github.codeyousef.sigil.summon.components.SigilPlane
import io.github.codeyousef.sigil.summon.components.SigilAmbientLight
import io.github.codeyousef.sigil.summon.components.SigilDirectionalLight
import io.github.codeyousef.sigil.summon.components.SigilCamera

/**
 * Sample 3D scene demonstrating Sigil with Summon.
 * This scene is rendered on the server and serialized to JSON.
 * Client-side hydration would use this data to create Materia objects.
 */
@Composable
fun Sample3DScene() {
    // Lighting setup
    SigilAmbientLight(
        color = 0x404040,
        intensity = 0.4f
    )

    SigilDirectionalLight(
        color = 0xFFFFFF,
        intensity = 1.2f,
        position = listOf(10f, 15f, 10f)
    )

    // Central metallic sphere
    SigilSphere(
        radius = 1.0f,
        widthSegments = 64,
        heightSegments = 64,
        color = 0xCCCCCC,
        metalness = 0.9f,
        roughness = 0.1f,
        position = listOf(0f, 1f, 0f)
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
        SigilBox(
            width = 1f,
            height = 1f,
            depth = 1f,
            color = cubeColors[index],
            metalness = 0.3f,
            roughness = 0.6f,
            position = position,
            rotation = listOf(0f, 45f, 0f)
        )
    }

    // Ground plane
    SigilPlane(
        width = 20f,
        height = 20f,
        color = 0x333333,
        position = listOf(0f, 0f, 0f),
        rotation = listOf(-90f, 0f, 0f)
    )

    // Camera configuration
    SigilCamera(
        position = listOf(8f, 6f, 8f),
        lookAt = listOf(0f, 0.5f, 0f),
        fov = 60f,
        near = 0.1f,
        far = 1000f
    )
}
