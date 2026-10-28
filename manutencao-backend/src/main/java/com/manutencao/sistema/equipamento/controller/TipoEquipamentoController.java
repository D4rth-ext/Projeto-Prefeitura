package com.manutencao.sistema.equipamento.controller;

import com.manutencao.sistema.equipamento.dto.TipoEquipamentoResponse;
import com.manutencao.sistema.equipamento.service.TipoEquipamentoService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tipos-equipamento")
public class TipoEquipamentoController {

    private final TipoEquipamentoService service;

    public TipoEquipamentoController(TipoEquipamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<TipoEquipamentoResponse> listar() {
        return service.listarAtivos();
    }
}
