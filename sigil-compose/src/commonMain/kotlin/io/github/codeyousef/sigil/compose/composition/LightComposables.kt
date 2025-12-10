package io.github.codeyousef.sigil.compose.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import io.github.codeyousef.sigil.compose.context.LocalMateriaLightingContext
import io.materia.core.math.Vector3
import io.materia.lighting.AmbientLightImpl
import io.materia.lighting.DirectionalLightImpl
import io.materia.lighting.PointLightImpl
import io.materia.lighting.SpotLightImpl
import io.materia.lighting.HemisphereLightImpl

/**
 * Ambient light that illuminates all objects equally.
 * 
 * Note: Lights in Materia are managed through a LightingSystem rather than
 * the scene graph. Ensure a MateriaLightingContext is provided.
 */
@Composable
fun AmbientLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(color, intensity, castShadow) {
        val light = AmbientLightImpl().apply {
            this.color = intToColor(color)
            this.intensity = intensity
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

/**
 * Directional light that emits parallel rays (like sunlight).
 */
@Composable
fun DirectionalLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3 = Vector3(5f, 10f, 7.5f),
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(color, intensity, position, castShadow) {
        val light = DirectionalLightImpl().apply {
            this.color = intToColor(color)
            this.intensity = intensity
            this.position = position
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

/**
 * Point light that emits from a single point in all directions.
 */
@Composable
fun PointLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3 = Vector3.ZERO,
    distance: Float = 0f,
    decay: Float = 2f,
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(color, intensity, position, distance, decay, castShadow) {
        val light = PointLightImpl().apply {
            this.color = intToColor(color)
            this.intensity = intensity
            this.position = position
            this.distance = distance
            this.decay = decay
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

/**
 * Spot light that emits a cone of light.
 */
@Composable
fun SpotLight(
    color: Int = 0xFFFFFFFF.toInt(),
    intensity: Float = 1f,
    position: Vector3 = Vector3.ZERO,
    distance: Float = 0f,
    angle: Float = 0.523599f, // PI/6
    penumbra: Float = 0f,
    decay: Float = 2f,
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(color, intensity, position, distance, angle, penumbra, decay, castShadow) {
        val light = SpotLightImpl().apply {
            this.color = intToColor(color)
            this.intensity = intensity
            this.position = position
            this.distance = distance
            this.angle = angle
            this.penumbra = penumbra
            this.decay = decay
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}

/**
 * Hemisphere light with sky and ground colors.
 */
@Composable
fun HemisphereLight(
    skyColor: Int = 0xFF87CEEB.toInt(),
    groundColor: Int = 0xFF362D1A.toInt(),
    intensity: Float = 1f,
    castShadow: Boolean = false
) {
    val lightingContext = LocalMateriaLightingContext.current
    
    DisposableEffect(skyColor, groundColor, intensity, castShadow) {
        val light = HemisphereLightImpl().apply {
            this.color = intToColor(skyColor)
            this.groundColor = intToColor(groundColor)
            this.intensity = intensity
            this.castShadow = castShadow
        }
        lightingContext?.addLight(light)
        
        onDispose {
            lightingContext?.removeLight(light)
        }
    }
}
