package com.devcrafter.Patisserie.App.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.kkiapay")
public class KkiapayConfig {
    private String publicKey;
    private String privateKey;
    private String secretKey;
    private boolean sandbox;
    private String baseUrl;
}
