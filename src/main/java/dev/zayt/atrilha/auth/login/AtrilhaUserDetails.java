package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.AccountRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapta {@link LoginAccount.LoginAccount} para o contrato {@link UserDetails}
 * do Spring Security.
 *
 * <p>Exposições extras para o template de placeholder (US-007):
 * <ul>
 *   <li>{@code getRole()} — papel da conta (TEEN / GUARDIAN)</li>
 *   <li>{@code hasGuardianLink()} — se o responsável tem filho vinculado</li>
 *   <li>{@code getAccount()} — a conta original para leitura no template</li>
 * </ul>
 */
public class AtrilhaUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final LoginAccountQuery.LoginAccount account;

    public AtrilhaUserDetails(LoginAccountQuery.LoginAccount account) {
        this.account = account;
    }

    /** Retorna o papel da conta (TEEN ou GUARDIAN). */
    public AccountRole getRole() {
        return account.role();
    }

    /** Retorna true se o responsável tem ao menos um filho vinculado. */
    public boolean hasGuardianLink() {
        return account.hasGuardianLink();
    }

    /** Retorna a conta original de login. */
    public LoginAccountQuery.LoginAccount getAccount() {
        return account;
    }

    // ---- UserDetails ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + account.role().name()));
    }

    @Override
    public String getPassword() {
        return account.passwordHashBcrypt();
    }

    @Override
    public String getUsername() {
        return account.email();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
