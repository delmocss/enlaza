package com.enlaza.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI enlazaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enlaza API")
                        .version("v0.1")
                        .description("""
                                API de facturacion electronica multi-empresa con cadena de integridad \
                                verificable entre facturas. Proyecto de portfolio inspirado en los \
                                requisitos tecnicos de VeriFactu (Espana); no implementa ni certifica \
                                su cumplimiento legal."""))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
