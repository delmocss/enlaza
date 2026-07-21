import { apiFetch, ensureAccessToken, clearSession, logout } from './apiClient.js';

const HOME_BY_ROLE = {
    ADMIN: '/admin/empresas.html',
    CONTABLE: '/contable/dashboard.html',
    CLIENTE: '/cliente/facturas.html',
};

export function homeFor(role) {
    return HOME_BY_ROLE[role] ?? '/login.html';
}

/**
 * Guard de pagina: cada pantalla (salvo login.html) empieza llamando a
 * esto. Si no hay sesion valida o el rol no encaja, redirige y no
 * devuelve nada usable -- el resto del script de la pagina puede asumir
 * que si `requireRole` resuelve, el usuario esta autenticado y autorizado.
 */
export async function requireRole(...allowedRoles) {
    const refreshed = await ensureAccessToken();
    if (!refreshed) {
        window.location.href = '/login.html';
        return null;
    }

    let me;
    try {
        me = await apiFetch('/auth/me');
    } catch {
        window.location.href = '/login.html';
        return null;
    }

    if (!allowedRoles.includes(me.role)) {
        window.location.href = homeFor(me.role);
        return null;
    }

    return me;
}

export async function logoutAndRedirect() {
    await logout();
    window.location.href = '/login.html';
}

export function forceLogout() {
    clearSession();
    window.location.href = '/login.html';
}
