package com.manutencao.sistema.os.repository;

import com.manutencao.sistema.common.exception.ConflitoException;
import com.manutencao.sistema.os.dto.RecebimentoOsResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RecebimentoOsRepository {

    private final JdbcTemplate jdbc;

    public RecebimentoOsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void registrarRecebimento(
            Long osId,
            Long equipamentoId,
            String acessorios,
            Long versao
    ) {
        int alterados = jdbc.update("""
                update ordem_servico
                set equipamento_id = ?,
                    acessorios = ?,
                    recebido_em = current_timestamp,
                    atualizado_em = current_timestamp,
                    versao = versao + 1
                where id = ? and versao = ?
                  and modalidade = 'BANCADA'
                  and status = 'RECEBIDO'
                  and recebido_em is null
                """, equipamentoId, acessorios, osId, versao);

        if (alterados != 1) {
            throw new ConflitoException(
                    "A OS foi alterada ou o equipamento já foi recebido. Consulte novamente."
            );
        }
    }

    public void registrarBackup(
            Long osId,
            boolean solicitado,
            Long solicitanteId,
            String escopo,
            String midia,
            String termo
    ) {
        jdbc.update("""
                insert into os_backup (
                    os_id, solicitado, ciencia_risco_por_id, ciencia_risco_em,
                    termo_ciencia_risco, escopo, midia_ti, situacao
                )
                values (?, ?, ?, case when ? then current_timestamp else null end, ?, ?, ?, ?)
                """,
                osId,
                solicitado,
                solicitado ? null : solicitanteId,
                !solicitado,
                solicitado ? null : termo,
                solicitado ? escopo : null,
                solicitado ? midia : null,
                solicitado ? "PENDENTE" : null
        );
    }

    public Optional<RecebimentoOsResponse> buscar(Long osId) {
        return jdbc.query("""
                select os.id as os_id, os.equipamento_id,
                       eq.patrimonio, eq.numero_serie,
                       os.acessorios, os.recebido_em, os.versao,
                       backup.solicitado, backup.escopo, backup.midia_ti,
                       backup.situacao, backup.ciencia_risco_por_id,
                       backup.ciencia_risco_em, backup.termo_ciencia_risco,
                       (
                           select evento.autor_id from os_evento evento
                           where evento.os_id = os.id and evento.tipo = 'RECEBIMENTO'
                           order by evento.criado_em, evento.id
                           limit 1
                       ) as recebido_por_id
                from ordem_servico os
                join equipamento eq on eq.id = os.equipamento_id
                join os_backup backup on backup.os_id = os.id
                where os.id = ? and os.recebido_em is not null
                """,
                (rs, linha) -> new RecebimentoOsResponse(
                        rs.getLong("os_id"),
                        rs.getLong("equipamento_id"),
                        rs.getString("patrimonio"),
                        rs.getString("numero_serie"),
                        rs.getString("acessorios"),
                        instante(rs.getTimestamp("recebido_em")),
                        rs.getObject("recebido_por_id", Long.class),
                        rs.getBoolean("solicitado"),
                        rs.getString("escopo"),
                        rs.getString("midia_ti"),
                        rs.getString("situacao"),
                        rs.getObject("ciencia_risco_por_id", Long.class),
                        instante(rs.getTimestamp("ciencia_risco_em")),
                        rs.getString("termo_ciencia_risco"),
                        rs.getLong("versao")
                ),
                osId
        ).stream().findFirst();
    }

    private Instant instante(Timestamp data) {
        return data == null ? null : data.toInstant();
    }
}