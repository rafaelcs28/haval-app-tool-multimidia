// GERADO por scripts/shared-runtime.mjs a partir de cluster-widgets/source/v1.0/shared — não edite à mão.
// Cartão de turn-by-turn e derivações do carro compartilhados com Default e Minimalist; `npm run build` regenera.
(function () {
"use strict";
// ---- source/v1.0/shared/utils/clusterAcFocus.js (createAcFocusPreference) ----
function createAcFocusPreference(getAndroid, warn = console.warn) {
    function bridge() {
        const candidate = getAndroid();
        if (!candidate || typeof candidate.getClusterAcFocus !== 'function' ||
            typeof candidate.saveClusterAcFocus !== 'function') {
            warn('[AC focus] Persistence bridge unavailable; using in-memory focus.');
            return null;
        }
        return candidate;
    }
    return {
        available() {
            try {
                const target = getAndroid();
                return !!target && typeof target.getClusterAcFocus === 'function' &&
                    typeof target.saveClusterAcFocus === 'function';
            } catch (_) { return false; }
        },
        restore() {
            try {
                const target = bridge();
                if (!target) return 'fan';
                const value = target.getClusterAcFocus();
                if (value === 'fan' || value === 'temp') return value;
                warn('[AC focus] Invalid stored focus; using fan.');
            } catch (error) { warn('[AC focus] Restore failed; using fan.', error); }
            return 'fan';
        },
        save(focus) {
            if (focus !== 'fan' && focus !== 'temp') return false;
            try {
                const target = bridge();
                if (target && target.saveClusterAcFocus(focus) === true) return true;
                warn('[AC focus] Focus remains in memory; persistence not accepted.');
            } catch (error) { warn('[AC focus] Save failed; focus remains in memory.', error); }
            return false;
        }
    };
}
// ---- source/v1.0/shared/car/carDerivations.js (getAdjustedSpeed) ----
function getAdjustedSpeed(rawSpeed, enableAdjustment = false, offsetPercent = 0.0) {
    const speed = parseFloat(rawSpeed) || 0.0;

    // Formula to match the physical instrument cluster
    const adjustedSpeed = speed * 1.07 - (speed / 180.0) * 0.02;

    const finalSpeed = enableAdjustment
        ? adjustedSpeed * (1.0 + (parseFloat(offsetPercent) / 100.0))
        : adjustedSpeed;

    return String(Math.floor(finalSpeed));
}
// ---- source/v1.0/shared/runtime/clusterVisibility.js (applyClusterVisibility) ----
function applyClusterVisibility(enabled) {
    document.documentElement.classList.toggle('cluster-disabled', enabled === false);
}
window.ApexShared = { createAcFocusPreference: createAcFocusPreference, getAdjustedSpeed: getAdjustedSpeed, applyClusterVisibility: applyClusterVisibility };
})();
