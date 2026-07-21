/**
 * escapeHtml es obligatorio en cualquier campo de texto que venga de la API
 * (razonSocial, nombre de cliente, descripcion de linea...) antes de
 * interpolarlo en un innerHTML de tabla: son datos introducidos por un
 * CONTABLE, no literales del propio codigo, asi que sin escapar serian una
 * inyeccion de HTML/script almacenada explotable por cualquier otro usuario
 * que abra esa misma pantalla.
 */
export function escapeHtml(value) {
    if (value === null || value === undefined) {
        return '';
    }
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

export function formatCurrency(value) {
    return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(value ?? 0);
}

export function formatDate(isoString) {
    if (!isoString) {
        return '';
    }
    return new Intl.DateTimeFormat('es-ES', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(isoString));
}

export function showError(container, message) {
    if (!container) {
        return;
    }
    if (!message) {
        container.classList.add('hidden');
        container.textContent = '';
        return;
    }
    container.textContent = message;
    container.classList.remove('hidden');
}

export function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
}
