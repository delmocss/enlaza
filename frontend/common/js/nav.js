import { requireRole, logoutAndRedirect, homeFor } from './session.js';
import { escapeHtml } from './ui.js';
import { getTheme, toggleTheme } from './theme.js';
import { iconLogo, iconSun, iconMoon, iconLogout, iconChevronRight } from './icons.js';

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

function initials(nombre) {
    const parts = nombre.trim().split(/\s+/).filter(Boolean).slice(0, 2);
    return parts.map(part => part[0].toUpperCase()).join('') || '?';
}

/**
 * Pinta la cabecera comun (logo, enlaces por rol, tema, usuario y logout)
 * dentro del elemento con id="app-nav" que cada pagina ya trae en su HTML.
 * No hay router de cliente: cada enlace es una navegacion normal a otro
 * .html.
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
                <a href="${homeFor(me.role)}" class="flex items-center gap-2 text-lg font-semibold text-slate-900 dark:text-slate-100">
                    ${iconLogo('h-5 w-5 text-indigo-600 dark:text-indigo-400')}
                    Enlaza
                </a>
                <div class="flex gap-4">
                    ${links.map(link => `
                        <a href="${link.href}"
                           class="text-sm font-medium ${currentPath === link.href ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-600 hover:text-indigo-600 dark:text-slate-400 dark:hover:text-indigo-400'}">
                            ${link.label}
                        </a>
                    `).join('')}
                </div>
            </div>
            <div class="flex items-center gap-3">
                <button id="theme-toggle" type="button" title="Cambiar tema"
                        class="rounded-md p-2 text-slate-500 transition hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-800">
                    <span id="theme-toggle-icon">${getTheme() === 'dark' ? iconSun('h-4 w-4') : iconMoon('h-4 w-4')}</span>
                </button>
                <div class="flex items-center gap-2 border-l border-slate-200 pl-3 dark:border-slate-800">
                    <span class="flex h-7 w-7 items-center justify-center rounded-full bg-indigo-100 text-xs font-semibold text-indigo-700 dark:bg-indigo-500/20 dark:text-indigo-300">
                        ${initials(me.nombre)}
                    </span>
                    <span class="hidden text-sm text-slate-500 sm:inline dark:text-slate-400">${escapeHtml(me.nombre)} &middot; ${escapeHtml(me.role)}</span>
                </div>
                <button id="logout-button" type="button" class="btn-icon">
                    ${iconLogout('h-4 w-4')}
                    <span class="hidden sm:inline">Salir</span>
                </button>
            </div>
        </div>
    `;

    document.getElementById('logout-button').addEventListener('click', logoutAndRedirect);
    document.getElementById('theme-toggle').addEventListener('click', () => {
        const next = toggleTheme();
        document.getElementById('theme-toggle-icon').innerHTML = next === 'dark' ? iconSun('h-4 w-4') : iconMoon('h-4 w-4');
    });
}

/**
 * Pinta la traza de migas de pan dentro del elemento con id="breadcrumbs"
 * que las paginas anidadas traen en su HTML (sustituye al enlace suelto
 * "&larr; Volver" de antes). `items` es una lista de {label, href} en orden
 * de raiz a hoja; el ultimo elemento se pinta como texto actual, no enlace.
 */
export function renderBreadcrumbs(items) {
    const container = document.getElementById('breadcrumbs');
    if (!container) {
        return;
    }

    container.innerHTML = items.map((item, index) => {
        const isLast = index === items.length - 1;
        const separator = index > 0 ? iconChevronRight('h-3.5 w-3.5 text-slate-300 dark:text-slate-600') : '';
        const content = isLast
            ? `<span class="font-medium text-slate-700 dark:text-slate-300">${escapeHtml(item.label)}</span>`
            : `<a href="${item.href}" class="breadcrumb-link">${escapeHtml(item.label)}</a>`;
        return `${separator}${content}`;
    }).join('');
}

/**
 * Arranque comun de cada pagina protegida: valida sesion+rol, pinta la
 * cabecera y solo entonces ejecuta la logica propia de la pagina. Evita
 * repetir "const me = await requireRole(...); if (me) { renderNav(me);
 * init(); }" identico en cada .html.
 */
export async function bootstrapPage(role, init) {
    const me = await requireRole(role);
    if (me) {
        renderNav(me);
        await init();
    }
}
