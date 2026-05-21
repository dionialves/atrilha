package dev.zayt.atrilha.auth.login;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * {@link org.springframework.security.core.userdetails.UserDetailsService} que
 * busca a conta de login pelo e-mail (US-007).
 *
 * <p>Delega para {@link LoginAccountQuery#findForLogin(String)} e retorna um
 * {@link AtrilhaUserDetails} com a autoridade {@code ROLE_TEEN} ou
 * {@code ROLE_GUARDIAN}. Lança {@link UsernameNotFoundException} se a conta
 * não for encontrada.</p>
 */
@Service
class LoginAccountUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(LoginAccountUserDetailsService.class);

    private final LoginAccountQuery loginAccountQuery;

    LoginAccountUserDetailsService(LoginAccountQuery loginAccountQuery) {
        this.loginAccountQuery = loginAccountQuery;
    }

    @Override
    public UserDetails loadUserByUsername(String emailLowercase) throws UsernameNotFoundException {
        LoginAccountQuery.LoginAccount account = loginAccountQuery.findForLogin(emailLowercase)
                .orElseThrow(() -> new UsernameNotFoundException("Conta não encontrada para: " + emailLowercase));

        log.info("auth.login.user_found email_hash={} role={}",
                hash(emailLowercase), account.role());

        return new AtrilhaUserDetails(account);
    }

    private static String hash(String value) {
        return Integer.toHexString(value.hashCode());
    }
}
