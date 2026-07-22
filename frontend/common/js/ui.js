import { ApiError } from './apiClient.js';
import { iconInbox } from './icons.js';

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
        ? `<span class="badge-positive">${positiveLabel}</span>`
        : `<span class="badge-neutral">${negativeLabel}</span>`;
}

/** Fila de "tabla vacia" con icono, para sustituir el texto plano suelto de antes. */
export function renderEmptyRow(colspan, message) {
    return `
        <tr>
            <td colspan="${colspan}" class="table-cell">
                <div class="empty-state">
                    ${iconInbox('h-8 w-8')}
                    <p class="text-sm">${escapeHtml(message)}</p>
                </div>
            </td>
        </tr>
    `;
}

/** Filas placeholder mostradas mientras el primer fetch de una tabla esta en vuelo. */
export function renderSkeletonRows(count, columns) {
    const cells = Array.from({ length: columns }, () => '<td class="table-cell"><div class="skeleton-block h-4 w-full"></div></td>').join('');
    return Array.from({ length: count }, () => `<tr class="table-row">${cells}</tr>`).join('');
}

let toastContainer = null;

function getToastContainer() {
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'pointer-events-none fixed bottom-4 right-4 z-50 flex w-full max-w-sm flex-col gap-2';
        document.body.appendChild(toastContainer);
    }
    return toastContainer;
}

const TOAST_TONES = {
    success: 'border-green-200 bg-white text-green-800 dark:border-green-900 dark:bg-slate-900 dark:text-green-300',
    error: 'border-red-200 bg-white text-red-800 dark:border-red-900 dark:bg-slate-900 dark:text-red-300',
    info: 'border-slate-200 bg-white text-slate-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300',
};

/** Notificacion flotante y autodesvanecida. Sustituye a alert() para no bloquear la interfaz. */
export function showToast(message, type = 'info') {
    const container = getToastContainer();
    const toast = document.createElement('div');
    const tone = TOAST_TONES[type] ?? TOAST_TONES.info;
    toast.className = `pointer-events-auto rounded-lg border px-4 py-3 text-sm shadow-lg transition duration-200 ease-out translate-y-2 opacity-0 ${tone}`;
    toast.textContent = message;
    container.appendChild(toast);

    requestAnimationFrame(() => {
        toast.classList.remove('translate-y-2', 'opacity-0');
    });

    setTimeout(() => {
        toast.classList.add('opacity-0');
        setTimeout(() => toast.remove(), 200);
    }, 4000);
}

/**
 * Modal de confirmacion accesible (foco en el boton principal, Escape y
 * click fuera cancelan). Sustituye a confirm(), que bloquea el hilo y no se
 * puede estilar. Devuelve una Promise<boolean> con la eleccion del usuario.
 */
export function showConfirmModal({ message, confirmLabel = 'Confirmar', cancelLabel = 'Cancelar', danger = false }) {
    return new Promise((resolve) => {
        const backdrop = document.createElement('div');
        backdrop.className = 'fixed inset-0 z-40 flex items-center justify-center bg-slate-900/40 p-4 backdrop-blur-sm dark:bg-slate-950/60';

        const panel = document.createElement('div');
        panel.className = 'w-full max-w-sm rounded-lg border border-slate-200 bg-white p-6 shadow-xl dark:border-slate-800 dark:bg-slate-900';
        panel.innerHTML = `
            <p class="text-sm text-slate-700 dark:text-slate-300">${escapeHtml(message)}</p>
            <div class="mt-5 flex justify-end gap-3">
                <button type="button" data-role="cancel" class="btn-secondary">${escapeHtml(cancelLabel)}</button>
                <button type="button" data-role="confirm" class="${danger ? 'btn-danger' : 'btn-primary'}">${escapeHtml(confirmLabel)}</button>
            </div>
        `;
        backdrop.appendChild(panel);
        document.body.appendChild(backdrop);

        function close(result) {
            backdrop.remove();
            document.removeEventListener('keydown', onKeydown);
            resolve(result);
        }
        function onKeydown(event) {
            if (event.key === 'Escape') {
                close(false);
            }
        }

        panel.querySelector('[data-role="cancel"]').addEventListener('click', () => close(false));
        panel.querySelector('[data-role="confirm"]').addEventListener('click', () => close(true));
        backdrop.addEventListener('click', (event) => {
            if (event.target === backdrop) {
                close(false);
            }
        });
        document.addEventListener('keydown', onKeydown);
        panel.querySelector('[data-role="confirm"]').focus();
    });
}

/**
 * Envuelve el patron repetido de las acciones de fila de las tablas:
 * confirmar (si aplica, via modal propio en vez de confirm()), deshabilitar
 * el boton mientras la peticion esta en vuelo, ejecutar la accion, avisar
 * en caso de error (via toast en vez de alert()), y siempre reactivar el
 * boton al terminar (si `onSuccess` ya reemplazo la fila -- p. ej.
 * recargando la tabla -- reactivar un nodo desmontado es inofensivo).
 */
export async function confirmAndRun({ button, confirmMessage, action, onSuccess, successMessage, errorFallback }) {
    if (confirmMessage) {
        const confirmed = await showConfirmModal({ message: confirmMessage, danger: true });
        if (!confirmed) {
            return;
        }
    }
    button.disabled = true;
    try {
        await action();
        await onSuccess();
        if (successMessage) {
            showToast(successMessage, 'success');
        }
    } catch (error) {
        showToast(error instanceof ApiError ? error.message : errorFallback, 'error');
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
