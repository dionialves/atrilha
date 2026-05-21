package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.config.LoginRateLimitProperties;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de rate-limit in-memory para proteção contra brute-force no login.
 *
 * <p>Chave = IP + e-mail normalizado. Armazenamento 100% em memória
 * ({@link ConcurrentHashMap}) — limitação consciente do MVP, documentada
 * como dívida pós-MVP (sem Redis/clustering).</p>
 */
@Service
public class LoginAttemptService {

    private final LoginRateLimitProperties properties;
    private final Clock clock;
    private final ConcurrentHashMap<LoginAttemptKey, AttemptState> store = new ConcurrentHashMap<>();

    public LoginAttemptService(LoginRateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Verifica se a chave está atualmente bloqueada.
     *
     * <p>Se a janela de tentativas já expirou e não há bloqueio ativo,
     * o estado é limpo do mapa.</p>
     */
    public boolean isBlocked(LoginAttemptKey key) {
        AttemptState state = store.get(key);
        if (state == null) return false;

        Instant now = clock.instant();

        // Se já foi bloqueado e o período de bloqueio expirou, libera
        if (state.blockedUntil() != null && now.isAfter(state.blockedUntil())) {
            store.remove(key);
            return false;
        }

        // Se a janela de tentativas expirou, reseta contagem (não bloqueia)
        if (state.windowStart() != null && now.isAfter(state.windowStart().plus(properties.attemptWindow()))) {
            store.remove(key);
            return false;
        }

        // Ainda dentro da janela — verifica se está bloqueado
        return state.blockedUntil() != null;
    }

    /**
     * Registra uma tentativa falha.
     *
     * <p>Se a janela atual expirou, reinicia a contagem. Quando atinge
     * {@code maxAttempts}, grava o tempo de bloqueio.</p>
     */
    public void registerFailure(LoginAttemptKey key) {
        Instant now = clock.instant();

        store.compute(key, (k, state) -> {
            if (state == null || isWindowExpired(state.windowStart(), now)) {
                // Janela expirada ou primeira tentativa — reseta
                return new AttemptState(1, now, null);
            }

            int nextCount = state.count() + 1;
            if (nextCount >= properties.maxAttempts()) {
                // Atingiu o limite — bloqueia
                return new AttemptState(nextCount, state.windowStart(),
                        now.plus(properties.blockDuration()));
            }
            return new AttemptState(nextCount, state.windowStart(), null);
        });
    }

    /**
     * Registra uma tentativa bem-sucedida — remove a chave do mapa.
     */
    public void registerSuccess(LoginAttemptKey key) {
        store.remove(key);
    }

    private boolean isWindowExpired(Instant windowStart, Instant now) {
        return windowStart == null || now.isAfter(windowStart.plus(properties.attemptWindow()));
    }

    /**
     * Estado interno de uma chave no mapa.
     */
    record AttemptState(int count, Instant windowStart, Instant blockedUntil) {
        AttemptState {
            if (count < 0) throw new IllegalArgumentException("count não pode ser negativo");
        }
    }
}
