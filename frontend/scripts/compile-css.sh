#!/bin/sh
# Descarga el binario standalone de Tailwind CLI y compila common/css/input.css.
# Compartido por frontend/Dockerfile (build en Alpine/musl) y
# frontend/build-vercel.sh (build en glibc): la unica diferencia real entre
# ambos entornos es el sufijo del asset de la release, pasado como $1, para
# no tener la misma logica de descarga/compilacion duplicada y en riesgo de
# desincronizarse entre los dos.
set -eu

variant="${1:?uso: compile-css.sh <sufijo-del-asset, ej. linux-x64 o linux-x64-musl>}"
bin="$(mktemp)"

# -f: que curl falle con un error claro si GitHub responde con un status
# no-2xx (limite de peticiones, asset renombrado, caida del servicio), en
# vez de guardar en silencio un cuerpo de error como si fuera el binario --
# eso fallaria despues con un "Exec format error" opaco al intentar
# ejecutarlo, escondiendo la causa real (descarga fallida).
curl -sLfo "$bin" "https://github.com/tailwindlabs/tailwindcss/releases/latest/download/tailwindcss-${variant}"
chmod +x "$bin"
"$bin" -i common/css/input.css -o common/css/styles.css --minify
