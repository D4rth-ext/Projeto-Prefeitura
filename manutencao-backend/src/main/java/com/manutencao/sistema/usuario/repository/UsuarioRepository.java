package com.manutencao.sistema.usuario.repository;

import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(value = "select u.* from usuario u where u.ativo = true and u.perfil = :perfil and regexp_replace(coalesce(u.telefone, ''), '[^0-9]', '', 'g') in (:telefone, :telefoneLocal)", nativeQuery = true)
    List<Usuario> buscarPorTelefoneAtivo(@Param("perfil") String perfil, @Param("telefone") String telefone,
                                        @Param("telefoneLocal") String telefoneLocal);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPerfilAndAtivoTrue(Perfil perfil);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> buscarParaAlteracao(@Param("id") Long id);

    @Query("""
            select u from Usuario u join fetch u.setor s
            where u.ativo = true and u.perfil = :perfil
              and (:setorId is null or s.id = :setorId)
              and (locate(:busca, lower(u.nome)) > 0 or locate(:busca, lower(u.email)) > 0)
            order by u.nome, u.id
            """)
    List<Usuario> buscarAtivosComSetor(@Param("perfil") Perfil perfil, @Param("setorId") Long setorId,
                                     @Param("busca") String busca);
}
