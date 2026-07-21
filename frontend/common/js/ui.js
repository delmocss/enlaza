import { ApiError } from './apiClient.js';

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

export function renderBadge(isPositive, positiveLabel, negativeLabel) {
    return isPositive
        ? `<span class="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">${positiveLabel}</span>`
        : `<span class="rounded-full bg-slate-200 px-2 py-0.5 text-xs font-medium text-slate-600">${negativeLabel}</span>`;
}

/**
 * Envuelve el patron repetido de las acciones de fila de las tablas:
 * confirmar (si aplica), deshabilitar el boton mientras la peticion esta en
 * vuelo, ejecutar la accion, avisar en caso de error, y siempre reactivar el
 * boton al terminar (si `onSuccess` ya reemplazo la fila -- p. ej. recargando
 * la tabla -- reactivar un nodo desmontado es inofensivo).
 */
export async function confirmAndRun({ button, confirmMessage, action, onSuccess, errorFallback }) {
    if (confirmMessage && !confirm(confirmMessage)) {
        return;
    }
    button.disabled = true;
    try {
        await action();
        await onSuccess();
    } catch (error) {
        alert(error instanceof ApiError ? error.message : errorFallback);
    } finally {
        button.disabled = false;
    }
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
