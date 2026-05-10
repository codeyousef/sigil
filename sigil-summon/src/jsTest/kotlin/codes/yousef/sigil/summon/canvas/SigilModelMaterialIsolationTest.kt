package codes.yousef.sigil.summon.canvas

import io.materia.core.scene.Group
import io.materia.core.scene.Mesh
import io.materia.geometry.primitives.BoxGeometry
import io.materia.material.MeshStandardMaterial
import io.materia.texture.Texture2D
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame

class SigilModelMaterialIsolationTest {

    @Test
    fun isolateMutableMaterialsClonesPerModelInstanceAndSharesTextureResources() {
        val texture = Texture2D.fromImageData(
            width = 2,
            height = 2,
            data = ByteArray(16) { 255.toByte() }
        )
        val sharedMaterial = MeshStandardMaterial(map = texture)
        val firstInstance = modelInstance(sharedMaterial)
        val secondInstance = modelInstance(sharedMaterial)

        SigilModelMaterialIsolation.isolateMutableMaterials(firstInstance.root)
        SigilModelMaterialIsolation.isolateMutableMaterials(secondInstance.root)

        val firstClone = firstInstance.first.material as MeshStandardMaterial
        val secondClone = secondInstance.first.material as MeshStandardMaterial

        assertFalse(firstClone === sharedMaterial)
        assertFalse(secondClone === sharedMaterial)
        assertFalse(firstClone === secondClone)
        assertSame(texture, firstClone.map)
        assertSame(texture, secondClone.map)
        assertSame(firstClone, firstInstance.second.material)
    }

    private fun modelInstance(material: MeshStandardMaterial): ModelInstance {
        val root = Group()
        val first = Mesh(BoxGeometry(1f, 1f, 1f), material)
        val second = Mesh(BoxGeometry(1f, 1f, 1f), material)
        root.add(first)
        root.add(second)
        return ModelInstance(root, first, second)
    }

    private data class ModelInstance(
        val root: Group,
        val first: Mesh,
        val second: Mesh
    )
}
