// Cliente HTTP para /api. El accessToken vive SOLO en memoria (variable de
// modulo): al ser un sitio multi-pagina clasico (sin router del lado del
// cliente), cada navegacion recarga el JS desde cero, asi que la memoria
// nunca sobrevive entre paginas. Por eso el refreshToken (unico valor que
// necesita sobrevivir) se guarda en sessionStorage, y cada pagina arranca
// pidiendo un accessToken nuevo con ese refreshToken via ensureAccessToken()
// -- ver session.js. Esto evita guardar el accessToken en Web Storage.

const API_BASE = window.ENLAZA_API_BASE;
const REFRESH_TOKEN_KEY = 'enlaza.refreshToken';

let accessToken = null;

export class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

function getRefreshToken() {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
}

function storeTokens(tokens) {
    accessToken = tokens.accessToken;
    sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function clearSession() {
    accessToken = null;
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
}

async function readErrorMessage(response) {
    try {
        const body = await response.json();
        return body.message || response.statusText;
    } catch {
        return response.statusText;
    }
}

async function publicPost(path, body) {
    const response = await fetch(`${API_BASE}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });
    if (!response.ok) {
        throw new ApiError(response.status, await readErrorMessage(response));
    }
    return response.json();
}

export async function login(email, password) {
    const tokens = await publicPost('/auth/login', { email, password });
    storeTokens(tokens);
}

export async function logout() {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
        await fetch(`${API_BASE}/auth/logout`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken }),
        }).catch(() => {});
    }
    clearSession();
}

/**
 * Pide un accessToken nuevo con el refreshToken guardado. Se llama una vez
 * al cargar cada pagina (ver session.js#requireRole) y tambien cuando una
 * llamada de apiFetch recibe 401 con un accessToken ya caducado en memoria.
 */
export async function ensureAccessToken() {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
        return false;
    }
    try {
        const tokens = await publicPost('/auth/refresh', { refreshToken });
        storeTokens(tokens);
        return true;
    } catch {
        clearSession();
        return false;
    }
}

export async function apiFetch(path, options = {}) {
    return doFetch(path, options, true);
}

async function doFetch(path, options, allowRetry) {
    const headers = new Headers(options.headers || {});
    if (accessToken) {
        headers.set('Authorization', `Bearer ${accessToken}`);
    }
    if (options.body && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

    if (response.status === 401 && allowRetry) {
        const refreshed = await ensureAccessToken();
        if (refreshed) {
            return doFetch(path, options, false);
        }
        window.location.href = '/login.html';
        throw new ApiError(401, 'Sesion expirada');
    }

    if (!response.ok) {
        throw new ApiError(response.status, await readErrorMessage(response));
    }

    if (response.status === 204) {
        return null;
    }
    return response.json();
}

export async function apiFetchBlob(path) {
    const headers = new Headers();
    if (accessToken) {
        headers.set('Authorization', `Bearer ${accessToken}`);
    }
    const response = await fetch(`${API_BASE}${path}`, { headers });
    if (!response.ok) {
        throw new ApiError(response.status, await readErrorMessage(response));
    }
    return response.blob();
}
