package com.manutencao.sistema.whatsapp.service;

import com.manutencao.sistema.whatsapp.config.WhatsAppProperties;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class WhatsAppPropertiesBridge {
    private final WhatsAppProperties properties;
    private final RestClient client = RestClient.builder().build();

    public WhatsAppPropertiesBridge(WhatsAppProperties properties) { this.properties = properties; }

    public void enviar(String telefone, String texto) {
        if (properties.accessToken() == null || properties.accessToken().isBlank()
                || properties.phoneNumberId() == null || properties.phoneNumberId().isBlank()) {
            return;
        }
        client.post()
                .uri("https://graph.facebook.com/{version}/{phoneId}/messages",
                        properties.apiVersionOrDefault(), properties.phoneNumberId())
                .header("Authorization", "Bearer " + properties.accessToken())
                .body(Map.of("messaging_product", "whatsapp", "to", telefone,
                        "type", "text", "text", Map.of("body", texto)))
                .retrieve().toBodilessEntity();
    }
}
