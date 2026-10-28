package com.manutencao.sistema.os.controller;

import com.manutencao.sistema.os.dto.EventoOsResponse;
import com.manutencao.sistema.os.dto.RegistroOsRequest;
import com.manutencao.sistema.os.dto.RegistroOsResponse;
import com.manutencao.sistema.os.dto.VinculoTecnicoResponse;
import com.manutencao.sistema.os.service.RegistroOsService;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/os")
public class RegistroOsController {

    private final RegistroOsService service;

    public RegistroOsController(RegistroOsService service) {
        this.service = service;
    }

    @PostMapping("/{id}/registros")
    public ResponseEntity<RegistroOsResponse> criar(@PathVariable Long id,
                                                   @Valid @RequestBody RegistroOsRequest request,
                                                   Principal principal) {
        RegistroOsResponse resposta = service.criar(id, request, principal.getName());
        return ResponseEntity.created(URI.create("/api/v1/os/" + id + "/registros")).body(resposta);
    }

    @GetMapping("/{id}/registros")
    public List<RegistroOsResponse> listar(@PathVariable Long id, Principal principal) {
        return service.listar(id, principal.getName());
    }

    @GetMapping("/{id}/eventos")
    public List<EventoOsResponse> eventos(@PathVariable Long id, Principal principal) {
        return service.eventos(id, principal.getName());
    }

    @GetMapping("/{id}/tecnicos/historico")
    public List<VinculoTecnicoResponse> tecnicos(@PathVariable Long id, Principal principal) {
        return service.tecnicos(id, principal.getName());
    }
}
