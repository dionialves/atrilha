// Stub do Sprint 3: contas-semente em memória para US-007 antes de US-001/002/003/004
// persistirem usuários. Substituir por implementação JPA nas próximas USs.

package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação stub de {@link LoginAccountQuery} que carrega contas-semente
 * a partir das properties (prefixo {@code atrilha.auth.seed}).
 *
 * <p>Este bean só é carregado quando a propriedade
 * {@code atrilha.auth.seed.enabled=true} está explicitamente definida.
 * Em produção (default), {@link JpaLoginAccountQuery} é o bean ativo.</p>
 *
 * <p>Útil em dev local quando se quer popular contas-fantasma sem ir pelo
 * cadastro real, ou em testes unitários que dependem de seeds determinísticas.</p>
 */
@Component
@ConfigurationProperties(prefix = "atrilha.auth.seed")
@ConditionalOnProperty(name = "atrilha.auth.seed.enabled", havingValue = "true", matchIfMissing = false)
public class InMemoryLoginAccountQuery implements LoginAccountQuery {

    private SeedConfig teen;
    private SeedConfig guardianLinked;
    private SeedConfig guardianUnlinked;

    private final PasswordEncoder passwordEncoder;
    private final Map<String, LoginAccount> accounts = new HashMap<>();

    public InMemoryLoginAccountQuery(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void init() {
        if (teen != null) {
            accounts.put(teen.email().toLowerCase(), toAccount(teen));
        }
        if (guardianLinked != null) {
            accounts.put(guardianLinked.email().toLowerCase(), toAccount(guardianLinked));
        }
        if (guardianUnlinked != null) {
            accounts.put(guardianUnlinked.email().toLowerCase(), toAccount(guardianUnlinked));
        }
    }

    private LoginAccount toAccount(SeedConfig seed) {
        String email = seed.email();
        String displayName;
        if (seed.displayName() != null && !seed.displayName().isBlank()) {
            displayName = seed.displayName();
        } else if (email == null || email.isBlank()) {
            displayName = "";
        } else {
            int at = email.indexOf('@');
            displayName = at > 0 ? email.substring(0, at) : email;
        }

        String encodedPassword = passwordEncoder.encode(seed.password());

        return new LoginAccount(
                email != null ? email.toLowerCase() : "",
                encodedPassword,
                seed.role(),
                seed.hasGuardianLink(),
                displayName);
    }

    @Override
    public Optional<LoginAccount> findForLogin(String emailLowercase) {
        return Optional.ofNullable(accounts.get(emailLowercase.toLowerCase()));
    }

    // ---- Setters para @ConfigurationProperties ----

    public void setTeen(SeedConfig teen) {
        this.teen = teen;
    }

    public void setGuardianLinked(SeedConfig guardianLinked) {
        this.guardianLinked = guardianLinked;
    }

    public void setGuardianUnlinked(SeedConfig guardianUnlinked) {
        this.guardianUnlinked = guardianUnlinked;
    }

    // ---- DTO interno para binding das properties ----

    public static class SeedConfig {
        private String email;
        private String password;
        private AccountRole role;
        private boolean hasGuardianLink;
        private String displayName;

        public String email() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String password() { return password; }
        public void setPassword(String password) { this.password = password; }

        public AccountRole role() { return role; }
        public void setRole(AccountRole role) { this.role = role; }

        public boolean hasGuardianLink() { return hasGuardianLink; }
        public void setHasGuardianLink(boolean hasGuardianLink) { this.hasGuardianLink = hasGuardianLink; }

        public String displayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }
}
