package codes.yousef.sigil.summon.canvas

internal object SigilRendererPolicy {
    fun preferWebGlFirst(
        userAgent: String?,
        webdriver: Boolean,
        rendererOverride: String?
    ): Boolean {
        when (rendererOverride?.trim()?.lowercase()) {
            "webgl", "webgl2" -> return true
            "webgpu", "gpu" -> return false
        }

        val normalizedUserAgent = userAgent.orEmpty().lowercase()
        return webdriver ||
            normalizedUserAgent.contains("headlesschrome") ||
            normalizedUserAgent.contains("swiftshader")
    }
}
