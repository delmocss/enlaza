import { ApiError } from './apiClient.js';
import { iconInbox, iconChevronUp, iconChevronDown, iconShield, iconCopy } from './icons.js';

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

/** Recorta un hash SHA-256 para mostrarlo en linea sin romper el layout ("a1b2c3d4...f0e1d2"). */
export function truncateHash(hash) {
    if (!hash) {
        return '—';
    }
    return hash.length <= 16 ? hash : `${hash.slice(0, 8)}…${hash.slice(-6)}`;
}

/**
 * Fila expandible con el desglose fiscal, las lineas y los hashes de una
 * factura -- la API ya devuelve todo esto en la misma respuesta de listado
 * (FacturaResponse), asi que no hace falta ninguna peticion nueva al
 * expandir. `colspan` debe igualar el numero de columnas de la tabla que
 * la contiene.
 */
export function renderFacturaDetailRow(factura, colspan) {
    const lineas = factura.lineas ?? [];
    return `
        <tr class="border-b border-stone-100 last:border-0 dark:border-stone-700/60">
            <td colspan="${colspan}" class="bg-stone-50/70 px-4 py-4 dark:bg-stone-900/30">
                <div class="grid gap-6 sm:grid-cols-2">
                    <div>
                        <h3 class="mb-2 text-xs font-semibold uppercase tracking-wide text-stone-500 dark:text-stone-400">Desglose fiscal</h3>
                        <dl class="grid grid-cols-2 gap-y-1 text-sm">
                            <dt class="text-stone-500 dark:text-stone-400">Base imponible</dt>
                            <dd class="text-right font-medium">${formatCurrency(factura.baseImponible)}</dd>
                            <dt class="text-stone-500 dark:text-stone-400">IVA</dt>
                            <dd class="text-right font-medium">${formatCurrency(factura.totalIva)}</dd>
                            <dt class="text-stone-500 dark:text-stone-400">IRPF</dt>
                            <dd class="text-right font-medium">${factura.totalIrpf > 0 ? '-' : ''}${formatCurrency(factura.totalIrpf)}</dd>
                            <dt class="font-semibold text-canela-700 dark:text-canela-400">Total</dt>
                            <dd class="text-right font-semibold text-canela-700 dark:text-canela-400">${formatCurrency(factura.total)}</dd>
                        </dl>
                    </div>
                    <div>
                        <h3 class="mb-2 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-stone-500 dark:text-stone-400">
                            ${iconShield('h-3.5 w-3.5')} Integridad de la cadena
                        </h3>
                        <dl class="space-y-1.5 text-xs">
                            <div class="flex items-center gap-2">
                                <dt class="w-24 shrink-0 text-stone-500 dark:text-stone-400">Hash anterior</dt>
                                <dd class="truncate font-mono" title="${escapeHtml(factura.hashAnterior ?? '')}">${escapeHtml(truncateHash(factura.hashAnterior))}</dd>
                                <button type="button" data-copy="${escapeHtml(factura.hashAnterior ?? '')}" class="btn-icon shrink-0 px-1.5 py-0.5" aria-label="Copiar hash anterior">${iconCopy()}</button>
                            </div>
                            <div class="flex items-center gap-2">
                                <dt class="w-24 shrink-0 text-stone-500 dark:text-stone-400">Hash actual</dt>
                                <dd class="truncate font-mono" title="${escapeHtml(factura.hashActual ?? '')}">${escapeHtml(truncateHash(factura.hashActual))}</dd>
                                <button type="button" data-copy="${escapeHtml(factura.hashActual ?? '')}" class="btn-icon shrink-0 px-1.5 py-0.5" aria-label="Copiar hash actual">${iconCopy()}</button>
                            </div>
                        </dl>
                    </div>
                </div>
                ${lineas.length ? `
                    <div class="mt-4">
                        <h3 class="mb-2 text-xs font-semibold uppercase tracking-wide text-stone-500 dark:text-stone-400">Lineas</h3>
                        <table class="w-full text-left text-xs">
                            <thead class="text-stone-500 dark:text-stone-400">
                                <tr>
                                    <th class="px-2 py-1 font-medium">Descripcion</th>
                                    <th class="px-2 py-1 font-medium">Cantidad</th>
                                    <th class="px-2 py-1 font-medium">Precio</th>
                                    <th class="px-2 py-1 font-medium">IVA %</th>
                                    <th class="px-2 py-1 font-medium">IRPF %</th>
                                    <th class="px-2 py-1 text-right font-medium">Importe</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${lineas.map(linea => `
                                    <tr class="border-t border-stone-100 dark:border-stone-700/60">
                                        <td class="px-2 py-1.5">${escapeHtml(linea.descripcion)}</td>
                                        <td class="px-2 py-1.5">${linea.cantidad}</td>
                                        <td class="px-2 py-1.5">${formatCurrency(linea.precioUnitario)}</td>
                                        <td class="px-2 py-1.5">${linea.porcentajeIva}%</td>
                                        <td class="px-2 py-1.5">${linea.porcentajeIrpf}%</td>
                                        <td class="px-2 py-1.5 text-right">${formatCurrency(linea.importeTotal)}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                ` : ''}
            </td>
        </tr>
    `;
}

/** Conecta los botones "copiar hash" (data-copy) de una fila de detalle ya insertada en el DOM. */
export function bindCopyButtons(container) {
    container.querySelectorAll('[data-copy]').forEach(button => {
        button.addEventListener('click', async (event) => {
            event.stopPropagation();
            try {
                await navigator.clipboard.writeText(button.dataset.copy);
                showToast('Hash copiado', 'success');
            } catch {
                showToast('No se pudo copiar el hash', 'error');
            }
        });
    });
}

/** Accessor de orden compartido por las tablas de factura de cliente/ y contable/: fechas se comparan como epoch, el resto por valor directo. */
export function facturaSortAccessor(factura, key) {
    if (key === 'fechaEmision') {
        return new Date(factura.fechaEmision).getTime();
    }
    return factura[key];
}

/** Alterna el estado de orden de una tabla: misma columna invierte direccion, columna nueva empieza en 'asc'. */
export function toggleSort(current, key) {
    if (current.key === key) {
        return { key, dir: current.dir === 'asc' ? 'desc' : 'asc' };
    }
    return { key, dir: 'asc' };
}

/** Copia y ordena `items` por `accessor(item, key)`, sin mutar el array original. */
export function sortItems(items, key, dir, accessor) {
    const factor = dir === 'asc' ? 1 : -1;
    return [...items].sort((a, b) => {
        const av = accessor(a, key);
        const bv = accessor(b, key);
        if (av < bv) return -1 * factor;
        if (av > bv) return 1 * factor;
        return 0;
    });
}

/** Icono de direccion para una cabecera ordenable; vacio si esa columna no es la activa. */
export function sortIndicator(sortState, key) {
    if (sortState.key !== key) {
        return '';
    }
    return sortState.dir === 'asc' ? iconChevronUp() : iconChevronDown();
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
    success: 'border-green-200 bg-white text-green-800 dark:border-green-900 dark:bg-stone-800 dark:text-green-300',
    error: 'border-red-200 bg-white text-red-800 dark:border-red-900 dark:bg-stone-800 dark:text-red-300',
    info: 'border-stone-200 bg-white text-stone-700 dark:border-stone-700 dark:bg-stone-800 dark:text-stone-300',
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
        backdrop.className = 'fixed inset-0 z-40 flex items-center justify-center bg-stone-900/40 p-4 backdrop-blur-sm dark:bg-stone-950/60';

        const panel = document.createElement('div');
        panel.className = 'w-full max-w-sm rounded-lg border border-stone-200 bg-white p-6 shadow-xl dark:border-stone-700 dark:bg-stone-800';
        panel.innerHTML = `
            <p class="text-sm text-stone-700 dark:text-stone-300">${escapeHtml(message)}</p>
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

/**
 * Formulario de alta/edicion compartido por las pantallas CRUD (empresas,
 * clientes): el mismo formulario sirve para crear y para editar. Gestiona el
 * modo (titulo, texto del boton, boton Cancelar), el envio (deshabilitar el
 * boton, mostrar el error de la API) y el reseteo tras guardar.
 *
 * - fill(item): vuelca la entidad a editar en los inputs.
 * - buildPayload(): lee los inputs y devuelve el cuerpo a enviar.
 * - save(payload, editingId): hace la llamada (POST si editingId es null, PUT si no).
 * - onSaved(): recarga la tabla tras guardar.
 * Devuelve { startEdit(item, title) }.
 */
export function setupEntityForm({ form, titleText, createLabel, fill, buildPayload, save, onSaved, errorFallback }) {
    const title = form.parentElement.querySelector('h2');
    const error = form.querySelector('.banner-error');
    const submitButton = form.querySelector('button[type="submit"]');
    const cancelButton = form.querySelector('button[type="button"]');
    let editingId = null;

    function reset() {
        editingId = null;
        form.reset();
        title.textContent = titleText;
        submitButton.textContent = createLabel;
        cancelButton.classList.add('hidden');
        showError(error, null);
    }

    function startEdit(item, editingTitle) {
        editingId = item.id;
        fill(item);
        title.textContent = editingTitle;
        submitButton.textContent = 'Guardar cambios';
        cancelButton.classList.remove('hidden');
        showError(error, null);
        form.scrollIntoView({ behavior: 'smooth' });
    }

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        showError(error, null);
        submitButton.disabled = true;
        try {
            await save(buildPayload(), editingId);
            reset();
            await onSaved();
        } catch (err) {
            showError(error, err instanceof ApiError ? err.message : errorFallback);
        } finally {
            submitButton.disabled = false;
        }
    });

    cancelButton.addEventListener('click', reset);

    return { startEdit };
}
