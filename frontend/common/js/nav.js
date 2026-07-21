import { logoutAndRedirect } from './session.js';

const LINKS_BY_ROLE = {
    ADMIN: [
        { href: '/admin/empresas.html', label: 'Empresas' },
    ],
    CONTABLE: [
        { href: '/contable/dashboard.html', label: 'Empresas' },
    ],
    CLIENTE: [
        { href: '/cliente/facturas.html', label: 'Mis facturas' },
    ],
};

/**
 * Pinta la cabecera comun (enlaces por rol + usuario + logout) dentro del
 * elemento con id="app-nav" que cada pagina ya trae en su HTML. No hay
 * router de cliente: cada enlace es una navegacion normal a otro .html.
 */
export function renderNav(me) {
    const nav = document.getElementById('app-nav');
    if (!nav) {
        return;
    }

    const links = LINKS_BY_ROLE[me.role] ?? [];
    const currentPath = window.location.pathname;

    nav.innerHTML = `
        <div class="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
            <div class="flex items-center gap-6">
                <span class="text-lg font-semibold text-slate-900">Enlaza</span>
                <div class="flex gap-4">
                    ${links.map(link => `
                        <a href="${link.href}"
                           class="text-sm font-medium ${currentPath === link.href ? 'text-indigo-600' : 'text-slate-600 hover:text-indigo-600'}">
                            ${link.label}
                        </a>
                    `).join('')}
                </div>
            </div>
            <div class="flex items-center gap-4">
                <span class="text-sm text-slate-500">${me.nombre} &middot; ${me.role}</span>
                <button id="logout-button" type="button"
                        class="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium text-slate-700 hover:bg-slate-50">
                    Cerrar sesion
                </button>
            </div>
        </div>
    `;

    document.getElementById('logout-button').addEventListener('click', logoutAndRedirect);
}
