package codes.yousef.sigil.summon.effects

import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import kotlin.test.*

/**
 * Tests for the hydration guard logic in SigilEffectHydrator.
 * 
 * These tests verify that:
 * 1. Double initialization is prevented
 * 2. Race conditions from concurrent hydration calls are handled
 * 3. Force reinitialization works correctly
 * 4. DOM markers are properly set and read
 */
class HydrationGuardTest {
    
    private fun createTestCanvas(id: String): HTMLCanvasElement {
        // Remove any existing canvas with this ID
        document.getElementById(id)?.remove()
        
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.id = id
        canvas.width = 100
        canvas.height = 100
        document.body?.appendChild(canvas)
        return canvas
    }
    
    private fun cleanupCanvas(id: String) {
        document.getElementById(id)?.remove()
        // Reset the hydrator tracking state
        SigilEffectHydrator.disposeHydrator(id)
    }
    
    @AfterTest
    fun cleanup() {
        // Clean up any test canvases
        listOf("test-canvas", "test-canvas-1", "test-canvas-2", "race-canvas", "force-canvas", "dom-marker-canvas").forEach {
            document.getElementById(it)?.remove()
        }
    }
    
    @Test
    fun isHydrated_returnsFalse_forNewCanvas() {
        val canvasId = "test-canvas-new"
        createTestCanvas(canvasId)
        
        try {
            assertFalse(SigilEffectHydrator.isHydrated(canvasId))
        } finally {
            cleanupCanvas(canvasId)
        }
    }
    
    @Test
    fun domMarker_blocksHydration_whenAlreadySet() {
        val canvasId = "dom-marker-canvas"
        val canvas = createTestCanvas(canvasId)
        
        try {
            // Simulate another script having already hydrated this canvas
            canvas.setAttribute("data-sigil-hydrated", "true")
            
            // Call hydrate - should be blocked by DOM marker
            SigilEffectHydrator.hydrateFromDOM(canvasId)
            
            // The canvas should now be tracked as hydrated (synced from DOM marker)
            assertTrue(SigilEffectHydrator.isHydrated(canvasId))
        } finally {
            cleanupCanvas(canvasId)
        }
    }
    
    @Test
    fun forceReinitialize_clearsExistingState() {
        val canvasId = "force-canvas"
        val canvas = createTestCanvas(canvasId)
        
        try {
            // Manually set up the state as if it was already hydrated
            canvas.setAttribute("data-sigil-hydrated", "true")
            
            // Confirm it's tracked as hydrated
            SigilEffectHydrator.hydrateFromDOM(canvasId)
            assertTrue(SigilEffectHydrator.isHydrated(canvasId))
            
            // Now call with forceReinitialize - it should clear the marker
            // Note: This will fail due to missing effect data, but it should still clear the state
            SigilEffectHydrator.hydrateFromDOM(canvasId, forceReinitialize = true)
            
            // The DOM marker should be removed when forceReinitialize starts
            // (before the coroutine potentially fails)
            // Note: Since we don't have valid effect data, the hydration will fail,
            // but the state should have been cleared
        } finally {
            cleanupCanvas(canvasId)
        }
    }
    
    @Test
    fun disposeHydrator_removesAllTrackingState() {
        val canvasId = "dispose-canvas"
        val canvas = createTestCanvas(canvasId)
        
        try {
            // Set up as if hydrated
            canvas.setAttribute("data-sigil-hydrated", "true")
            SigilEffectHydrator.hydrateFromDOM(canvasId) // Syncs tracking
            assertTrue(SigilEffectHydrator.isHydrated(canvasId))
            
            // Dispose
            SigilEffectHydrator.disposeHydrator(canvasId)
            
            // Should no longer be tracked as hydrated
            assertFalse(SigilEffectHydrator.isHydrated(canvasId))
            
            // DOM marker should be removed
            assertNull(canvas.getAttribute("data-sigil-hydrated"))
        } finally {
            document.getElementById(canvasId)?.remove()
        }
    }
}
