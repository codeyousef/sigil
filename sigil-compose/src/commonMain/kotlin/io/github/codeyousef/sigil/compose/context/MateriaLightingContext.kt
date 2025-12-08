package io.github.codeyousef.sigil.compose.context

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import io.materia.lighting.Light
import io.materia.lighting.LightingSystem
import io.materia.lighting.DefaultLightingSystem

/**
 * Context for managing lights in a Materia scene.
 * 
 * Lights in Materia are managed through a LightingSystem rather than
 * being part of the scene graph (Object3D hierarchy). This context
 * bridges Compose's lifecycle with the LightingSystem.
 */
class MateriaLightingContext(
    val lightingSystem: LightingSystem = DefaultLightingSystem()
) {
    private val managedLights = mutableListOf<Light>()
    
    /**
     * Add a light to the scene.
     */
    fun addLight(light: Light) {
        lightingSystem.addLight(light)
        managedLights.add(light)
    }
    
    /**
     * Remove a light from the scene.
     */
    fun removeLight(light: Light) {
        lightingSystem.removeLight(light)
        managedLights.remove(light)
    }
    
    /**
     * Update a light's properties in the lighting system.
     */
    fun updateLight(light: Light) {
        lightingSystem.updateLight(light)
    }
    
    /**
     * Clear all managed lights.
     */
    fun clear() {
        managedLights.forEach { light ->
            lightingSystem.removeLight(light)
        }
        managedLights.clear()
    }
    
    /**
     * Dispose the lighting system and all managed lights.
     */
    fun dispose() {
        clear()
        lightingSystem.dispose()
    }
}

/**
 * CompositionLocal for providing lighting context to composables.
 */
val LocalMateriaLightingContext = compositionLocalOf<MateriaLightingContext?> { null }

/**
 * Helper composable for registering a light with automatic cleanup.
 * 
 * @param factory Lambda to create the light
 * @param update Lambda to update the light properties on recomposition
 */
@Composable
inline fun <reified T : Light> MateriaLight(
    crossinline factory: () -> T,
    crossinline update: (T) -> Unit = {}
) {
    val lightingContext = LocalMateriaLightingContext.current
        ?: error("MateriaLight must be used within a MateriaLightingContext provider")
    
    DisposableEffect(Unit) {
        val light = factory()
        lightingContext.addLight(light)
        update(light)
        
        onDispose {
            lightingContext.removeLight(light)
        }
    }
}
