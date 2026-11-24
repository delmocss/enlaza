// Cliente HTTP para /api. accessToken y refreshToken se guardan los dos en
// sessionStorage (el accessToken tambien se cachea en memoria para no leer
// storage en cada peticion). Antes el accessToken vivia solo en memoria y
// cada pagina refrescaba el refreshToken al cargar -- pero el refreshToken
// es de un solo uso (rotacion atomica en el backend) y ya vivia en
// sessionStorage de todos modos, asi que ese diseño no anadia proteccion
// real y sí forzaba una rotacion en CADA navegacion. Eso disparaba el
// mecanismo de deteccion de robo del backend (RefreshTokenService.rotate())
// con dos pestañas normales y corrientes: sessionStorage se hereda al abrir
// un enlace en una pestaña nueva (ctrl/click), asi que ambas rotaban el
// mismo token casi a la vez y una perdia la carrera, lo que revoca TODAS
// las sesiones. Guardando tambien el accessToken, una navegacion normal no
// dispara ninguna rotacion -- solo se refresca de verdad cuando el
// accessToken realmente ha caducado (via el reintento-en-401 de abajo),
// que ocurre con muchisima menos frecuencia y estrecha muchisimo la
// ventana de carrera entre pestañas.
const API_BASE = window.ENLAZA_API_BASE;
const ACCESS_TOKEN_KEY = 'enlaza.accessToken';
const REFRESH_TOKEN_KEY = 'enlaza.refreshToken';
const REFRESH_LOCK_NAME = 'enlaza-refresh-token';

let accessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY);

export class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

function getRefreshToken() {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
}

/**
 * Comprobacion barata y sin red de "hay algo guardado que parece una
 * sesion". Usado solo por index.html para decidir, sin arriesgarse al
 * redirect-a-login-html de apiFetch en 401, si debe intentar resolver la
 * pagina de inicio del usuario o mostrar la landing publica directamente.
 */
export function hasStoredSession() {
    return Boolean(getRefreshToken());
}

function storeTokens(tokens) {
    accessToken = tokens.accessToken;
    sessionStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function clearSession() {
    accessToken = null;
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
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

// El backend vive en un plan gratuito que se duerme tras ~15 min sin trafico y tarda
// 30-90 s en despertar. Sin esto, el primer login de la jornada deja el boton
// esperando en silencio. warmUpApi() lo despierta mientras el usuario aun escribe.
const HEALTH_URL = `${API_BASE.replace(/\/api\/?$/, '')}/actuator/health`;

export function warmUpApi() {
    // no-cors: la respuesta no importa, solo que el servidor reciba la peticion.
    fetch(HEALTH_URL, { mode: 'no-cors', cache: 'no-store' }).catch(() => {});
}

// status 0 = sin respuesta (red caida o tiempo agotado).
async function publicPost(path, body, { timeoutMs = 20000 } = {}) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
        const response = await fetch(`${API_BASE}${path}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
            signal: controller.signal,
        });
        if (!response.ok) {
            throw new ApiError(response.status, await readErrorMessage(response));
        }
        return await response.json();
    } catch (error) {
        if (error instanceof ApiError) {
            throw error;
        }
        throw new ApiError(0, error.name === 'AbortError' ? 'timeout' : 'network');
    } finally {
        clearTimeout(timer);
    }
}

const RETRYABLE_STATUSES = new Set([0, 502, 503, 504]);
const LOGIN_ATTEMPTS = 4;
const LOGIN_ATTEMPT_TIMEOUT_MS = 30000;
const LOGIN_RETRY_DELAY_MS = 3000;

/**
 * Inicia sesion. Si el backend esta despertando (sin respuesta, 502/503/504) reintenta
 * hasta LOGIN_ATTEMPTS veces; una respuesta real (401, 400...) se devuelve al instante.
 * onWaiting se llama la primera vez que hay que esperar mas de la cuenta.
 */
export async function login(email, password, { onWaiting } = {}) {
    let notified = false;
    const notify = () => {
        if (!notified && onWaiting) {
            notified = true;
            onWaiting();
        }
    };
    const slowTimer = setTimeout(notify, 4000);
    try {
        for (let attempt = 1; ; attempt++) {
            try {
                const tokens = await publicPost('/auth/login', { email, password }, { timeoutMs: LOGIN_ATTEMPT_TIMEOUT_MS });
                storeTokens(tokens);
                return;
            } catch (error) {
                const retryable = error instanceof ApiError && RETRYABLE_STATUSES.has(error.status);
                if (!retryable || attempt >= LOGIN_ATTEMPTS) {
                    throw error;
                }
                notify();
                await new Promise((resolve) => setTimeout(resolve, LOGIN_RETRY_DELAY_MS));
            }
        }
    } finally {
        clearTimeout(slowTimer);
    }
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

async function rotateRefreshToken() {
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

/**
 * Pide un accessToken nuevo con el refreshToken guardado. Solo se llama
 * cuando una peticion recibe 401 de verdad (ver doFetch) -- nunca por
 * adelantado. navigator.locks sigue sirviendo de defensa en profundidad
 * dentro de una MISMA pestaña (dos peticiones que expiran a la vez no
 * disparan dos rotaciones concurrentes), aunque no puede cerrar del todo
 * la ventana entre pestañas distintas si sus accessToken caducan casi
 * exactamente a la vez -- eso ya es un caso mucho mas raro que "cada clic".
 */
export async function ensureAccessToken() {
    if (!getRefreshToken()) {
        return false;
    }
    if (!('locks' in navigator)) {
        return rotateRefreshToken();
    }
    return navigator.locks.request(REFRESH_LOCK_NAME, () => rotateRefreshToken());
}

async function doFetch(path, options, responseType, allowRetry) {
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
            return doFetch(path, options, responseType, false);
        }
        window.location.href = '/login.html';
        throw new ApiError(401, 'Sesion expirada');
    }

    if (!response.ok) {
        throw new ApiError(response.status, await readErrorMessage(response));
    }

    if (responseType === 'blob') {
        return response.blob();
    }
    if (response.status === 204) {
        return null;
    }
    return response.json();
}

export async function apiFetch(path, options = {}) {
    return doFetch(path, options, 'json', true);
}

export async function apiFetchBlob(path) {
    return doFetch(path, {}, 'blob', true);
}
