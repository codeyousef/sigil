package codes.yousef.sigil.summon.effects

import kotlin.test.*

/**
 * Unit tests for the hydration guard logic pattern.
 * 
 * These tests verify the synchronous guard pattern prevents race conditions
 * by testing the core logic without browser dependencies.
 * 
 * The key insight is that the guard checks (hydratedCanvases, hydrationInProgress)
 * run SYNCHRONOUSLY before launching any coroutine. This means even if two calls
 * happen "simultaneously", the second call will see the in-progress marker set by
 * the first call before it returns.
 */
class HydrationGuardLogicTest {
    
    /**
     * Simulates the guard logic pattern used in SigilEffectHydrator.hydrateFromDOM().
     * This is a testable extraction of the core synchronization pattern.
     */
    class HydrationGuardSimulator {
        // Mirrors the actual tracking sets in SigilEffectHydrator
        private val hydratedCanvases = mutableSetOf<String>()
        private val hydrationInProgress = mutableSetOf<String>()
        
        // Track how many times initialization actually ran
        var initializationCount = 0
            private set
        
        // For testing: track rejected attempts
        var rejectedAsAlreadyHydrated = 0
            private set
        var rejectedAsInProgress = 0
            private set
        
        /**
         * Simulates the guard logic from hydrateFromDOM().
         * Returns true if hydration was started, false if blocked.
         */
        fun tryStartHydration(
            canvasId: String,
            forceReinitialize: Boolean = false
        ): Boolean {
            // SYNCHRONOUS checks BEFORE any async work (mirrors actual implementation)
            
            // Check if already fully hydrated
            if (canvasId in hydratedCanvases && !forceReinitialize) {
                rejectedAsAlreadyHydrated++
                return false
            }
            
            // Check if hydration is already in progress
            if (canvasId in hydrationInProgress && !forceReinitialize) {
                rejectedAsInProgress++
                return false
            }
            
            // Handle force reinitialization
            if (forceReinitialize) {
                hydratedCanvases.remove(canvasId)
                hydrationInProgress.remove(canvasId)
            }
            
            // Mark as in-progress SYNCHRONOUSLY before any async work
            hydrationInProgress.add(canvasId)
            
            return true
        }
        
        /**
         * Simulates successful completion of hydration (called from coroutine).
         */
        fun completeHydration(canvasId: String) {
            initializationCount++
            hydrationInProgress.remove(canvasId)
            hydratedCanvases.add(canvasId)
        }
        
        /**
         * Simulates failed hydration (called from coroutine).
         */
        fun failHydration(canvasId: String) {
            hydrationInProgress.remove(canvasId)
        }
        
        fun isHydrated(canvasId: String): Boolean = canvasId in hydratedCanvases
        fun isInProgress(canvasId: String): Boolean = canvasId in hydrationInProgress
        
        fun reset() {
            hydratedCanvases.clear()
            hydrationInProgress.clear()
            initializationCount = 0
            rejectedAsAlreadyHydrated = 0
            rejectedAsInProgress = 0
        }
    }
    
    private val guard = HydrationGuardSimulator()
    
    @BeforeTest
    fun setup() {
        guard.reset()
    }
    
    @Test
    fun singleHydration_shouldSucceed() {
        val started = guard.tryStartHydration("canvas-1")
        
        assertTrue(started, "First hydration should start")
        assertTrue(guard.isInProgress("canvas-1"), "Should be marked as in-progress")
        
        guard.completeHydration("canvas-1")
        
        assertTrue(guard.isHydrated("canvas-1"), "Should be marked as hydrated")
        assertFalse(guard.isInProgress("canvas-1"), "Should no longer be in-progress")
        assertEquals(1, guard.initializationCount, "Should have initialized once")
    }
    
    @Test
    fun doubleHydration_secondShouldBeBlocked() {
        // First hydration
        val first = guard.tryStartHydration("canvas-1")
        guard.completeHydration("canvas-1")
        
        // Second attempt
        val second = guard.tryStartHydration("canvas-1")
        
        assertTrue(first, "First should succeed")
        assertFalse(second, "Second should be blocked")
        assertEquals(1, guard.initializationCount, "Should only initialize once")
        assertEquals(1, guard.rejectedAsAlreadyHydrated, "Should record rejection")
    }
    
