package codes.yousef.sigil.compose.composition

import io.materia.core.math.Color
import io.materia.geometry.primitives.BoxGeometry
import io.materia.geometry.primitives.SphereGeometry
import io.materia.geometry.primitives.PlaneGeometry
import io.materia.geometry.primitives.CylinderGeometry
import io.materia.geometry.ConeGeometry
import io.materia.geometry.primitives.TorusGeometry
import io.materia.geometry.CircleGeometry
import io.materia.geometry.primitives.RingGeometry
import io.materia.geometry.IcosahedronGeometry
import io.materia.geometry.OctahedronGeometry
import io.materia.geometry.TetrahedronGeometry
import io.materia.geometry.DodecahedronGeometry
import io.materia.geometry.BufferGeometry
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.GeometryParams

/**
 * Convert an ARGB int to Materia Color.
 */
internal fun intToColor(argb: Int): Color {
    val a = ((argb shr 24) and 0xFF) / 255f
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    return Color(r, g, b)
}

/**
 * Create a BufferGeometry based on the geometry type and parameters.
 */
internal fun createGeometry(type: GeometryType, params: GeometryParams): BufferGeometry {
    return when (type) {
        GeometryType.BOX -> BoxGeometry(
            width = params.width,
            height = params.height,
            depth = params.depth,
            widthSegments = params.widthSegments,
            heightSegments = params.heightSegments,
            depthSegments = params.widthSegments
        )
        GeometryType.SPHERE -> SphereGeometry(
            radius = params.radius,
            widthSegments = params.widthSegments.coerceAtLeast(8),
            heightSegments = params.heightSegments.coerceAtLeast(6)
        )
        GeometryType.PLANE -> PlaneGeometry(
            width = params.width,
            height = params.height,
            widthSegments = params.widthSegments,
            heightSegments = params.heightSegments
        )
        GeometryType.CYLINDER -> CylinderGeometry(
            radiusTop = params.radiusTop,
            radiusBottom = params.radiusBottom,
            height = params.height,
            radialSegments = params.radialSegments,
            heightSegments = params.heightSegments,
            openEnded = params.openEnded
        )
        GeometryType.CONE -> ConeGeometry(
            radius = params.radius,
            height = params.height,
            radialSegments = params.radialSegments,
            heightSegments = params.heightSegments,
            openEnded = params.openEnded
        )
        GeometryType.TORUS -> TorusGeometry(
            radius = params.radius,
            tube = params.tube,
            radialSegments = params.radialSegments,
            tubularSegments = params.tubularSegments
        )
        GeometryType.CIRCLE -> CircleGeometry(
            radius = params.radius,
            segments = params.radialSegments
        )
        GeometryType.RING -> RingGeometry(
            innerRadius = params.innerRadius,
            outerRadius = params.outerRadius,
            thetaSegments = params.radialSegments
        )
        GeometryType.ICOSAHEDRON -> IcosahedronGeometry(
            radius = params.radius,
            detail = params.detail
        )
        GeometryType.OCTAHEDRON -> OctahedronGeometry(
            radius = params.radius,
            detail = params.detail
        )
        GeometryType.TETRAHEDRON -> TetrahedronGeometry(
            radius = params.radius,
            detail = params.detail
        )
        GeometryType.DODECAHEDRON -> DodecahedronGeometry(
            radius = params.radius,
            detail = params.detail
        )
    }
}
