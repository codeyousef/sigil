package codes.yousef.sigil.compose.context

import androidx.compose.runtime.compositionLocalOf
import codes.yousef.sigil.compose.canvas.MateriaCanvasState

/**
 * CompositionLocal for providing the active MateriaCanvasState.
 */
val LocalMateriaCanvasState = compositionLocalOf<MateriaCanvasState?> { null }
