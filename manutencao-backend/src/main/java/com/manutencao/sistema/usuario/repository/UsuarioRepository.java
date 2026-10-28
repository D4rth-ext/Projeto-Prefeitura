package com.manutencao.sistema.usuario.repository;

import com.manutencao.sistema.usuario.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @org.springframework.data.jpa.repository.Query("""
            select new com.manutencao.sistema.usuario.dto.SolicitanteResumo(u.id, u.nome, s.id, s.nome)
            from Usuario u join u.setor s
            where u.ativo = true and s.ativo = true
                and u.perfil = com.manutencao.sistema.usuario.domain.Perfil.SOLICITANTE
            order by u.nome, u.id
            """)
    java.util.List<com.manutencao.sistema.usuario.dto.SolicitanteResumo> listarSolicitantesAtivos();

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
