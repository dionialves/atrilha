package dev.zayt.atrilha.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import dev.zayt.atrilha.auth.login.LoginAccountUserDetailsService;
import dev.zayt.atrilha.auth.login.LoginAttemptKey;
import dev.zayt.atrilha.auth.login.LoginAttemptService;
import dev.zayt.atrilha.auth.login.RateLimitedAuthenticationFailureHandler;
import dev.zayt.atrilha.auth.login.RoleBasedAuthenticationSuccessHandler;

/**
 * Configuração de Spring Security para autenticação por formulário.
 *
 * <p>Rotas públicas: tela inicial, login, cadastro (US-001), verificação de
 * e-mail, estáticos, health e páginas de erro.</p>
 *
 * <p>Rotas protegidas por papel:
 * <ul>
 *   <li>{@code /trilha/**} → {@code ROLE_TEEN}</li>
 *   <li>{@code /painel/**} → {@code ROLE_GUARDIAN}</li>
 *   <li>{@code /vincular/**} → {@code ROLE_GUARDIAN}</li>
 * </ul></p>
 *
 * <p>CSRF habilitado por default — Thymeleaf injeta o token automaticamente em
 * POSTs de formulário. HTTP Basic desabilitado.</p>
 *
 * <p>Rate-limit: {@link DaoAuthenticationProvider} com pre-authentication checks
 * customizado que consulta {@link LoginAttemptService} e lança
 * {@code LockedException} quando a chave IP+email está bloqueada. O handler de
 * falha mapeia para {@code /login?blocked}.</p>
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

    /**
     * Registry de sessões usado para invalidar todas as sessões ativas de um
     * usuário após redefinição de senha (US-008-d CA-4). É lido por
     * {@code PasswordResetController.invalidatePreviousSessions} e populado
     * pelo {@code HttpSessionEventPublisher} (registrado abaixo).
     */
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Publisher de eventos de sessão HTTP — sem ele o {@link SessionRegistryImpl}
     * não recebe notificações de criação/expiração e fica vazio. Necessário
     * para que a invalidação de sessões (US-008-d) funcione fim-a-fim.
     */
    @Bean
    org.springframework.security.web.session.HttpSessionEventPublisher httpSessionEventPublisher() {
        return new org.springframework.security.web.session.HttpSessionEventPublisher();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    SessionRegistry sessionRegistry,
                                    RoleBasedAuthenticationSuccessHandler roleBasedSuccessHandler,
                                    RateLimitedAuthenticationFailureHandler rateLimitedFailureHandler) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        // Rotas públicas — estáticos, health, erro
                        .requestMatchers("/", "/health", "/login",
                                "/css/**", "/img/**", "/js/**", "/error", "/error/**")
                                .permitAll()
                        // Fluxo de cadastro (US-001) — permanece público
                        .requestMatchers("/cadastro/**", "/comecar").permitAll()
                        // Verificação de e-mail
                        .requestMatchers("/verificar-email", "/verify-email").permitAll()
                        // Redefinição de senha (US-008-c solicita, US-008-d consome)
                        .requestMatchers("/esqueci-senha", "/reset-senha").permitAll()
                        // Rotas protegidas por papel
                        .requestMatchers("/trilha/**").hasRole("TEEN")
                        .requestMatchers("/painel/**").hasRole("GUARDIAN")
                        .requestMatchers("/vincular/**").hasRole("GUARDIAN")
                        // Reenvio de verificação exige sessão autenticada
                        .requestMatchers("/verificar-email/reenviar").authenticated()
                        // Rotas não listadas acima: permitem acesso (cadastro,
                        // 404, etc.). As rotas protegidas (/trilha/**, /painel/**,
                        // /vincular/**) já foram declaradas acima com hasRole().
                        .anyRequest().permitAll()
                )
                // fix-001: habilita session-url-rewriting para que o
                // DisableEncodeUrlFilter do Spring Security NAO seja
                // adicionado ao chain. Esse filter, quando ativo, envolve
                // a HttpServletResponse com um wrapper cujo encodeURL() e
                // no-op — o que neutraliza o ResourceUrlEncodingFilter do
                // Spring Web e impede o fingerprint de CSS configurado em
                // application-prod.properties. A app nao usa JSESSIONID em
                // URL em nenhum lugar (sessao 100% cookie-based,
                // HttpOnly), entao liberar essa flag e seguro. Referencia:
                // SessionManagementConfigurer.enableSessionUrlRewriting,
                // Spring Security 7.0.5, Javadoc cita ResourceUrlEncodingFilter
                // pelo nome como o caso de uso desta opcao.
                // US-008-d: registra cada sessão autenticada no SessionRegistry para
                // que o reset de senha possa invalidar todas as sessões pré-existentes
                // do usuário (CA-4). maximumSessions(-1) = ilimitado (apenas rastreia,
                // não bloqueia múltiplos dispositivos).
                .sessionManagement(s -> s
                        .enableSessionUrlRewriting(true)
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry))
                // US-007: Form login com handlers customizados
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(roleBasedSuccessHandler)
                        .failureHandler(rateLimitedFailureHandler)
                )
                // Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * {@link DaoAuthenticationProvider} com pre-authentication checks customizado
     * que verifica se a chave IP+email está bloqueada pelo rate-limit.
     *
     * <p>Isso garante que MESMO credenciais corretas sejam recusadas durante o
     * período de bloqueio. O {@link RateLimitedAuthenticationFailureHandler}
     * mapeia {@code LockedException} para {@code /login?blocked}.</p>
     */
    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(
            LoginAccountUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        // Pre-authentication check: verifica bloqueio antes de validar senha
        provider.setPreAuthenticationChecks(authentication -> {
            String username = authentication.getUsername();
            if (username == null || username.isBlank()) {
                return;
            }

            // Obtém o IP via RequestContextHolder (disponível no contexto do filtro)
            org.springframework.web.context.request.ServletRequestAttributes attrs =
                    (org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder
                                    .getRequestAttributes();
            if (attrs == null) {
                return;
            }

            String ip = attrs.getRequest().getRemoteAddr();
            LoginAttemptKey key = LoginAttemptKey.of(username, ip);

            if (loginAttemptService.isBlocked(key)) {
                throw new org.springframework.security.authentication.LockedException(
                        "Conta temporariamente bloqueada. Tente novamente mais tarde.");
            }
        });

        return provider;
    }

}
