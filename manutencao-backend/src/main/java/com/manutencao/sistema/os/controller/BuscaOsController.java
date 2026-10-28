package com.manutencao.sistema.os.controller;

import com.manutencao.sistema.os.dto.BuscaOsRequest;
import com.manutencao.sistema.os.dto.PaginaOsResponse;
import com.manutencao.sistema.os.service.BuscaOsService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/os")
public class BuscaOsController {

    private final BuscaOsService service;

    public BuscaOsController(BuscaOsService service) {
        this.service = service;
    }

    @GetMapping("/busca")
    public PaginaOsResponse buscar(@Valid @ModelAttribute BuscaOsRequest filtro, Principal principal) {
        return service.buscar(filtro, principal.getName());
    }
}
