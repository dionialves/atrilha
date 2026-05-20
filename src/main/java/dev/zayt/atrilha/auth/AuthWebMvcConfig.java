package dev.zayt.atrilha.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra interceptors do módulo {@code auth} no pipeline MVC do Spring.
 *
 * <p>Por enquanto cobre apenas {@link RequiresVerifiedEmailInterceptor}
 * (US-006). À medida que outros interceptors do {@code auth} entrarem
 * (rate-limit por IP — US-022, etc.) ficarão aqui também.</p>
 *
 * <p>Visibilidade package-private — não é referenciado fora do módulo.</p>
 */
@Configuration
class AuthWebMvcConfig implements WebMvcConfigurer {

    private final RequiresVerifiedEmailInterceptor requiresVerifiedEmailInterceptor;

    AuthWebMvcConfig(RequiresVerifiedEmailInterceptor requiresVerifiedEmailInterceptor) {
        this.requiresVerifiedEmailInterceptor = requiresVerifiedEmailInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requiresVerifiedEmailInterceptor);
    }
}
