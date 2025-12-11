/**
 * Sigil Hydration Client
 * 
 * This script is automatically served by sigilStaticAssets() and hydrates
 * Sigil effect canvases on the client side.
 * 
 * In production, this file should be replaced with the actual compiled
 * sigil-summon-js output from the build process.
 */

(function() {
    'use strict';
    
    console.log('[Sigil] Hydration client loaded');
    
    // Initialize Sigil effects when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeSigil);
    } else {
        initializeSigil();
    }
    
    function initializeSigil() {
        console.log('[Sigil] Initializing effect canvases');
        
        // Find all canvases with data-sigil-effects attribute
        const canvases = document.querySelectorAll('canvas[data-sigil-effects]');
        
        if (canvases.length === 0) {
            console.log('[Sigil] No effect canvases found');
            return;
        }
        
        console.log(`[Sigil] Found ${canvases.length} effect canvas(es)`);
        
        // In production, this would:
        // 1. Parse the data-sigil-effects JSON
        // 2. Initialize WebGPU or WebGL renderer
        // 3. Create and start effect composition
        // 4. Set up user interaction handlers
        
        canvases.forEach((canvas, index) => {
            try {
                const effectsData = canvas.getAttribute('data-sigil-effects');
                if (effectsData) {
                    console.log(`[Sigil] Canvas ${index}: ${effectsData.substring(0, 100)}...`);
                    // TODO: Parse and hydrate effects
                }
            } catch (error) {
                console.error(`[Sigil] Error initializing canvas ${index}:`, error);
            }
        });
    }
    
    // Export for external access if needed
    if (typeof window !== 'undefined') {
        window.Sigil = {
            version: '0.2.1.0',
            initialized: true
        };
    }
})();
