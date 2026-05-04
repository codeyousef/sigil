package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.HitVolumeData
import codes.yousef.sigil.schema.HitVolumeShape
import codes.yousef.sigil.schema.InteractionMetadata
import io.materia.camera.PerspectiveCamera
import io.materia.core.math.Box3
import io.materia.core.math.Matrix4
import io.materia.core.math.Ray
import io.materia.core.math.Sphere
import io.materia.core.math.Vector2
import io.materia.core.math.Vector3
import io.materia.core.scene.Intersection
import io.materia.core.scene.Mesh
import io.materia.core.scene.Object3D

internal data class SigilInteractionHit(
    val intersection: Intersection,
    val node: Object3D
)

internal object SigilInteractionPicker {

    fun rayFromCamera(pointer: Vector2, camera: PerspectiveCamera): Ray {
        camera.updateMatrixWorld()
        camera.updateProjectionMatrix()

        val origin = Vector3().setFromMatrixColumn(camera.matrixWorld, 3)
        val direction = Vector3(pointer.x, pointer.y, 0.5f)
            .unproject(camera)
            .sub(origin)
            .normalize()

        return Ray(origin, direction)
    }

    fun intersectHitVolume(ray: Ray, node: Object3D, interaction: InteractionMetadata): SigilInteractionHit? {
        val hitVolume = interaction.hitVolume ?: return null
        if (hitVolume.shape == HitVolumeShape.MESH) return null

        node.updateWorldMatrix(updateParents = true, updateChildren = true)

        val localRay = rayToNodeLocalSpace(ray, node)
        val localPoint = when (hitVolume.shape) {
            HitVolumeShape.SPHERE -> intersectSphere(localRay, hitVolume)
            HitVolumeShape.BOX,
            HitVolumeShape.BOUNDING_BOX -> intersectBox(localRay, node, hitVolume)
            HitVolumeShape.MESH -> null
        } ?: return null

        val worldPoint = localPoint.applyMatrix4(node.matrixWorld)
        val distance = ray.origin.distanceTo(worldPoint)
        if (!distance.isFinite()) return null

        return SigilInteractionHit(
            intersection = Intersection(
                distance = distance,
                point = worldPoint,
                `object` = node
            ),
            node = node
        )
    }

    private fun rayToNodeLocalSpace(ray: Ray, node: Object3D): Ray {
        val inverseMatrix = Matrix4().copy(node.matrixWorld).invert()
        val localOrigin = ray.origin.clone().applyMatrix4(inverseMatrix)
        val localEnd = ray.at(1f).applyMatrix4(inverseMatrix)
        val localDirection = localEnd.sub(localOrigin).normalize()
        return Ray(localOrigin, localDirection)
    }

    private fun intersectSphere(ray: Ray, hitVolume: HitVolumeData): Vector3? {
        val radius = hitVolume.radius ?: return null
        if (radius <= 0f) return null

        return ray.intersectSpherePoint(
            Sphere(
                center = hitVolume.center.toVector3(),
                radius = radius
            )
        )
    }

    private fun intersectBox(ray: Ray, node: Object3D, hitVolume: HitVolumeData): Vector3? {
        val box = hitVolume.size?.let { size ->
            Box3.fromCenterAndSize(hitVolume.center.toVector3(), size.toVector3())
        } ?: inferLocalBoundingBox(node) ?: return null

        return ray.intersectBox(box)
    }

    private fun inferLocalBoundingBox(node: Object3D): Box3? {
        val nodeToLocal = Matrix4().copy(node.matrixWorld).invert()
        val box = Box3.empty()
        var foundGeometry = false

        node.traverse { candidate ->
            val mesh = candidate as? Mesh ?: return@traverse
            val geometryBox = mesh.geometry.computeBoundingBox()
            if (geometryBox.isEmpty()) return@traverse

            val meshToNode = Matrix4().multiplyMatrices(nodeToLocal, mesh.matrixWorld)
            val candidateBox = geometryBox.clone().applyMatrix4(meshToNode)
            box.expandByPoint(candidateBox.min)
            box.expandByPoint(candidateBox.max)
            foundGeometry = true
        }

        return if (foundGeometry && !box.isEmpty()) box else null
    }

    private fun List<Float>.toVector3(): Vector3 =
        Vector3(
            getOrNull(0) ?: 0f,
            getOrNull(1) ?: 0f,
            getOrNull(2) ?: 0f
        )
}
