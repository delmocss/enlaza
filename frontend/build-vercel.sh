#!/bin/sh
# Build de Vercel para el frontend estatico. Equivalente al stage de build
# del Dockerfile (que compila para nginx/Alpine), pero con dos diferencias
# reales:
#   1. El entorno de build de Vercel es glibc (no musl como Alpine), asi
#      que scripts/compile-css.sh se llama con el sufijo de asset normal
#      ("linux-x64"), no el "-musl" que usa el Dockerfile.
#   2. Aqui tambien se genera common/js/config.js a partir de la variable
#      de entorno ENLAZA_API_BASE del proyecto de Vercel, para que el
#      frontend desplegado apunte al backend real (Render) en vez de a
#      localhost. Esto SOLO reescribe el archivo en el build de Vercel;
#      el config.js del repo (con el valor localhost por defecto) no se
#      toca, asi que el desarrollo local con docker-compose sigue igual.
set -eu

# Falla ANTES de descargar/compilar nada si falta la variable: un proyecto
# de Vercel mal configurado (o duplicado sin copiar las env vars) falla al
# instante en vez de gastar minutos de build en cada reintento.
: "${ENLAZA_API_BASE:?falta la variable de entorno ENLAZA_API_BASE en el proyecto de Vercel (ej. https://enlaza-api.onrender.com/api)}"

sh scripts/compile-css.sh linux-x64

cat > common/js/config.js <<EOF
// Generado en el build de Vercel (build-vercel.sh) a partir de la
// variable de entorno ENLAZA_API_BASE del proyecto. No editar a mano:
// se sobreescribe en cada build. La version commiteada de este archivo
// (con el valor localhost) es la que se usa en desarrollo local.
window.ENLAZA_API_BASE = window.ENLAZA_API_BASE || '${ENLAZA_API_BASE}';
EOF
