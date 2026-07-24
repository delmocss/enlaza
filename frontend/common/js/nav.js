import { requireRole, logoutAndRedirect, homeFor } from './session.js';
import { escapeHtml } from './ui.js';
import { mountThemeToggle } from './theme.js';
import { iconLogo, iconLogout, iconChevronRight } from './icons.js';

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
                <a href="${homeFor(me.role)}" class="flex items-center gap-2 text-lg font-semibold text-stone-900 dark:text-stone-100">
                    ${iconLogo('h-5 w-5 text-canela-600 dark:text-canela-400')}
                    Enlaza
                </a>
                <div class="flex gap-4">
                    ${links.map(link => `
                        <a href="${link.href}"
                           class="text-sm font-medium ${currentPath === link.href ? 'text-canela-700 dark:text-canela-400' : 'text-stone-600 hover:text-canela-700 dark:text-stone-400 dark:hover:text-canela-400'}">
                            ${link.label}
                        </a>
                    `).join('')}
                </div>
            </div>
            <div class="flex items-center gap-3">
                <button id="theme-toggle" type="button" title="Cambiar tema"
                        class="rounded-md p-2 text-stone-500 transition hover:bg-stone-100 dark:text-stone-400 dark:hover:bg-stone-700">
                    <span id="theme-toggle-icon"></span>
                </button>
                <div class="flex items-center gap-2 border-l border-stone-200 pl-3 dark:border-stone-700">
                    <span class="flex h-7 w-7 items-center justify-center rounded-full bg-canela-100 text-xs font-semibold text-canela-800 dark:bg-canela-500/20 dark:text-canela-300">
                        ${escapeHtml(initials(me.nombre))}
                    </span>
                    <span class="hidden text-sm text-stone-500 sm:inline dark:text-stone-400">${escapeHtml(me.nombre)} &middot; ${escapeHtml(me.role)}</span>
                </div>
                <button id="logout-button" type="button" class="btn-icon">
                    ${iconLogout('h-4 w-4')}
                    <span class="hidden sm:inline">Salir</span>
                </button>
            </div>
        </div>
        <div class="h-0.5 bg-gradient-to-r from-canela-300 via-canela-500 to-canela-300 dark:from-canela-800 dark:via-canela-600 dark:to-canela-800"></div>
    `;

    document.getElementById('logout-button').addEventListener('click', logoutAndRedirect);
    mountThemeToggle();
}

/**
 * Pinta la traza de migas de pan dentro del elemento con id="breadcrumbs"
 * que las paginas anidadas traen en su HTML (sustituye al enlace suelto
 * "&larr; Volver" de antes). `items` es una lista de {label, href?} en orden
 * de raiz a hoja; el ultimo elemento, y cualquier elemento sin `href` (p.
 * ej. un nombre de empresa que no tiene pagina propia a la que enlazar), se
 * pinta como texto plano, no enlace.
 */
export function renderBreadcrumbs(items) {
    const container = document.getElementById('breadcrumbs');
    if (!container) {
        return;
    }

    container.innerHTML = items.map((item, index) => {
        const isLast = index === items.length - 1;
        const separator = index > 0 ? iconChevronRight('h-3.5 w-3.5 text-stone-300 dark:text-stone-600') : '';
        const content = (isLast || !item.href)
            ? `<span class="font-medium text-stone-700 dark:text-stone-300">${escapeHtml(item.label)}</span>`
            : `<a href="${item.href}" class="breadcrumb-link">${escapeHtml(item.label)}</a>`;
        return `${separator}${content}`;
    }).join('');
}

/**
 * El bfcache del navegador puede restaurar una pagina protegida (nav ya
 * pintado, datos ya cargados) al navegar adelante/atras SIN volver a
 * ejecutar este script -- si la sesion se cerro o expiro mientras tanto
 * (p. ej. "Cerrar sesion" y luego el boton "Adelante" del navegador), se
 * veria la pagina autenticada tal cual quedo en memoria, sin comprobar
 * nada. Forzar una recarga completa cuando el evento trae persisted=true
 * hace que la pagina pase otra vez por bootstrapPage() y por tanto por
 * requireRole(), que expulsa a login.html si la sesion ya no es valida.
 */
function forceReloadOnBackForwardRestore() {
    window.addEventListener('pageshow', (event) => {
        if (event.persisted) {
            window.location.reload();
        }
    });
}

/**
 * Arranque comun de cada pagina protegida: valida sesion+rol, pinta la
 * cabecera y solo entonces ejecuta la logica propia de la pagina. Evita
 * repetir "const me = await requireRole(...); if (me) { renderNav(me);
 * init(); }" identico en cada .html.
 */
export async function bootstrapPage(role, init) {
    forceReloadOnBackForwardRestore();
    const me = await requireRole(role);
    if (me) {
        renderNav(me);
        await init();
    }
}
