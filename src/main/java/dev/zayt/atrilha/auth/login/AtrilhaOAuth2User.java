package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.AccountRole;
import dev.zayt.atrilha.auth.AuthenticatedPrincipal;
import dev.zayt.atrilha.auth.login.LoginAccountQuery;
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

    // ---- AuthenticatedPrincipal ----

    @Override
    public AccountRole role() {
        return account.role();
    }

    @Override
    public String displayName() {
        return account.displayName();
    }

    @Override
    public boolean hasGuardianLink() {
        return account.hasGuardianLink();
    }

    @Override
    public LoginAccountQuery.LoginAccount getAccount() {
        return account;
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
        return account.email();
    }
}
