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

    fun isSoftwareWebGpuAdapter(adapterSummary: String?): Boolean {
        val normalized = adapterSummary.orEmpty().lowercase()
        return normalized.contains("swiftshader") ||
            normalized.contains("llvmpipe") ||
            normalized.contains("software rasterizer") ||
            normalized.contains("software adapter")
    }
}
