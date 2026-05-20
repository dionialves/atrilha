package dev.zayt.atrilha.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração mínima de Spring Security para o início do épico E1 (US-001).
 *
 * <p>Rotas públicas (não autenticadas): tela inicial, fluxo de cadastro,
 * estáticos, health, media de avatares e páginas de erro. Tudo mais exige
 * sessão autenticada (preparação para US-006/US-007).</p>
 *
 * <p>CSRF habilitado por default (Spring Boot/Security 7) — Thymeleaf injeta
 * o token automaticamente em POSTs de formulário. Login form e HTTP Basic
 * são desabilitados; o fluxo de autenticação dessa US é via
 * {@link SessionAuthenticator} imediatamente após cadastro bem-sucedido.</p>
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        // /verify-email é o endpoint público do link do e-mail
                        // (US-006). O token é a única credencial — qualquer
                        // sessão (logada ou não) pode acessar a tela de
                        // resultado.
                        .requestMatchers("/verify-email").permitAll()
                        // Tela "Confirme teu e-mail" + reenvio: precisam de
                        // sessão autenticada para identificar o usuário.
                        .requestMatchers("/verificar-email", "/verificar-email/reenviar").authenticated()
                        // Demais rotas seguem públicas (cadastro, 404, etc.).
                        .anyRequest().permitAll()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
