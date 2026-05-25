package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Adapta um {@link OAuth2User} (Google) para o contrato do Spring Security com
 * authorities derivadas da conta no banco.
 *
 * <p>Construído pelo {@link dev.zayt.atrilha.auth.GoogleOAuth2UserService}
 * durante o callback OAuth. Implementa {@link AuthenticatedPrincipal} para que
 * controllers possam ler role, displayName e hasGuardianLink sem se importar
 * com a origem da autenticação.</p>
 *
 * <p>Diferente do {@code DefaultOAuth2User}, NAO inclui authorities
 * {@code OAUTH2_USER} / {@code SCOPE_*} — apenas o role da conta (TEEN ou
 * GUARDIAN). Isso evita o bug de 403 onde o principal OAuth não tinha
 * {@code ROLE_TEEN} e o AuthorizationManager barrava a request.</p>
 *
 * <p><b>Estado PENDING_SIGNUP:</b> quando o e-mail Google não corresponde a
 * nenhuma conta no banco, o construtor {@link #pendingSignup(String, Map)} cria
 * uma instância com {@code account == null} e authorities vazias. Métodos que
 * dependem da conta ({@code role()}, {@code displayName()}, etc.) lançam
 * {@link IllegalStateException} nesse estado — o chamador deve verificar
 * {@link #isPendingSignup()} antes.</p>
 *
 * <p>Visibilidade package-private; o {@code SecurityConfig} resolve via DI.</p>
 */
public class AtrilhaOAuth2User implements OAuth2User, AuthenticatedPrincipal {

    private static final long serialVersionUID = 1L;

    private final LoginAccountQuery.LoginAccount account;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Construtor a partir da conta resolvida e dos atributos OAuth crus.
     */
    public AtrilhaOAuth2User(LoginAccountQuery.LoginAccount account, Map<String, Object> oauthAttributes) {
        this.account = account;
        this.attributes = oauthAttributes != null ? Map.copyOf(oauthAttributes) : Map.of();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name()));
    }

    /**
     * Construtor para estado PENDING_SIGNUP — e-mail Google sem conta no banco.
     *
     * <p>Usado pelo {@link dev.zayt.atrilha.auth.GoogleOAuth2UserService} quando
     * {@code findForLogin(email)} retorna vazio. O dispatcher no success handler
     * identifica esse estado e encaminha para o fluxo de cadastro novo.</p>
     */
    private AtrilhaOAuth2User(Map<String, Object> oauthAttributes) {
        this.account = null;
        this.attributes = oauthAttributes != null ? Map.copyOf(oauthAttributes) : Map.of();
        this.authorities = List.of();
    }

    /**
     * Fábrica para estado PENDING_SIGNUP.
     */
    public static AtrilhaOAuth2User pendingSignup(String email, Map<String, Object> oauthAttributes) {
        // Normaliza o e-mail para lowercase (mesma regra do GoogleOAuth2UserService)
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        Map<String, Object> attrs;
        if (oauthAttributes == null) {
            attrs = Map.of("email", normalizedEmail);
        } else if (oauthAttributes.containsKey("email")) {
            // Substituir o email original pelo normalizado para consistencia na sessao
            attrs = new java.util.HashMap<>(oauthAttributes);
            attrs.put("email", normalizedEmail);
        } else {
            attrs = Map.copyOf(oauthAttributes);
        }
        return new AtrilhaOAuth2User(attrs);
    }

    // ---- AuthenticatedPrincipal ----

    @Override
    public AccountRole role() {
        if (account == null) {
            throw new IllegalStateException("AtrilhaOAuth2User em estado PENDING_SIGNUP — verifique isPendingSignup() antes");
        }
        return account.role();
    }

    @Override
    public String displayName() {
        if (account == null) {
            throw new IllegalStateException("AtrilhaOAuth2User em estado PENDING_SIGNUP — verifique isPendingSignup() antes");
        }
        return account.displayName();
    }

    @Override
    public boolean hasGuardianLink() {
        if (account == null) {
            throw new IllegalStateException("AtrilhaOAuth2User em estado PENDING_SIGNUP — verifique isPendingSignup() antes");
        }
        return account.hasGuardianLink();
    }

    @Override
    public LoginAccountQuery.LoginAccount getAccount() {
        if (account == null) {
            throw new IllegalStateException("AtrilhaOAuth2User em estado PENDING_SIGNUP — verifique isPendingSignup() antes");
        }
        return account;
    }

    /** Indica se esta instância representa um e-mail Google sem conta no banco. */
    public boolean isPendingSignup() {
        return account == null;
    }

    // ---- OAuth2User ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        // getName() sempre funciona — retorna o email dos atributos OAuth
        if (account != null) {
            return account.email();
        }
        return (String) attributes.get("email");
    }
}
