package com.manutencao.sistema.whatsapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppProperties(String verifyToken, String appSecret, String accessToken,
                                 String phoneNumberId, String apiVersion) {
    public String apiVersionOrDefault() { return apiVersion == null || apiVersion.isBlank() ? "v23.0" : apiVersion; }
}
