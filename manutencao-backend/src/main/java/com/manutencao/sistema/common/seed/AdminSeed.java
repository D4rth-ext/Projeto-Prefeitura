package com.manutencao.sistema.common.seed;

import com.manutencao.sistema.usuario.domain.Perfil;
import com.manutencao.sistema.usuario.domain.Usuario;
import com.manutencao.sistema.usuario.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Garante que sempre exista ao menos um Admin para começar a usar o sistema. */
@Component
public class AdminSeed implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeed.class);
    private static final String EMAIL_ADMIN = "admin@manutencao.local";
    private static final String SENHA_INICIAL = "admin123"; // trocar depois do primeiro login

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeed(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (repository.existsByEmailIgnoreCase(EMAIL_ADMIN)) {
            return;
        }
        Usuario admin = new Usuario("Administrador", EMAIL_ADMIN, null,
                passwordEncoder.encode(SENHA_INICIAL), Perfil.ADMIN);
        repository.save(admin);
        log.info("Admin inicial criado: {} / senha: {}", EMAIL_ADMIN, SENHA_INICIAL);
    }
}
