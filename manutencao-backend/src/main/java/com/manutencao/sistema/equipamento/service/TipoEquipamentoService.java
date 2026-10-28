package com.manutencao.sistema.equipamento.service;

import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.equipamento.domain.TipoEquipamento;
import com.manutencao.sistema.equipamento.dto.TipoEquipamentoResponse;
import com.manutencao.sistema.equipamento.repository.TipoEquipamentoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TipoEquipamentoService {

    private final TipoEquipamentoRepository repository;

    public TipoEquipamentoService(TipoEquipamentoRepository repository) {
        this.repository = repository;
    }

    public List<TipoEquipamentoResponse> listarAtivos() {
        return repository.findByAtivoTrueOrderByNome().stream().map(TipoEquipamentoResponse::de).toList();
    }

    /** Uso interno do EquipamentoService: só devolve o tipo se estiver ativo. */
    TipoEquipamento obterAtivo(Long id) {
        TipoEquipamento tipo = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de equipamento", id));
        if (!tipo.isAtivo()) {
            throw new RegraNegocioException("O tipo de equipamento informado está inativo.");
        }
        return tipo;
    }
}
