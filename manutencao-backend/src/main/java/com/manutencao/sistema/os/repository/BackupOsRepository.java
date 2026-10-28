package com.manutencao.sistema.os.repository;

import com.manutencao.sistema.os.domain.SituacaoBackup;
import com.manutencao.sistema.os.dto.BackupOsResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BackupOsRepository {

    private final JdbcTemplate jdbc;

    public BackupOsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<BackupOsResponse> buscar(Long osId) {
        return consultar("where os.id = ?", osId).stream().findFirst();
    }

    public List<BackupOsResponse> descartesPendentes() {
        return consultar("""
                where backup.solicitado and backup.descartado_em is null
                  and os.entregue_em is not null
                order by os.entregue_em, os.id
                """);
    }

    public void atualizarSituacao(Long osId, SituacaoBackup situacao) {
        jdbc.update("""
                update os_backup set situacao = ?, atualizado_em = current_timestamp
                where os_id = ?
                """, situacao.name(), osId);
    }

    public void registrarDescarte(Long osId, Long autorId) {
        jdbc.update("""
                update os_backup
                set descartado_em = current_timestamp, descartado_por_id = ?,
                    atualizado_em = current_timestamp
                where os_id = ?
                """, autorId, osId);
    }

    private List<BackupOsResponse> consultar(String filtro, Object... parametros) {
        String sql = """
                select backup.*, os.entregue_em, os.versao
                from os_backup backup
                join ordem_servico os on os.id = backup.os_id
                """ + filtro;

        return jdbc.query(sql, (rs, linha) -> {
            Instant entrega = instante(rs.getTimestamp("entregue_em"));
            boolean solicitado = rs.getBoolean("solicitado");
            Instant limite = solicitado && entrega != null ? entrega.plus(30, ChronoUnit.DAYS) : null;
            Instant descarte = instante(rs.getTimestamp("descartado_em"));
            String situacao = rs.getString("situacao");

            return new BackupOsResponse(
                    rs.getLong("os_id"), solicitado,
                    rs.getObject("ciencia_risco_por_id", Long.class),
                    instante(rs.getTimestamp("ciencia_risco_em")),
                    rs.getString("termo_ciencia_risco"), rs.getString("escopo"),
                    rs.getString("midia_ti"),
                    situacao == null ? null : SituacaoBackup.valueOf(situacao),
                    descarte, rs.getObject("descartado_por_id", Long.class),
                    limite, limite != null && descarte == null && Instant.now().isAfter(limite),
                    rs.getLong("versao")
            );
        }, parametros);
    }

    private Instant instante(Timestamp data) {
        return data == null ? null : data.toInstant();
    }
}
