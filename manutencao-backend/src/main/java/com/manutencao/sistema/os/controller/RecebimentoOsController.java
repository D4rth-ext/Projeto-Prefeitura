package com.manutencao.sistema.os.controller;

import com.manutencao.sistema.os.dto.RecebimentoOsRequest;
import com.manutencao.sistema.os.dto.RecebimentoOsResponse;
import com.manutencao.sistema.os.service.RecebimentoOsService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/os")
public class RecebimentoOsController {

    private final RecebimentoOsService service;

    public RecebimentoOsController(RecebimentoOsService service) {
        this.service = service;
    }

    @PostMapping("/{id}/recebimento")
    public ResponseEntity<RecebimentoOsResponse> receber(
            @PathVariable Long id,
            @Valid @RequestBody RecebimentoOsRequest request,
            Principal principal
    ) {
        RecebimentoOsResponse resposta = service.receber(
                id, request, principal.getName()
        );

        return ResponseEntity.created(
                URI.create("/api/v1/os/" + id + "/recebimento")
        ).body(resposta);
    }

    @GetMapping("/{id}/recebimento")
    public RecebimentoOsResponse consultar(
            @PathVariable Long id,
            Principal principal
    ) {
        return service.consultar(id, principal.getName());
    }
}