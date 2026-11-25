# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [1.0.0]

Primera versión estable.

### Añadido
- Multi-tenancy: un ADMIN gestiona varias empresas, cada una con sus series, clientes y contables asignados.
- RBAC con tres roles: ADMIN, CONTABLE y CLIENTE.
- Numeración correlativa de facturas sin huecos, con bloqueo pesimista y prueba de concurrencia (10 hilos).
- Cadena de hashes SHA-256 entre facturas de una misma serie y endpoint de verificación de integridad que detecta la primera manipulación.
- Autenticación JWT con rotación de refresh tokens de un solo uso y detección de reutilización.
- Exportación de facturas a PDF.
- Documentación de la API con Swagger UI y colección de Postman.
- 81 tests (unitarios e integración con PostgreSQL real vía Testcontainers) y CI en GitHub Actions.
- Informe de cobertura con JaCoCo.
- Dependabot para Maven, GitHub Actions y Docker.

### Límites conocidos
- No implementa ni certifica el cumplimiento legal de VeriFactu.
- Sin alta de usuarios desde la UI, sin borradores de factura y sin tabla de auditoría.
