package codes.yousef.sigil.summon.canvas

import io.materia.core.math.Vector2
import io.materia.core.scene.Mesh
import io.materia.geometry.BufferAttribute
import io.materia.material.MeshBasicMaterial
import io.materia.material.MeshStandardMaterial
import io.materia.renderer.TextureWrap
import io.materia.texture.Texture
import io.materia.texture.Texture2D
import kotlin.math.floor

internal object SigilWebGlTextureBaker {
    fun bakeBaseColorTexture(mesh: Mesh): Boolean {
        val material = mesh.material ?: return false
        val texture = when (material) {
            is MeshStandardMaterial -> material.map
            is MeshBasicMaterial -> material.map as? Texture2D
            else -> null
        } ?: return false
        val materialUserData = when (material) {
            is MeshStandardMaterial -> material.userData
            is MeshBasicMaterial -> material.userData
            else -> return false
        }

        val geometry = mesh.geometry
        val bakeKey = "${texture.id}:${texture.version}"
        val geometryBakeKey = "sigilWebGlTextureBakeKey:${geometry.uuid}"
        if (materialUserData[geometryBakeKey] == bakeKey) return false

        val uv = geometry.getAttribute("uv") ?: return false
        val position = geometry.getAttribute("position") ?: return false
        val textureData = texture.getData() ?: return false
        if (texture.width <= 0 || texture.height <= 0 || textureData.size < texture.width * texture.height * 4) {
            return false
        }

        val sourceColor = geometry.getAttribute("color")
        val baked = FloatArray(position.count * 3)
        val uvVector = Vector2()

        for (vertexIndex in 0 until position.count) {
            uvVector.set(uv.getX(vertexIndex), uv.getY(vertexIndex))
            texture.transformUv(uvVector)
            val texel = sampleTexture(texture, textureData, uvVector.x, uvVector.y)
            val colorOffset = vertexIndex * 3

            val sourceRed = sourceColor?.getX(vertexIndex) ?: 1f
            val sourceGreen = sourceColor?.getY(vertexIndex) ?: 1f
            val sourceBlue = sourceColor?.getZ(vertexIndex) ?: 1f

            baked[colorOffset] = sourceRed * texel.red
            baked[colorOffset + 1] = sourceGreen * texel.green
            baked[colorOffset + 2] = sourceBlue * texel.blue
        }

        geometry.setAttribute("color", BufferAttribute(baked, 3))
        materialUserData[geometryBakeKey] = bakeKey
        mesh.userData["sigilWebGlTextureBakeKey"] = bakeKey
        return true
    }

    private fun sampleTexture(texture: Texture2D, data: ByteArray, u: Float, v: Float): Texel {
        val x = wrapCoordinate(u, texture.wrapS) * (texture.width - 1).coerceAtLeast(0)
        val y = wrapCoordinate(v, texture.wrapT) * (texture.height - 1).coerceAtLeast(0)

        val x0 = floor(x).toInt().coerceIn(0, texture.width - 1)
        val y0 = floor(y).toInt().coerceIn(0, texture.height - 1)
        val x1 = (x0 + 1).coerceAtMost(texture.width - 1)
        val y1 = (y0 + 1).coerceAtMost(texture.height - 1)
        val tx = x - x0
        val ty = y - y0

        val top = mix(readTexel(data, texture.width, x0, y0), readTexel(data, texture.width, x1, y0), tx)
        val bottom = mix(readTexel(data, texture.width, x0, y1), readTexel(data, texture.width, x1, y1), tx)
        return mix(top, bottom, ty)
    }

    private fun wrapCoordinate(value: Float, wrap: TextureWrap): Float {
        return when (wrap) {
            TextureWrap.CLAMP_TO_EDGE -> value.coerceIn(0f, 1f)
            TextureWrap.REPEAT -> value - floor(value)
            TextureWrap.MIRRORED_REPEAT -> {
                val repeated = value - floor(value)
                val segment = floor(value).toInt()
                if (segment % 2 == 0) repeated else 1f - repeated
            }
        }
    }

    private fun readTexel(data: ByteArray, width: Int, x: Int, y: Int): Texel {
        val offset = (y * width + x) * 4
        return Texel(
            red = data[offset].toUnsignedFloat(),
            green = data[offset + 1].toUnsignedFloat(),
            blue = data[offset + 2].toUnsignedFloat()
        )
    }

    private fun Byte.toUnsignedFloat(): Float =
        (toInt() and 0xFF) / 255f

    private fun mix(left: Texel, right: Texel, amount: Float): Texel =
        Texel(
            red = left.red + (right.red - left.red) * amount,
            green = left.green + (right.green - left.green) * amount,
            blue = left.blue + (right.blue - left.blue) * amount
        )

    private data class Texel(
        val red: Float,
        val green: Float,
        val blue: Float
    )
}
