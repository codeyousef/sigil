package codes.yousef.sigil.summon.canvas

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SigilRendererPolicyTest {
    @Test
    fun preferWebGlFirst_honorsExplicitWebGlOverride() {
        assertTrue(
            SigilRendererPolicy.preferWebGlFirst(
                userAgent = "Mozilla/5.0",
                webdriver = false,
                rendererOverride = "webgl"
            )
        )
    }

    @Test
    fun preferWebGlFirst_honorsExplicitWebGpuOverride() {
        assertFalse(
            SigilRendererPolicy.preferWebGlFirst(
                userAgent = "HeadlessChrome",
                webdriver = true,
                rendererOverride = "webgpu"
            )
        )
    }

    @Test
    fun preferWebGlFirst_detectsHeadlessValidation() {
        assertTrue(
            SigilRendererPolicy.preferWebGlFirst(
                userAgent = "Mozilla/5.0 HeadlessChrome/126.0.0.0",
                webdriver = false,
                rendererOverride = null
            )
        )
    }

    @Test
    fun preferWebGlFirst_detectsWebdriverValidation() {
        assertTrue(
            SigilRendererPolicy.preferWebGlFirst(
                userAgent = "Mozilla/5.0 Chrome/126.0.0.0",
                webdriver = true,
                rendererOverride = null
            )
        )
    }

    @Test
    fun isSoftwareWebGpuAdapter_detectsSwiftShader() {
        assertTrue(
            SigilRendererPolicy.isSoftwareWebGpuAdapter(
                "Backend=WEBGPU, Device='0xc0de', Vendor=google, Architecture=swiftshader"
            )
        )
    }

    @Test
    fun isSoftwareWebGpuAdapter_allowsHardwareAdapter() {
        assertFalse(
            SigilRendererPolicy.isSoftwareWebGpuAdapter(
                "Backend=WEBGPU, Vendor=amd, Architecture=rdna3"
            )
        )
    }
}
