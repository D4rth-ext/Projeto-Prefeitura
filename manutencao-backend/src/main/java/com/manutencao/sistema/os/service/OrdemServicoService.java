package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RecursoNaoEncontradoException;
import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.OrdemServico;
import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.StatusOs;
import com.manutencao.sistema.os.dto.AberturaOsRequest;
import com.manutencao.sistema.os.dto.EdicaoSolicitacaoRequest;
import com.manutencao.sistema.os.dto.OrdemServicoResponse;
import com.manutencao.sistema.os.dto.PatrimonioOsRequest;
import com.manutencao.sistema.os.repository.BackupOsRepository;
import com.manutencao.sistema.os.repository.OrdemServicoRepository;
import com.manutencao.sistema.os.repository.TriagemOsRepository;
import com.manutencao.sistema.setor.domain.Setor;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OrdemServicoService {

    private final OrdemServicoRepository osRepository;
    private final UsuarioRepository usuarioRepository;
    private final BackupOsRepository backupRepository;
    private final AcessoOsService acesso;
    private final TriagemOsRepository triagemRepository;

    public OrdemServicoService(
            OrdemServicoRepository osRepository,
            UsuarioRepository usuarioRepository,
            BackupOsRepository backupRepository,
            AcessoOsService acesso,
            TriagemOsRepository triagemRepository
    ) {
        this.osRepository = osRepository;
        this.usuarioRepository = usuarioRepository;
        this.backupRepository = backupRepository;
        this.acesso = acesso;
        this.triagemRepository = triagemRepository;
    }

    @Transactional
    public OrdemServicoResponse editarSolicitacao(Long id, EdicaoSolicitacaoRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        OrdemServico os = acesso.consultar(id, autor);
        if (autor.getPerfil() != Perfil.SOLICITANTE && autor.getPerfil() != Perfil.ADMIN && autor.getPerfil() != Perfil.ATENDENTE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação não permitida.");
        }
        if (os.getStatus() != StatusOs.RECEBIDO || os.getModalidade() != ModalidadeOs.A_DEFINIR
                || !triagemRepository.tecnicosAtivos(id).isEmpty()) {
            throw new RegraNegocioException("O chamado só pode ser editado antes da triagem.");
        }
        acesso.conferirVersao(os, request.versao());
        os.editarSolicitacao(request.categoria(), request.descricaoProblema(), request.patrimonioInformado(),
                request.computadorLiga(), request.temRede(), request.consegueAnyDesk(), request.contato(), request.melhorHorario());
        return OrdemServicoResponse.de(osRepository.saveAndFlush(os));
    }

    @Transactional
    public OrdemServicoResponse abrir(AberturaOsRequest request, String email) {
        Usuario autor = acesso.usuario(email, "Usuário autenticado não encontrado.");

        if (autor.getPerfil() == Perfil.TECNICO) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Técnico não pode abrir uma ordem de serviço."
            );
        }

        Usuario solicitante = resolverSolicitante(request.solicitanteId(), autor);
        Setor setor = solicitante.getSetor();

        if (setor == null) {
            throw new RegraNegocioException("O solicitante não possui setor vinculado.");
        }

        if (!setor.isAtivo()) {
            throw new RegraNegocioException("Não é possível abrir OS para um setor inativo.");
        }

        OrdemServico os = new OrdemServico(
                setor,
                solicitante,
                autor,
                request.categoria(),
                request.descricaoProblema()
        );

        os.preencherAbertura(
                request.patrimonioInformado(), request.computadorLiga(), request.temRede(),
                request.consegueAnyDesk(), request.contato(), request.melhorHorario()
        );

        return OrdemServicoResponse.de(osRepository.save(os), autor.getPerfil() == Perfil.SOLICITANTE);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public OrdemServicoResponse abrirPeloWhatsApp(AberturaOsRequest request, String waId) {
        String numero = waId.replaceAll("\\D", "");
        String local = numero.startsWith("55") && numero.length() > 11
                ? numero.substring(2) : numero;
        List<Usuario> encontrados = usuarioRepository.buscarPorTelefoneAtivo(
                Perfil.SOLICITANTE.name(), numero, local);
        if (encontrados.size() != 1) {
            throw new RegraNegocioException(encontrados.isEmpty()
                    ? "Não encontrei um solicitante ativo com esse WhatsApp cadastrado."
                    : "Há mais de um cadastro com esse telefone. Fale com o atendimento para corrigir.");
        }
        Usuario solicitante = encontrados.getFirst();
        Setor setor = solicitante.getSetor();
        if (setor == null || !setor.isAtivo()) {
            throw new RegraNegocioException("Seu cadastro precisa ter um setor ativo vinculado para abrir chamado.");
        }
        OrdemServico os = new OrdemServico(setor, solicitante, solicitante,
                request.categoria(), request.descricaoProblema());
        os.preencherAbertura(request.patrimonioInformado(), request.computadorLiga(),
                request.temRede(), request.consegueAnyDesk(), request.contato(), request.melhorHorario());
        return OrdemServicoResponse.de(osRepository.save(os));
    }

    public List<OrdemServicoResponse> listar(String email) {
        Usuario usuario = acesso.usuario(email, "Usuário autenticado não encontrado.");

        List<OrdemServico> ordens = switch (usuario.getPerfil()) {
            case ADMIN, ATENDENTE -> osRepository.findAllByExcluidaEmIsNullOrderByAbertaEmDesc();
            case SOLICITANTE -> osRepository
                    .findBySolicitanteIdAndExcluidaEmIsNullOrderByAbertaEmDesc(usuario.getId());
            case TECNICO -> listarDoTecnico(usuario.getId());
        };

        boolean visaoSolicitante = usuario.getPerfil() == Perfil.SOLICITANTE;
        return ordens.stream().map(os -> OrdemServicoResponse.de(os, visaoSolicitante)).toList();
    }

    public OrdemServicoResponse buscarPorId(Long id, String email) {
        Usuario usuario = acesso.usuario(email, "Usuário autenticado não encontrado.");
        OrdemServico os = acesso.consultar(id, usuario);
        return OrdemServicoResponse.de(os, usuario.getPerfil() == Perfil.SOLICITANTE);
    }

    @Transactional
    public OrdemServicoResponse atualizarPatrimonio(Long id, PatrimonioOsRequest request, String email) {
        Usuario autor = acesso.usuario(email);
        OrdemServico os = acesso.consultar(id, autor);
        acesso.exigirTecnicoOuAdmin(os, autor);
        if (os.getStatus().terminal()) {
            throw new RegraNegocioException("Não é possível alterar o patrimônio de uma OS encerrada.");
        }
        acesso.conferirVersao(os, request.versao());
        String anterior = os.getPatrimonioInformado();
        os.atualizarPatrimonioInformado(request.patrimonioInformado());
        osRepository.saveAndFlush(os);
        triagemRepository.registrarEvento(id, "PATRIMONIO", autor.getId(), anterior,
                os.getPatrimonioInformado(), "Patrimônio informado corrigido pela equipe técnica.");
        return OrdemServicoResponse.de(os);
    }

    @Transactional
    public void excluir(Long id, String email) {
        Usuario autor = acesso.usuario(email, "Usuário autenticado não encontrado.");
        OrdemServico os = acesso.consultar(id, autor);

        boolean ehAdministrador = autor.getPerfil() == Perfil.ADMIN;
        boolean ehSolicitanteDaOs = autor.getPerfil() == Perfil.SOLICITANTE
                && os.getSolicitante().getId().equals(autor.getId());
        if (!ehAdministrador && !ehSolicitanteDaOs) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode excluir chamados abertos por você.");
        }

        if (os.getRecebidoEm() != null && os.getEntregueEm() == null) {
            throw new RegraNegocioException("Registre a devolução do equipamento antes de excluir a OS.");
        }
        if (backupRepository.buscar(id).filter(backup -> backup.solicitado() && backup.descartadoEm() == null)
                .isPresent()) {
            throw new RegraNegocioException("Confirme o descarte do backup antes de excluir a OS.");
        }

        os.excluir(autor.getId());
        osRepository.saveAndFlush(os);
    }

    private List<OrdemServico> listarDoTecnico(Long tecnicoId) {
        List<Long> ids = osRepository.buscarIdsPorTecnicoAtivo(tecnicoId);

        if (ids.isEmpty()) {
            return List.of();
        }

        return osRepository.buscarPorIds(ids);
    }

    private Usuario resolverSolicitante(Long solicitanteId, Usuario autor) {
        if (autor.getPerfil() == Perfil.SOLICITANTE) {
            if (solicitanteId != null && !solicitanteId.equals(autor.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Você só pode abrir OS em seu próprio nome."
                );
            }

            return autor;
        }

        if (solicitanteId == null) {
            throw new RegraNegocioException(
                    "Admin e atendente devem informar o solicitanteId."
            );
        }

        Usuario solicitante = usuarioRepository.findById(solicitanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Solicitante", solicitanteId
                ));

        if (solicitante.getPerfil() != Perfil.SOLICITANTE) {
            throw new RegraNegocioException(
                    "O usuário informado deve possuir o perfil SOLICITANTE."
            );
        }

        if (!solicitante.isAtivo()) {
            throw new RegraNegocioException("O solicitante informado está inativo.");
        }

        return solicitante;
    }
}

