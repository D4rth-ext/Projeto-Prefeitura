package com.manutencao.sistema.os.dto;

import java.util.List;

public record PaginaOsResponse(
        List<OrdemServicoResponse> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        long totalPaginas
) {
}
