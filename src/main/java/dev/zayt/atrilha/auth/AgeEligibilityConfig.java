package dev.zayt.atrilha.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Configuração de beans usados pela elegibilidade por idade (US-005).
 *
 * <p>O {@link Clock} é fixado em {@code America/Sao_Paulo} para que "hoje"
 * tenha uma interpretação determinística do produto, independente da
 * timezone do container. Testes substituem este bean por
 * {@link Clock#fixed}.</p>
 */
@Configuration
class AgeEligibilityConfig {

    @Bean
    Clock atrilhaClock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
