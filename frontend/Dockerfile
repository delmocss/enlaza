# ---- Build (Tailwind CLI standalone: sin Node/npm, un solo binario) ----
FROM alpine:3.20 AS build
RUN apk add --no-cache curl libstdc++ libgcc
WORKDIR /frontend

COPY . .
# La imagen de build es Alpine (musl), no glibc: el asset "tailwindcss-linux-x64"
# normal es un binario glibc y falla en silencio con "not found" (ELF interpreter
# ausente) bajo musl -- hace falta el asset "-musl" dedicado.
RUN curl -sLo /usr/local/bin/tailwindcss \
        https://github.com/tailwindlabs/tailwindcss/releases/latest/download/tailwindcss-linux-x64-musl \
    && chmod +x /usr/local/bin/tailwindcss \
    && tailwindcss -i common/css/input.css -o common/css/styles.css --minify

# ---- Runtime ----
FROM nginx:alpine
COPY --from=build /frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
