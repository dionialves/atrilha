package dev.zayt.atrilha.auth.session;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Estabelece sessão autenticada imediatamente após cadastro bem-sucedido
 * (US-001 CA-5: "usuário fica logado sem precisar logar de novo").
 *
 * <p>O principal é um {@link AuthenticatedAccount} — record público
 * carregando id + role — que serve para chamadores em outros módulos
 * recuperarem o usuário corrente sem expor entidades JPA.</p>
 *
 * <p>O {@link SecurityContextRepository} usado é {@code HttpSessionSecurityContextRepository}
 * (default do Spring Security em apps com sessão), gravando o contexto na
 * sessão HTTP para que requests subsequentes sejam reconhecidos.</p>
 */
@Component
public class SessionAuthenticator {

    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public void authenticate(HttpServletRequest request,
                             HttpServletResponse response,
                             UUID accountId,
                             AccountRole role) {
        AuthenticatedAccount principal = new AuthenticatedAccount(accountId, role);
        Authentication auth = UsernamePasswordAuthenticationToken.authenticated(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
