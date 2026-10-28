package com.manutencao.sistema.common.config;

import com.manutencao.sistema.auth.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {}) // usa o bean CorsConfigurationSource do CorsConfig
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        // 401: sem token válido nenhum
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        // 403: token válido, mas perfil sem permissão para a rota
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/v1/usuarios", "/api/v1/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/equipe").hasAnyRole("ADMIN", "ATENDENTE")
                        .requestMatchers("/api/v1/solicitantes", "/api/v1/solicitantes/**").hasAnyRole("ADMIN", "ATENDENTE")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/setores", "/api/v1/setores/**",
                                "/api/v1/equipamentos", "/api/v1/equipamentos/**",
                                "/api/v1/tipos-equipamento", "/api/v1/tipos-equipamento/**")
                                .hasAnyRole("ADMIN", "ATENDENTE", "TECNICO")
                        .requestMatchers("/api/v1/setores", "/api/v1/setores/**",
                                "/api/v1/equipamentos", "/api/v1/equipamentos/**",
                                "/api/v1/tipos-equipamento", "/api/v1/tipos-equipamento/**")
                                .hasAnyRole("ADMIN", "ATENDENTE")
                        .requestMatchers("/api/v1/agendamentos", "/api/v1/agendamentos/**")
                                .hasAnyRole("ADMIN", "ATENDENTE", "TECNICO")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
