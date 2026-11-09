package com.manutencao.sistema.os.dto;

import com.manutencao.sistema.os.domain.ModalidadeOs;
import com.manutencao.sistema.os.domain.StatusOs;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RelatorioOsResponse(
        LocalDate inicio,
        LocalDate fim,
        Long setorId,
        long total,
        long atrasadas,
        Map<StatusOs, Long> porStatus,
        Map<ModalidadeOs, Long> porModalidade,
        List<TotalPorSetor> porSetor
) {
    public record TotalPorSetor(Long setorId, String setorNome, long total) {
    }
}
