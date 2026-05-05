package codes.yousef.sigil.summon.canvas

import io.materia.core.scene.Material
import io.materia.core.scene.Mesh
import io.materia.material.Material as BaseMaterial
import io.materia.material.MeshBasicMaterial
import io.materia.material.MeshStandardMaterial
import io.materia.renderer.TextureFilter
import io.materia.texture.Texture

internal object SigilTextureFidelity {
    fun configureGltfGeometryAttributes(mesh: Mesh) {
        val geometry = mesh.geometry
        if (geometry.hasAttribute("TEXCOORD_0") && !geometry.hasAttribute("uv")) {
            geometry.getAttribute("TEXCOORD_0")?.let { geometry.setAttribute("uv", it) }
        }
        if (geometry.hasAttribute("COLOR_0") && !geometry.hasAttribute("color")) {
            geometry.getAttribute("COLOR_0")?.let { geometry.setAttribute("color", it) }
        }

        if (!geometry.hasAttribute("color") && !geometry.hasAttribute("COLOR_0")) return

        when (val material = mesh.material) {
            is MeshStandardMaterial -> {
                if (material.map == null) {
                    material.vertexColors = true
                    material.needsUpdate = true
                }
            }
            is MeshBasicMaterial -> {
                if (material.map == null) {
                    material.vertexColors = true
                    material.needsUpdate = true
                }
            }
            is BaseMaterial -> {
                material.vertexColors = true
                material.needsUpdate = true
            }
        }
    }

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
