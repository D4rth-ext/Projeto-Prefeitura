package com.manutencao.sistema.os.controller;

import com.manutencao.sistema.os.dto.OrdemServicoResponse;
import com.manutencao.sistema.os.dto.PrazoOsRequest;
import com.manutencao.sistema.os.dto.StatusOsRequest;
import com.manutencao.sistema.os.service.FluxoOsService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/os")
public class FluxoOsController {

    private final FluxoOsService service;

    public FluxoOsController(FluxoOsService service) {
        this.service = service;
    }

    @PatchMapping("/{id}/status")
    public OrdemServicoResponse alterarStatus(@PathVariable Long id,
                                              @Valid @RequestBody StatusOsRequest request,
                                              Principal principal) {
        return service.alterarStatus(id, request, principal.getName());
    }

    @PatchMapping("/{id}/prazo")
    public OrdemServicoResponse alterarPrazo(@PathVariable Long id,
                                             @Valid @RequestBody PrazoOsRequest request,
                                             Principal principal) {
        return service.alterarPrazo(id, request, principal.getName());
    }
}
