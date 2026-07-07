package com.enlaza;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base comun para tests de integracion: arranca un Postgres real via
 * Testcontainers (nunca H2), compartido por todas las subclases del mismo
 * proceso de test (contenedor unico, arranque perezoso al primer uso).
 *
 * El contenedor se arranca manualmente en un bloque estatico y NO se anota
 * con @Container: esa anotacion ata el ciclo de vida del contenedor a la
 * primera clase de test que lo usa y lo para al terminarla, rompiendo el
 * contexto de Spring cacheado (con el mismo DataSource) que reutilizan las
 * demas subclases.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
