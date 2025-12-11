package codes.yousef.sigil.compose.composition

import androidx.compose.runtime.Composable
import io.materia.core.math.Vector3
import io.materia.camera.PerspectiveCamera
import io.materia.camera.OrthographicCamera

/**
 * Perspective camera with field-of-view projection.
 */
@Composable
fun PerspectiveCamera(
    fov: Float = 75f,
    aspect: Float = 1.777778f,
    near: Float = 0.1f,
    far: Float = 1000f,
    position: Vector3 = Vector3(0f, 0f, 5f),
    lookAt: Vector3? = null,
    visible: Boolean = true,
    name: String = ""
) {
    MateriaNode(
        factory = { io.materia.camera.PerspectiveCamera(fov, aspect, near, far) },
        update = { camera ->
            camera.fov = fov
            camera.aspect = aspect
            camera.near = near
            camera.far = far
            camera.position.copy(position)
            lookAt?.let { camera.lookAt(it) }
            camera.visible = visible
            camera.name = name
            camera.updateProjectionMatrix()
        }
    )
}

/**
 * Orthographic camera with parallel projection.
 */
@Composable
fun OrthographicCamera(
    left: Float = -1f,
    right: Float = 1f,
    top: Float = 1f,
    bottom: Float = -1f,
    near: Float = 0.1f,
    far: Float = 1000f,
    position: Vector3 = Vector3(0f, 0f, 5f),
    lookAt: Vector3? = null,
    visible: Boolean = true,
    name: String = ""
) {
    MateriaNode(
        factory = { io.materia.camera.OrthographicCamera(left, right, top, bottom, near, far) },
        update = { camera ->
            camera.left = left
            camera.right = right
            camera.top = top
            camera.bottom = bottom
            camera.near = near
            camera.far = far
            camera.position.copy(position)
            lookAt?.let { camera.lookAt(it) }
            camera.visible = visible
            camera.name = name
            camera.updateProjectionMatrix()
        }
    )
}
