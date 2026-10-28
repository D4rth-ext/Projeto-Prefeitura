package com.manutencao.sistema.os.repository;

import com.manutencao.sistema.os.dto.BuscaOsRequest;
import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BuscaOsRepository {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private final NamedParameterJdbcTemplate jdbc;

    public BuscaOsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Resultado buscar(BuscaOsRequest filtro, Usuario usuario) {
        StringBuilder condicoes = new StringBuilder(" where 1 = 1");
        MapSqlParameterSource parametros = new MapSqlParameterSource();

        if (usuario.getPerfil() == Perfil.SOLICITANTE) {
            condicoes.append(" and os.solicitante_id = :usuarioId");
            parametros.addValue("usuarioId", usuario.getId());
        } else if (usuario.getPerfil() == Perfil.TECNICO) {
            condicoes.append("""
                     and exists (select 1 from os_tecnico vinculo
                         where vinculo.os_id = os.id and vinculo.tecnico_id = :usuarioId
                           and vinculo.saida_em is null)
                    """);
            parametros.addValue("usuarioId", usuario.getId());
        }

        adicionar(condicoes, parametros, "os.id = :id", "id", filtro.id());
        adicionar(condicoes, parametros, "os.setor_id = :setorId", "setorId", filtro.setorId());
        adicionar(condicoes, parametros, "os.equipamento_id = :equipamentoId", "equipamentoId", filtro.equipamentoId());
        adicionar(condicoes, parametros, "upper(eq.patrimonio) = upper(:patrimonio)", "patrimonio", normalizar(filtro.patrimonio()));
        adicionar(condicoes, parametros, "lower(eq.numero_serie) = lower(:numeroSerie)", "numeroSerie", normalizar(filtro.numeroSerie()));
        adicionar(condicoes, parametros, "os.status = :status", "status", filtro.status() == null ? null : filtro.status().name());
        adicionar(condicoes, parametros, "os.modalidade = :modalidade", "modalidade", filtro.modalidade() == null ? null : filtro.modalidade().name());

        if (filtro.tecnicoId() != null) {
            condicoes.append("""
                     and exists (select 1 from os_tecnico tecnico_filtro
                         where tecnico_filtro.os_id = os.id and tecnico_filtro.tecnico_id = :tecnicoId
                           and tecnico_filtro.saida_em is null)
                    """);
            parametros.addValue("tecnicoId", filtro.tecnicoId());
        }

        if (Boolean.TRUE.equals(filtro.backupEmAberto())) {
            condicoes.append("""
                     and exists (select 1 from os_backup backup
                         where backup.os_id = os.id and backup.solicitado
                           and backup.situacao in ('PENDENTE', 'CONCLUIDO')
                           and backup.descartado_em is null)
                    """);
        }

        if (Boolean.TRUE.equals(filtro.atrasadas())) {
            condicoes.append("""
                     and os.status in ('RECEBIDO', 'EM_ANALISE', 'AGUARDANDO_PECA')
                     and os.prazo_previsto < :hoje
                    """);
            parametros.addValue("hoje", LocalDate.now(FUSO));
        }

        if (filtro.inicio() != null) {
            condicoes.append(" and os.aberta_em >= :inicio");
            parametros.addValue("inicio", Timestamp.from(filtro.inicio().atStartOfDay(FUSO).toInstant()));
        }
        if (filtro.fim() != null) {
            condicoes.append(" and os.aberta_em < :fim");
            parametros.addValue("fim", Timestamp.from(filtro.fim().plusDays(1).atStartOfDay(FUSO).toInstant()));
        }

        String origem = " from ordem_servico os left join equipamento eq on eq.id = os.equipamento_id";
        Long total = jdbc.queryForObject("select count(*)" + origem + condicoes, parametros, Long.class);
        parametros.addValue("limite", filtro.tamanhoAtual());
        parametros.addValue("deslocamento", (long) filtro.paginaAtual() * filtro.tamanhoAtual());
        List<Long> ids = jdbc.queryForList("select os.id" + origem + condicoes
                + " order by os.aberta_em desc, os.id desc limit :limite offset :deslocamento", parametros, Long.class);
        return new Resultado(ids, total == null ? 0 : total);
    }

    private void adicionar(StringBuilder sql, MapSqlParameterSource parametros,
                            String condicao, String nome, Object valor) {
        if (valor != null) {
            sql.append(" and ").append(condicao);
            parametros.addValue(nome, valor);
        }
    }

    private String normalizar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    public record Resultado(List<Long> ids, long total) {
    }
}
