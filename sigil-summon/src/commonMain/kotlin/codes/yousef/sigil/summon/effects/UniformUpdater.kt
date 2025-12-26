package codes.yousef.sigil.summon.effects

import codes.yousef.sigil.schema.effects.UniformValue

/**
 * Shared helper to map schema UniformValue to concrete shader uniform setters.
 */
fun applyUniformValue(
    name: String,
    value: UniformValue,
    setFloat: (String, Float) -> Unit,
    setVec2: (String, Float, Float) -> Unit,
    setVec3: (String, Float, Float, Float) -> Unit,
    setVec4: (String, Float, Float, Float, Float) -> Unit,
    setMat3: (String, FloatArray) -> Unit,
    setMat4: (String, FloatArray) -> Unit
) {
    when (value) {
        is UniformValue.FloatValue -> setFloat(name, value.value)
        is UniformValue.IntValue -> setFloat(name, value.value.toFloat())
        is UniformValue.Vec2Value -> setVec2(name, value.value.x, value.value.y)
        is UniformValue.Vec3Value -> setVec3(name, value.value.x, value.value.y, value.value.z)
        is UniformValue.Vec4Value -> setVec4(name, value.value.x, value.value.y, value.value.z, value.value.w)
        is UniformValue.Mat3Value -> setMat3(name, value.values.toFloatArray())
        is UniformValue.Mat4Value -> setMat4(name, value.values.toFloatArray())
    }
}
