# ---- Build (Tailwind CLI standalone: sin Node/npm, un solo binario) ----
FROM alpine:3.20 AS build
RUN apk add --no-cache curl libstdc++ libgcc
WORKDIR /frontend

COPY . .
# La imagen de build es Alpine (musl), no glibc: el asset "tailwindcss-linux-x64"
# normal es un binario glibc y falla en silencio con "not found" (ELF interpreter
# ausente) bajo musl -- hace falta el asset "-musl" dedicado. Ver
# scripts/compile-css.sh (compartido con el build de Vercel, que si es glibc).
RUN sh scripts/compile-css.sh linux-x64-musl

# ---- Runtime ----
FROM nginx:alpine
COPY --from=build /frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
