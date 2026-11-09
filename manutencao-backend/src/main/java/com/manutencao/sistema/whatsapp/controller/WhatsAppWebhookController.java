package com.manutencao.sistema.whatsapp.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.manutencao.sistema.whatsapp.config.WhatsAppProperties;
import com.manutencao.sistema.whatsapp.service.WhatsAppBotService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
public class WhatsAppWebhookController {
    private final WhatsAppBotService bot;
    private final WhatsAppProperties properties;
    private final ObjectMapper mapper;

    public WhatsAppWebhookController(WhatsAppBotService bot, WhatsAppProperties properties, ObjectMapper mapper) {
        this.bot = bot;
        this.properties = properties;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<String> verificar(@RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && StringUtils.hasText(properties.verifyToken())
                && properties.verifyToken().equals(token) && challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
    }

    @PostMapping
    public ResponseEntity<Void> receber(@RequestBody byte[] body,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura) throws Exception {
        if (!assinaturaValida(body, assinatura)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        JsonNode raiz = mapper.readTree(body);
        for (JsonNode entry : raiz.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                for (JsonNode mensagem : change.path("value").path("messages")) {
                    String telefone = mensagem.path("from").asText("");
                    String id = mensagem.path("id").asText("");
                    String texto = mensagem.path("text").path("body").asText("");
                    if (!telefone.isBlank() && !id.isBlank() && !texto.isBlank()) bot.receber(telefone, id, texto);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    private boolean assinaturaValida(byte[] corpo, String assinatura) throws Exception {
        if (!StringUtils.hasText(properties.appSecret()) || !StringUtils.hasText(assinatura)
                || !assinatura.startsWith("sha256=")) return false;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.appSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] recebida;
        try { recebida = HexFormat.of().parseHex(assinatura.substring(7)); }
        catch (IllegalArgumentException invalida) { return false; }
        return MessageDigest.isEqual(mac.doFinal(corpo), recebida);
    }
}