    @Test
    fun concurrentHydration_secondShouldBeBlockedByInProgress() {
        // Simulate race condition: two calls before either completes
        
        // First call - starts hydration
        val first = guard.tryStartHydration("canvas-1")
        assertTrue(first, "First should succeed")
        assertTrue(guard.isInProgress("canvas-1"), "Should be in-progress")
        
        // Second call - should be blocked because first is in-progress
        val second = guard.tryStartHydration("canvas-1")
        assertFalse(second, "Second should be blocked by in-progress check")
        assertEquals(1, guard.rejectedAsInProgress, "Should record rejection as in-progress")
        
        // First completes
        guard.completeHydration("canvas-1")
        
        assertEquals(1, guard.initializationCount, "Should only initialize once")
    }
    
    @Test
    fun forceReinitialize_shouldBypassGuards() {
        // First hydration
        guard.tryStartHydration("canvas-1")
        guard.completeHydration("canvas-1")
        
        assertTrue(guard.isHydrated("canvas-1"))
        assertEquals(1, guard.initializationCount)
        
        // Force reinitialize
        val reinit = guard.tryStartHydration("canvas-1", forceReinitialize = true)
        assertTrue(reinit, "Force reinitialize should succeed")
        
        guard.completeHydration("canvas-1")
        assertEquals(2, guard.initializationCount, "Should have initialized twice")
    }
    
    @Test
    fun forceReinitialize_shouldBypassInProgressCheck() {
        // Start hydration but don't complete
        guard.tryStartHydration("canvas-1")
        assertTrue(guard.isInProgress("canvas-1"))
        
        // Force reinitialize should still work
        val reinit = guard.tryStartHydration("canvas-1", forceReinitialize = true)
        assertTrue(reinit, "Force reinitialize should bypass in-progress check")
    }
    
    @Test
    fun failedHydration_shouldNotMarkAsHydrated() {
        val started = guard.tryStartHydration("canvas-1")
        assertTrue(started)
        assertTrue(guard.isInProgress("canvas-1"))
        
        // Simulate failure
        guard.failHydration("canvas-1")
        
        assertFalse(guard.isHydrated("canvas-1"), "Should not be marked as hydrated after failure")
        assertFalse(guard.isInProgress("canvas-1"), "Should not be in-progress after failure")
        assertEquals(0, guard.initializationCount, "Should not count as initialized")
    }
    
    @Test
    fun failedHydration_shouldAllowRetry() {
        // First attempt fails
        guard.tryStartHydration("canvas-1")
        guard.failHydration("canvas-1")
        
        // Retry should be allowed
        val retry = guard.tryStartHydration("canvas-1")
        assertTrue(retry, "Retry after failure should be allowed")
        
        guard.completeHydration("canvas-1")
        assertEquals(1, guard.initializationCount)
    }
    
    @Test
    fun multipleCanvases_shouldBeIndependent() {
        guard.tryStartHydration("canvas-1")
        guard.tryStartHydration("canvas-2")
        guard.tryStartHydration("canvas-3")
        
        assertTrue(guard.isInProgress("canvas-1"))
        assertTrue(guard.isInProgress("canvas-2"))
        assertTrue(guard.isInProgress("canvas-3"))
        
        guard.completeHydration("canvas-1")
        guard.completeHydration("canvas-2")
        guard.completeHydration("canvas-3")
        
        assertEquals(3, guard.initializationCount, "All three should initialize")
    }
    
    @Test
    fun raceConditionSimulation_synchronousGuardPreventsDoubleInit() {
        // This test simulates the exact race condition that was causing the bug:
        // Auto-hydration and manual hydration both call hydrateFromDOM() nearly simultaneously
        
        // The key insight is that tryStartHydration() runs SYNCHRONOUSLY,
        // so the second call sees the in-progress marker immediately.
        // This is the FIX - previously the checks were INSIDE the coroutine,
        // so both coroutines could pass the checks before either set the marker.
        
        val initCountBefore = guard.initializationCount
        
        // First call - simulates auto-hydration
        val call1Started = guard.tryStartHydration("race-canvas")
        assertTrue(call1Started, "First call should start")
        assertTrue(guard.isInProgress("race-canvas"), "Should be marked in-progress immediately")
        
        // Second call - simulates manual hydration happening "simultaneously"
        // Key: This happens BEFORE the first call's coroutine completes
        val call2Started = guard.tryStartHydration("race-canvas")
        assertFalse(call2Started, "Second call should be BLOCKED by in-progress check")
        assertEquals(1, guard.rejectedAsInProgress, "Second call should be rejected as in-progress")
        
        // First call completes (in reality this would be in a coroutine)
        guard.completeHydration("race-canvas")
        
        // Verify only ONE initialization occurred
        assertEquals(
            1, 
            guard.initializationCount - initCountBefore,
            "Only one initialization should occur even with concurrent calls"
        )
        assertTrue(guard.isHydrated("race-canvas"))
        assertFalse(guard.isInProgress("race-canvas"))
    }
}
