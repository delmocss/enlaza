import { apiFetch, logout } from './apiClient.js';

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
 * Sin roles (0 argumentos), solo exige sesion valida -- lo usa resolveHome.
 *
 * No refresca el accessToken por adelantado: apiFetch ya lo hace por su
 * cuenta si /auth/me responde 401 (accessToken caducado o inexistente), y
 * si no hay refreshToken tampoco redirige sola a login.html -- este metodo
 * no necesita duplicar esa logica.
 */
export async function requireRole(...allowedRoles) {
    let me;
    try {
        me = await apiFetch('/auth/me');
    } catch {
        return null;
    }

    if (allowedRoles.length > 0 && !allowedRoles.includes(me.role)) {
        window.location.href = homeFor(me.role);
        return null;
    }

    return me;
}

export async function logoutAndRedirect() {
    await logout();
    window.location.href = '/login.html';
}

/** Usado solo por index.html: entra a la pantalla de inicio de cada rol si hay sesion valida, o a login si no. */
export async function resolveHome() {
    const me = await requireRole();
    if (me) {
        window.location.href = homeFor(me.role);
    }
}

/**
 * Lee parametros de la query string que una pagina necesita para funcionar
 * (p. ej. empresaId, serieId). Si falta alguno, redirige a `fallbackUrl` en
 * vez de dejar la pagina renderizar a medias con un id undefined.
 */
export function requireQueryParams(names, fallbackUrl) {
    const params = new URLSearchParams(window.location.search);
    const values = {};
    for (const name of names) {
        const value = params.get(name);
        if (!value) {
            window.location.href = fallbackUrl;
            return null;
        }
        values[name] = value;
    }
    return values;
}
