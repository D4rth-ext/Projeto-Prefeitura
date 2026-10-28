package com.manutencao.sistema.os.controller;

import com.manutencao.sistema.os.dto.AtualizacaoBackupRequest;
import com.manutencao.sistema.os.dto.BackupOsResponse;
import com.manutencao.sistema.os.dto.DescarteBackupRequest;
import com.manutencao.sistema.os.service.BackupOsService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/os")
public class BackupOsController {

    private final BackupOsService service;

    public BackupOsController(BackupOsService service) {
        this.service = service;
    }

    @GetMapping("/{id}/backup")
    public BackupOsResponse consultar(@PathVariable Long id, Principal principal) {
        return service.consultar(id, principal.getName());
    }

    @PatchMapping("/{id}/backup")
    public BackupOsResponse atualizar(@PathVariable Long id,
                                      @Valid @RequestBody AtualizacaoBackupRequest request,
                                      Principal principal) {
        return service.atualizar(id, request, principal.getName());
    }

    @PostMapping("/{id}/backup/descarte")
    public BackupOsResponse descartar(@PathVariable Long id,
                                      @Valid @RequestBody DescarteBackupRequest request,
                                      Principal principal) {
        return service.descartar(id, request, principal.getName());
    }

    @GetMapping("/backups/descarte-pendente")
    public List<BackupOsResponse> descartesPendentes(Principal principal) {
        return service.descartesPendentes(principal.getName());
    }
}
