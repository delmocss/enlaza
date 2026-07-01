# Enlaza

Sistema de facturación electrónica multi-empresa con cadena de integridad verificable (hash-chain), inspirado en los requisitos técnicos de VeriFactu (España). El nombre viene de su mecanismo central: cada factura queda "enlazada" a la anterior de su serie mediante un hash.

> Proyecto en construcción. La documentación completa (arquitectura, API, despliegue) se añadirá al cierre del desarrollo.

Este proyecto es un ejercicio de portfolio backend y **no implementa ni certifica el cumplimiento legal de VeriFactu**.

## Ejecutar en local (provisional, se ampliará en la Fase 8)

Copia `.env.example` a `.env` y exporta esas variables antes de arrancar (por ejemplo con `docker-compose up`, que ya las lee de `.env`). Ejecutar con `mvn spring-boot:run` sin exportarlas primero fallará al arrancar: `JWT_SECRET` y las credenciales de los usuarios semilla (`ADMIN_EMAIL`/`ADMIN_PASSWORD`, `DEMO_CONTABLE_*`, `DEMO_CLIENTE_*`) son obligatorias desde la Fase 3.

