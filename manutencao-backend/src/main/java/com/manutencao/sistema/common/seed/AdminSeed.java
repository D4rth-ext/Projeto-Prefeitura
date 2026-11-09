package com.manutencao.sistema.common.seed;

import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import com.manutencao.sistema.usuario.service.PoliticaSenha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Garante que sempre exista ao menos um Admin para começar a usar o sistema. */
@Component
public class AdminSeed implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeed.class);
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminSeed(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                     @Value("${app.admin-inicial.email:}") String email,
                     @Value("${app.admin-inicial.senha:}") String senha) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (repository.existsByPerfilAndAtivoTrue(Perfil.ADMIN)) {
            return;
        }
        if (email.isBlank() || senha.isBlank()) {
            throw new IllegalStateException("Configure APP_ADMIN_INICIAL_EMAIL e APP_ADMIN_INICIAL_SENHA.");
        }
        PoliticaSenha.validar(senha);
        String emailNormalizado = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw new IllegalStateException("O e-mail do administrador inicial já está cadastrado.");
        }
        Usuario admin = new Usuario("Administrador", emailNormalizado, null,
                passwordEncoder.encode(senha), Perfil.ADMIN);
        repository.save(admin);
        log.info("Administrador inicial criado.");
    }
}
