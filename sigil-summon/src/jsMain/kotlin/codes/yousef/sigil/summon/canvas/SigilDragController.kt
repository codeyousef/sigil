package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.DragConstraintMode
import codes.yousef.sigil.schema.DragMetadata
import io.materia.core.math.Ray
import io.materia.core.math.Vector3
import kotlin.math.abs
import kotlin.math.sqrt

internal data class SigilDragSession(
    val mode: DragConstraintMode,
    val startNodePosition: Vector3,
    val startPointerPoint: Vector3,
    val planeNormal: Vector3,
    val planePoint: Vector3,
    val laneAxis: Vector3?,
    val laneStartValue: Float,
    val min: Float?,
    val max: Float?,
    val restrictHorizontal: Boolean,
    val restrictVertical: Boolean
)

internal object SigilDragController {
    fun begin(
        ray: Ray,
        nodePosition: Vector3,
        hitPoint: Vector3?,
        metadata: DragMetadata?
    ): SigilDragSession? {
        val mode = metadata?.mode ?: DragConstraintMode.CAMERA_PLANE
        val startNodePosition = nodePosition.clone()
        val planePoint = metadata?.planePoint?.toVector3() ?: hitPoint?.clone() ?: startNodePosition.clone()
        val planeNormal = metadata?.planeNormal?.toVector3()?.normalizedOrNull() ?: defaultPlaneNormal(mode, ray)
        val startPointerPoint = intersectRayPlane(ray, planeNormal, planePoint) ?: hitPoint?.clone() ?: planePoint.clone()
        val laneAxis = if (mode == DragConstraintMode.LANE) {
            (metadata?.laneAxis ?: listOf(1f, 0f, 0f)).toVector3().normalizedOrNull() ?: Vector3(1f, 0f, 0f)
        } else {
            null
        }

        return SigilDragSession(
            mode = mode,
            startNodePosition = startNodePosition,
            startPointerPoint = startPointerPoint,
            planeNormal = planeNormal,
            planePoint = planePoint,
            laneAxis = laneAxis,
            laneStartValue = laneAxis?.let { dot(startNodePosition, it) } ?: 0f,
            min = metadata?.min,
            max = metadata?.max,
            restrictHorizontal = mode == DragConstraintMode.HORIZONTAL,
            restrictVertical = mode == DragConstraintMode.VERTICAL
        )
    }

    fun positionFor(session: SigilDragSession, ray: Ray): Vector3? {
        val currentPointerPoint = intersectRayPlane(ray, session.planeNormal, session.planePoint) ?: return null
        val delta = subtract(currentPointerPoint, session.startPointerPoint)
        val laneAxis = session.laneAxis

        if (laneAxis != null) {
            val unclampedLaneValue = session.laneStartValue + dot(delta, laneAxis)
            val laneValue = clamp(unclampedLaneValue, session.min, session.max)
            val laneDelta = laneValue - session.laneStartValue
            return Vector3(
                session.startNodePosition.x + laneAxis.x * laneDelta,
                session.startNodePosition.y + laneAxis.y * laneDelta,
                session.startNodePosition.z + laneAxis.z * laneDelta
            )
        }

        val unconstrained = Vector3(
            session.startNodePosition.x + delta.x,
            session.startNodePosition.y + delta.y,
            session.startNodePosition.z + delta.z
        )

        return when {
            session.restrictHorizontal -> Vector3(
                unconstrained.x,
                session.startNodePosition.y,
                unconstrained.z
            )
            session.restrictVertical -> Vector3(
                unconstrained.x,
                unconstrained.y,
                session.startNodePosition.z
            )
            else -> unconstrained
        }
    }

    private fun defaultPlaneNormal(mode: DragConstraintMode, ray: Ray): Vector3 =
        when (mode) {
            DragConstraintMode.HORIZONTAL -> Vector3(0f, 1f, 0f)
            DragConstraintMode.VERTICAL -> Vector3(0f, 0f, 1f)
            DragConstraintMode.CAMERA_PLANE,
            DragConstraintMode.LANE -> ray.direction.clone().normalizedOrNull() ?: Vector3(0f, 0f, -1f)
        }

    private fun intersectRayPlane(ray: Ray, normal: Vector3, planePoint: Vector3): Vector3? {
        val denominator = dot(normal, ray.direction)
        if (abs(denominator) < EPSILON) return null

        val t = dot(subtract(planePoint, ray.origin), normal) / denominator
        if (t < 0f || !t.isFinite()) return null

        return ray.at(t)
    }

    private fun subtract(left: Vector3, right: Vector3): Vector3 =
        Vector3(left.x - right.x, left.y - right.y, left.z - right.z)

    private fun List<Float>.toVector3(): Vector3 =
        Vector3(
            getOrNull(0) ?: 0f,
            getOrNull(1) ?: 0f,
            getOrNull(2) ?: 0f
        )

    private fun Vector3.normalizedOrNull(): Vector3? {
        val length = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        if (length <= EPSILON || !length.isFinite()) return null
        return Vector3(x / length, y / length, z / length)
    }

    private fun dot(left: Vector3, right: Vector3): Float =
        left.x * right.x + left.y * right.y + left.z * right.z

    private fun clamp(value: Float, min: Float?, max: Float?): Float {
        var clamped = value
        min?.let { lower -> clamped = maxOf(clamped, lower) }
        max?.let { upper -> clamped = minOf(clamped, upper) }
        return clamped
    }

    private const val EPSILON = 0.00001f
}
