package com.manutencao.sistema.os.repository;

import com.manutencao.sistema.common.exception.ConflitoException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TriagemOsRepository {

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
        Long total = jdbc.queryForObject("""
                select count(*) from os_tecnico vinculo
                join usuario tecnico on tecnico.id = vinculo.tecnico_id
                where vinculo.os_id = ? and vinculo.saida_em is null
                  and tecnico.ativo and tecnico.perfil = 'TECNICO'
                """, Long.class, osId);
        return total == null ? 0 : total;
    }

    public void adicionarTecnico(Long osId, Long tecnicoId, Long autorId) {
        jdbc.update("""
                insert into os_tecnico (os_id, tecnico_id, designado_por_id)
                values (?, ?, ?)
                """, osId, tecnicoId, autorId);
    }

    public void removerTecnico(Long osId, Long tecnicoId, String motivo) {
        int alterados = jdbc.update("""
                update os_tecnico
                set saida_em = current_timestamp, motivo_saida = ?
                where os_id = ? and tecnico_id = ? and saida_em is null
                """, motivo, osId, tecnicoId);

        if (alterados != 1) {
            throw new ConflitoException("O vínculo do técnico foi alterado. Consulte a OS novamente.");
        }
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
