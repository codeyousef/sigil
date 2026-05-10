package codes.yousef.sigil.summon.canvas

import io.materia.core.scene.Material
import io.materia.core.scene.Mesh
import io.materia.core.scene.Object3D
import io.materia.material.MeshBasicMaterial
import io.materia.material.MeshStandardMaterial
import io.materia.material.Material as BaseMaterial

internal object SigilModelMaterialIsolation {
    fun isolateMutableMaterials(root: Object3D) {
        val clonedMaterials = mutableMapOf<Material, Material>()

        root.traverse { node ->
            val mesh = node as? Mesh ?: return@traverse
            val material = mesh.material ?: return@traverse
            mesh.material = clonedMaterials.getOrPut(material) {
                cloneMutableMaterial(material)
            }
        }
    }

    private fun cloneMutableMaterial(material: Material): Material =
        when (material) {
            is MeshStandardMaterial -> material.copy()
            is MeshBasicMaterial -> material.clone()
            is BaseMaterial -> material.clone()
            else -> material
        }
}
