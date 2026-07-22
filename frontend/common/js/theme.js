import { iconSun, iconMoon } from './icons.js';

const STORAGE_KEY = 'enlaza.theme';

export function getTheme() {
    return document.documentElement.classList.contains('dark') ? 'dark' : 'light';
}

export function setTheme(theme) {
    document.documentElement.classList.toggle('dark', theme === 'dark');
    try {
        localStorage.setItem(STORAGE_KEY, theme);
    } catch (error) {
        // Sin almacenamiento persistente el tema sigue funcionando en la
        // pestana actual, solo no sobrevive a una recarga.
    }
}

export function toggleTheme() {
    const next = getTheme() === 'dark' ? 'light' : 'dark';
    setTheme(next);
    return next;
}

/**
 * Conecta el boton de alternancia de tema (icono + click) a los elementos
 * #theme-toggle / #theme-toggle-icon del documento. Compartido entre nav.js
 * (paginas con sesion) y login.html (que no pinta la cabecera comun), para
 * no mantener dos copias del mismo "sembrar icono, escuchar click, cambiar
 * icono".
 */
export function mountThemeToggle() {
    const button = document.getElementById('theme-toggle');
    const icon = document.getElementById('theme-toggle-icon');
    if (!button || !icon) {
        return;
    }

    icon.innerHTML = getTheme() === 'dark' ? iconSun('h-4 w-4') : iconMoon('h-4 w-4');
    button.addEventListener('click', () => {
        const next = toggleTheme();
        icon.innerHTML = next === 'dark' ? iconSun('h-4 w-4') : iconMoon('h-4 w-4');
    });
}
