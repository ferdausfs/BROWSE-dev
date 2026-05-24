/*
 * Placeholder for Eruda v3.4.3
 *
 * Replace this file with the real eruda.js (download from:
 *   https://cdn.jsdelivr.net/npm/eruda@3.4.3/eruda.js
 *   or https://github.com/liriliri/eruda/releases
 * ).
 *
 * This stub is just so the asset path resolves during builds before you
 * drop in the real library. It defines a minimal eruda.init() no-op
 * so the injection bootstrap does not throw.
 */
(function () {
    if (typeof window === 'undefined') return;
    if (window.eruda && typeof window.eruda.init === 'function') return;
    window.eruda = {
        init: function () {
            console.warn('[Eruda placeholder] eruda.js has not been replaced with the real library yet.');
        },
        version: 'placeholder'
    };
})();
