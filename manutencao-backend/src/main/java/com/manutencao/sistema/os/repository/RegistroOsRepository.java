package com.manutencao.sistema.os.repository;

import com.manutencao.sistema.os.domain.TipoRegistroOs;
import com.manutencao.sistema.os.dto.EventoOsResponse;
import com.manutencao.sistema.os.dto.RegistroOsResponse;
import com.manutencao.sistema.os.dto.VinculoTecnicoResponse;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RegistroOsRepository {

    private final JdbcTemplate jdbc;

    public RegistroOsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Long inserir(Long osId, TipoRegistroOs tipo, Long autorId, String texto) {
        GeneratedKeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            PreparedStatement comando = conexao.prepareStatement("""
                    insert into os_registro (os_id, tipo, autor_id, texto)
                    values (?, ?, ?, ?)
                    """, new String[]{"id"});
            comando.setLong(1, osId);
            comando.setString(2, tipo.name());
            comando.setLong(3, autorId);
            comando.setString(4, texto);
            return comando;
        }, chave);
        Number id = chave.getKey();
        if (id == null) {
            throw new IllegalStateException("O banco não retornou o ID do registro.");
        }
        return id.longValue();
    }

    public boolean existeDiagnostico(Long osId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from os_registro where os_id = ? and tipo = 'DIAGNOSTICO')
                """, Boolean.class, osId));
    }

    public boolean existeApontamento(Long osId, Long registroId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (
                    select 1 from os_registro where os_id = ? and id = ? and tipo in ('DIAGNOSTICO', 'ACAO')
                )
                """, Boolean.class, osId, registroId));
    }

    public List<RegistroOsResponse> listar(Long osId, boolean apenasOrientacoes) {
        String filtro = apenasOrientacoes ? " and registro.tipo = 'ORIENTACAO'" : "";
        return jdbc.query("""
                select registro.*, autor.nome as autor_nome, os.versao
                from os_registro registro
                join usuario autor on autor.id = registro.autor_id
                join ordem_servico os on os.id = registro.os_id
                where registro.os_id = ?
                """ + filtro + " order by registro.criado_em, registro.id",
                (rs, linha) -> new RegistroOsResponse(
                        rs.getLong("id"), rs.getLong("os_id"),
                        TipoRegistroOs.valueOf(rs.getString("tipo")), rs.getLong("autor_id"),
                        rs.getString("autor_nome"), rs.getString("texto"),
                        instante(rs.getTimestamp("criado_em")), rs.getLong("versao")
                ), osId);
    }

    public List<EventoOsResponse> eventos(Long osId) {
        return jdbc.query("""
                select evento.*, autor.nome as autor_nome
                from os_evento evento join usuario autor on autor.id = evento.autor_id
                where evento.os_id = ? order by evento.criado_em, evento.id
                """, (rs, linha) -> new EventoOsResponse(
                        rs.getLong("id"), rs.getLong("os_id"), rs.getString("tipo"),
                        rs.getLong("autor_id"), rs.getString("autor_nome"),
                        rs.getString("valor_anterior"), rs.getString("valor_novo"),
                        rs.getString("motivo"), instante(rs.getTimestamp("criado_em"))
                ), osId);
    }

    public List<VinculoTecnicoResponse> tecnicos(Long osId) {
        return jdbc.query("""
                select vinculo.*, tecnico.nome as tecnico_nome
                from os_tecnico vinculo join usuario tecnico on tecnico.id = vinculo.tecnico_id
                where vinculo.os_id = ? order by vinculo.entrada_em, vinculo.id
                """, (rs, linha) -> new VinculoTecnicoResponse(
                        rs.getLong("id"), rs.getLong("tecnico_id"), rs.getString("tecnico_nome"),
                        rs.getLong("designado_por_id"), instante(rs.getTimestamp("entrada_em")),
                        instante(rs.getTimestamp("saida_em")), rs.getString("motivo_saida")
                ), osId);
    }

    private Instant instante(Timestamp data) {
        return data == null ? null : data.toInstant();
    }
}
