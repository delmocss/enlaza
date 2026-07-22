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
