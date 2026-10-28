package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.dto.BuscaOsRequest;
import com.manutencao.sistema.os.dto.OrdemServicoResponse;
import com.manutencao.sistema.os.dto.PaginaOsResponse;
import com.manutencao.sistema.os.repository.BuscaOsRepository;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BuscaOsService {

    private final AcessoOsService acesso;
    private final BuscaOsRepository buscaRepository;
    private final OrdemServicoRepository osRepository;

    public BuscaOsService(AcessoOsService acesso, BuscaOsRepository buscaRepository,
                          OrdemServicoRepository osRepository) {
        this.acesso = acesso;
        this.buscaRepository = buscaRepository;
        this.osRepository = osRepository;
    }

    public PaginaOsResponse buscar(BuscaOsRequest filtro, String email) {
        if (filtro.inicio() != null && filtro.fim() != null && filtro.fim().isBefore(filtro.inicio())) {
            throw new RegraNegocioException("O fim do período não pode ser anterior ao início.");
        }

        BuscaOsRepository.Resultado resultado = buscaRepository.buscar(filtro, acesso.usuario(email));
        List<OrdemServicoResponse> conteudo = resultado.ids().isEmpty() ? List.of()
                : osRepository.buscarPorIds(resultado.ids()).stream().map(OrdemServicoResponse::de).toList();
        long paginas = (resultado.total() + filtro.tamanhoAtual() - 1) / filtro.tamanhoAtual();
        return new PaginaOsResponse(conteudo, filtro.paginaAtual(), filtro.tamanhoAtual(), resultado.total(), paginas);
    }
}
