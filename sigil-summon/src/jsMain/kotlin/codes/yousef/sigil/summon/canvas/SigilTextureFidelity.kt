package codes.yousef.sigil.summon.canvas

import io.materia.core.scene.Material
import io.materia.material.MeshBasicMaterial
import io.materia.material.MeshStandardMaterial
import io.materia.renderer.TextureFilter
import io.materia.texture.Texture

internal object SigilTextureFidelity {
    fun configureMaterial(material: Material?) {
        when (material) {
            is MeshStandardMaterial -> {
                configureTexture(material.map)
                material.needsUpdate = true
            }
            is MeshBasicMaterial -> {
                configureTexture(material.map)
                material.needsUpdate = true
            }
        }
    }

    fun configureTexture(texture: Texture?) {
        texture ?: return
        texture.generateMipmaps = true
        texture.magFilter = TextureFilter.LINEAR
        texture.minFilter = TextureFilter.LINEAR_MIPMAP_LINEAR
        texture.anisotropy = 4f
        texture.markTextureNeedsUpdate()
    }
}
