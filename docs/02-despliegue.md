# Fase 10 — Despliegue

Backend en **Render** (Docker + Postgres gestionado) y frontend en **Vercel** (sitio estático). Ambos leen del repositorio conectado por Git; despliega automáticamente en cada push a la rama configurada (recomendado: `main`, la rama estable del proyecto).

## Orden de despliegue (importa, por las dependencias entre URLs)

Hay una dependencia circular aparente entre CORS (el backend necesita saber el origen del frontend) y la URL de la API (el frontend necesita saber dónde está el backend). Se resuelve en este orden:

1. **Postgres** en Render.
2. **Backend** en Render (obtienes su URL, ej. `https://enlaza-api.onrender.com`).
3. **Frontend** en Vercel, usando la URL del paso 2 como `ENLAZA_API_BASE`.
4. Vuelve a Render y actualiza `CORS_ALLOWED_ORIGIN` con la URL real que Vercel asignó al frontend (paso 3). Render redespliega solo al cambiar una variable de entorno.

## 1. Postgres (Render)

Crea una instancia de PostgreSQL gestionada en Render. Anota el `Internal Database URL`, usuario y contraseña que Render genera — se usan en el paso siguiente. No hace falta ejecutar las migraciones a mano: Flyway (`spring.flyway.enabled: true`) las aplica solo al arrancar la app.

## 2. Backend (Render, Web Service con Docker)

Crea un **Web Service** apuntando al repositorio, rama `main`, `Dockerfile` en la raíz (no hace falta build command ni start command manuales, Render los toma del Dockerfile). Configura:

- **Health Check Path**: `/actuator/health` (endpoint público añadido en esta fase, expone solo `status: UP/DOWN`, nada de detalles internos).
- **Variables de entorno**:

| Variable | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host-interno-de-render>:5432/<db>` (del paso 1) |
| `DB_USERNAME` / `DB_PASSWORD` | Las que generó Render en el paso 1 |
| `JWT_SECRET` | **Generar uno nuevo**, nunca el de `.env.example` — ver aviso de seguridad abajo |
| `JWT_ACCESS_EXPIRATION_MINUTES` | `15` |
| `JWT_REFRESH_EXPIRATION_DAYS` | `7` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Igual que en `.env.example` — ver aviso abajo, **no rotar estas** |
| `DEMO_CONTABLE_EMAIL` / `DEMO_CONTABLE_PASSWORD` | Igual que en `.env.example` |
| `DEMO_CLIENTE_EMAIL` / `DEMO_CLIENTE_PASSWORD` | Igual que en `.env.example` |
| `SERVER_PORT` | `8080` (coincide con el `EXPOSE 8080` del Dockerfile) |
| `CORS_ALLOWED_ORIGIN` | La URL de Vercel del paso 3 (déjalo con un valor provisional y vuelve a este paso después) |

## 3. Frontend (Vercel)

Importa el repositorio en Vercel. En **Root Directory**, selecciona `frontend/` (el `vercel.json` de esa carpeta ya define `buildCommand` y las cabeceras `Cache-Control`, se detecta solo). Configura:

- **Variable de entorno**: `ENLAZA_API_BASE` = `https://<tu-backend-en-render>/api` (con el `/api` al final, igual que el valor local por defecto en `common/js/config.js`).

El build (`frontend/build-vercel.sh`) compila Tailwind con el binario `linux-x64` (el build de Vercel es glibc, no musl como el `Dockerfile` del frontend) y genera `common/js/config.js` con esa URL — el `config.js` del repo no se toca, solo se reescribe en este build.

**Cabeceras de caché**: `vercel.json` reproduce exactamente lo que hace `frontend/nginx.conf` en local (`Cache-Control: no-store` en `.html`, `no-cache` en `.js`/`.css`). Esto no es cosmético: es el fix real de seguridad de la Fase 8/9 que impide que el botón "atrás" del navegador muestre una página protegida ya cargada tras cerrar sesión. Verifica tras el primer despliegue que esas cabeceras llegan de verdad (herramientas de desarrollador del navegador, pestaña Network, mirar la respuesta de cualquier `.html`).

## 4. Cerrar el círculo

Vuelve al servicio de Render y actualiza `CORS_ALLOWED_ORIGIN` con la URL final de Vercel (ej. `https://enlaza.vercel.app`, o tu dominio propio si configuras uno). Guardar la variable dispara un redespliegue automático del backend.

## Aviso de seguridad: qué rotar y qué NO rotar

- **`JWT_SECRET`: rotar siempre, sin excepción.** El valor de `.env.example` es público (está en el repo). Es el secreto criptográfico que firma los access tokens — con el valor público, cualquiera podría forjar un token válido para **cualquier usuario y cualquier rol**, no solo iniciar sesión con las cuentas demo. Generar uno nuevo: `openssl rand -base64 48`.
- **Contraseñas de las cuentas demo (`ADMIN_PASSWORD`, `DEMO_CONTABLE_PASSWORD`, `DEMO_CLIENTE_PASSWORD`): NO rotar.** Es una decisión deliberada, no un descuido — la landing pública del frontend publica estas mismas credenciales a propósito, para que cualquiera que evalúe el portfolio pueda entrar con los 3 roles sin pedir acceso. Si en algún momento se decide meter datos no-demo reales en esta instancia, ahí sí hace falta rotarlas (y probablemente dejar de sembrarlas automáticamente).

## Verificación tras el despliegue

- `https://<backend>/actuator/health` → `{"status":"UP"}`.
- `https://<backend>/swagger-ui.html` → carga y el botón "Authorize" acepta un Bearer token.
- `https://<frontend>` → landing pública, login con los 3 roles demo, y repetir el escenario de "cerrar sesión → atrás en el navegador" para confirmar que las cabeceras de caché de Vercel bloquean la vuelta a una página protegida (mismo fix de seguridad que en local, ver la sección de restyle en el histórico del proyecto).

## Actualizar `main`

`main` se mantuvo casi vacío a propósito durante todo el desarrollo (todo el trabajo real vivía en `develop`, ver README de metodología). Este es el punto de corte: antes de configurar Render/Vercel para desplegar desde `main`, se trae `main` al día con `develop` (fast-forward, ya que `main` es antepasado directo — no hace falta merge commit).
