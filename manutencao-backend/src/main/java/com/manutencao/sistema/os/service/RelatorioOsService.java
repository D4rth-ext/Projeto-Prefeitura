package com.manutencao.sistema.os.service;

import com.manutencao.sistema.common.exception.RegraNegocioException;
import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.StatusOs;
import com.manutencao.sistema.os.dto.RelatorioOsResponse;
import com.manutencao.sistema.os.dto.RelatorioOsResponse.TotalPorSetor;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RelatorioOsService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final AcessoOsService acesso;
    private final NamedParameterJdbcTemplate jdbc;

    public RelatorioOsService(AcessoOsService acesso, NamedParameterJdbcTemplate jdbc) {
        this.acesso = acesso;
        this.jdbc = jdbc;
    }

    public RelatorioOsResponse gerar(LocalDate inicio, LocalDate fim, Long setorId, String email) {
        acesso.exigirGestao(acesso.usuario(email));
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new RegraNegocioException("O fim do período não pode ser anterior ao início.");
        }
        if (setorId != null && setorId <= 0) {
            throw new RegraNegocioException("Informe um ID de setor positivo.");
        }

        StringBuilder sql = new StringBuilder("""
                select os.status, os.modalidade, s.id as setor_id, s.nome as setor_nome,
                    count(*) as total,
                    count(*) filter (where os.status in ('RECEBIDO', 'EM_ANALISE', 'AGUARDANDO_PECA')
                        and os.prazo_previsto < :hoje) as atrasadas
                from ordem_servico os
                join setor s on s.id = os.setor_id
                where true
                """);
        var parametros = new MapSqlParameterSource("hoje", LocalDate.now(FUSO));
        if (inicio != null) {
            sql.append(" and os.aberta_em >= :inicio");
            parametros.addValue("inicio", Timestamp.from(inicio.atStartOfDay(FUSO).toInstant()));
        }
        if (fim != null) {
            sql.append(" and os.aberta_em < :fim");
            parametros.addValue("fim", Timestamp.from(fim.plusDays(1).atStartOfDay(FUSO).toInstant()));
        }
        if (setorId != null) {
            sql.append(" and os.setor_id = :setorId");
            parametros.addValue("setorId", setorId);
        }
        sql.append("""
                 group by grouping sets ((), (os.status), (os.modalidade), (s.id, s.nome))
                 order by s.nome, s.id
                """);

        return jdbc.query(sql.toString(), parametros, rs -> {
            long total = 0;
            long atrasadas = 0;
            var porStatus = new EnumMap<StatusOs, Long>(StatusOs.class);
            var porModalidade = new EnumMap<ModalidadeOs, Long>(ModalidadeOs.class);
            var porSetor = new ArrayList<TotalPorSetor>();
            for (StatusOs status : StatusOs.values()) {
                porStatus.put(status, 0L);
            }
            for (ModalidadeOs modalidade : ModalidadeOs.values()) {
                porModalidade.put(modalidade, 0L);
            }
            while (rs.next()) {
                long quantidade = rs.getLong("total");
                String status = rs.getString("status");
                String modalidade = rs.getString("modalidade");
                Long setor = rs.getObject("setor_id", Long.class);
                if (status != null) {
                    porStatus.put(StatusOs.valueOf(status), quantidade);
                } else if (modalidade != null) {
                    porModalidade.put(ModalidadeOs.valueOf(modalidade), quantidade);
                } else if (setor != null) {
                    porSetor.add(new TotalPorSetor(setor, rs.getString("setor_nome"), quantidade));
                } else {
                    total = quantidade;
                    atrasadas = rs.getLong("atrasadas");
                }
            }
            return new RelatorioOsResponse(inicio, fim, setorId, total, atrasadas, porStatus,
                    porModalidade, List.copyOf(porSetor));
        });
    }
}
