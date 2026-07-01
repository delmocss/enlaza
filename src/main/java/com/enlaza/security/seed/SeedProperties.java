package com.enlaza.security.seed;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app.seed")
@Validated
@Getter
@Setter
public class SeedProperties {

    @NotBlank
    private String adminEmail;
    @NotBlank
    private String adminPassword;

    @NotBlank
    private String contableEmail;
    @NotBlank
    private String contablePassword;

    @NotBlank
    private String clienteEmail;
    @NotBlank
    private String clientePassword;
}
