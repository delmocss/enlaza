package com.enlaza;

import org.junit.jupiter.api.Test;

/**
 * Comprueba que el contexto de Spring arranca contra una base de datos Postgres real
 * (via Testcontainers), no una en memoria. Requiere Docker en ejecucion.
 */
class EnlazaApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
