package com.manutencao.sistema.os.repository;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.os.domain.StatusOs;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TriagemOsRepository {

    private static final String VINCULOS_ATIVOS = """
             from os_tecnico vinculo
             join usuario tecnico on tecnico.id = vinculo.tecnico_id
             where vinculo.os_id = ? and vinculo.saida_em is null
               and tecnico.ativo and tecnico.perfil = 'TECNICO'
            """;

    private static final String MOTIVO_INATIVACAO = "Usuário inativado.";

    private final JdbcTemplate jdbc;

    public TriagemOsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Long> tecnicosAtivos(Long osId) {
        return jdbc.queryForList("""
                select tecnico_id from os_tecnico
                where os_id = ? and saida_em is null
                order by tecnico_id
                """, Long.class, osId);
    }

    public long quantidadeTecnicosAtivos(Long osId) {
        Long total = jdbc.queryForObject("select count(*)" + VINCULOS_ATIVOS, Long.class, osId);
        return total == null ? 0 : total;
    }

    public boolean temOutroTecnicoAtivo(Long osId, Long tecnicoId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1" + VINCULOS_ATIVOS + " and tecnico.id <> ?)",
                Boolean.class, osId, tecnicoId));
    }

    public record OsDoTecnico(Long id, StatusOs status) {
    }

    public List<OsDoTecnico> bloquearOrdensDoTecnico(Long tecnicoId) {
        // Bloqueie as OSs pela ordem dos IDs também quando há inativações simultâneas.
        return jdbc.query("""
                select os.id, os.status
                from ordem_servico os
                where os.excluida_em is null
                  and exists (select 1 from os_tecnico vinculo
                    where vinculo.os_id = os.id and vinculo.tecnico_id = ?
                      and vinculo.saida_em is null)
                order by os.id for update
                """, (rs, linha) -> new OsDoTecnico(rs.getLong("id"), StatusOs.valueOf(rs.getString("status"))),
                tecnicoId);
    }

    public void adicionarTecnico(Long osId, Long tecnicoId, Long autorId) {
        jdbc.update("""
                insert into os_tecnico (os_id, tecnico_id, designado_por_id)
                values (?, ?, ?)
                """, osId, tecnicoId, autorId);
    }

    public void removerTecnico(Long osId, Long tecnicoId, String motivo) {
        if (encerrarVinculo(osId, tecnicoId, motivo) != 1) {
            throw new ConflitoException("O vínculo do técnico foi alterado. Consulte a OS novamente.");
        }
    }

    public void registrarInativacao(Long osId, Long tecnicoId, Long autorId) {
        encerrarVinculo(osId, tecnicoId, MOTIVO_INATIVACAO);
        jdbc.update("""
                update ordem_servico set versao = versao + 1, atualizado_em = current_timestamp
                where id = ?
                """, osId);
        registrarEvento(osId, "TECNICO_SAIU", autorId, tecnicoId, null, MOTIVO_INATIVACAO);
    }

    private int encerrarVinculo(Long osId, Long tecnicoId, String motivo) {
        return jdbc.update("""
                update os_tecnico
                set saida_em = current_timestamp, motivo_saida = ?
                where os_id = ? and tecnico_id = ? and saida_em is null
                """, motivo, osId, tecnicoId);
    }

    public void registrarEvento(
            Long osId,
            String tipo,
            Long autorId,
            Object anterior,
            Object novo,
            String motivo
    ) {
        jdbc.update("""
                insert into os_evento
                    (os_id, tipo, autor_id, valor_anterior, valor_novo, motivo)
                values (?, ?, ?, ?, ?, ?)
                """,
                osId,
                tipo,
                autorId,
                anterior == null ? null : anterior.toString(),
                novo == null ? null : novo.toString(),
                motivo
        );
    }
}
