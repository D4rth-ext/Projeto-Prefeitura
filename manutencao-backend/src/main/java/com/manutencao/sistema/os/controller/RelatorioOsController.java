package com.manutencao.sistema.os.controller;

import com.manutencao.sistema.os.dto.RelatorioOsResponse;
import com.manutencao.sistema.os.service.RelatorioOsService;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioOsController {

    private final RelatorioOsService service;

    public RelatorioOsController(RelatorioOsService service) {
        this.service = service;
    }

    @GetMapping("/os")
    public RelatorioOsResponse gerar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long setorId,
            Principal principal) {
        return service.gerar(inicio, fim, setorId, principal.getName());
    }
}
