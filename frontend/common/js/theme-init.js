// Script clasico (no type="module"): debe ejecutarse de forma sincrona
// ANTES del primer pintado para evitar un flash del tema incorrecto. Un
// script de modulo se difiere hasta despues del parseo del documento, lo
// que llegaria tarde. Referenciado igual que config.js: un <script src>
// normal en <head>, sin async/defer.
(function () {
    var STORAGE_KEY = 'enlaza.theme';
    var stored = null;
    try {
        stored = localStorage.getItem(STORAGE_KEY);
    } catch (error) {
        // Almacenamiento no disponible (p. ej. modo privado estricto): se
        // usa la preferencia del sistema operativo como unico criterio.
    }
    var isDark = stored ? stored === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
    document.documentElement.classList.toggle('dark', isDark);
})();
