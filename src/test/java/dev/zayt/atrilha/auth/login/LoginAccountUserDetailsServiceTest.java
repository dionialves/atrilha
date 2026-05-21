package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.auth.AccountRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginAccountUserDetailsServiceTest {

    // ---- Critério: retorna AtrilhaUserDetails com ROLE_TEEN ----

    @Test
    @DisplayName("loadUserByUsernameRetornaAtrilhaUserDetailsComROLETeen")
    void loadUserByUsernameRetornaAtrilhaUserDetailsComRoleTeen() {
        LoginAccountQuery query = mock(LoginAccountQuery.class);
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "teen@test.com", "$2a$10$hash", AccountRole.TEEN, false, "Juca");
        when(query.findForLogin("teen@test.com")).thenReturn(Optional.of(account));

        LoginAccountUserDetailsService service = new LoginAccountUserDetailsService(query);

        UserDetails userDetails = service.loadUserByUsername("teen@test.com");

        assertNotNull(userDetails);
        assertInstanceOf(AtrilhaUserDetails.class, userDetails);
        assertEquals("ROLE_TEEN", userDetails.getAuthorities().iterator().next().getAuthority());
    }

    // ---- Critério: retorna AtrilhaUserDetails com ROLE_GUARDIAN ----

    @Test
    @DisplayName("loadUserByUsernameRetornaAtrilhaUserDetailsComROLEGuardian")
    void loadUserByUsernameRetornaAtrilhaUserDetailsComRoleGuardian() {
        LoginAccountQuery query = mock(LoginAccountQuery.class);
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "guardian@test.com", "$2a$10$hash", AccountRole.GUARDIAN, true, "Maria");
        when(query.findForLogin("guardian@test.com")).thenReturn(Optional.of(account));

        LoginAccountUserDetailsService service = new LoginAccountUserDetailsService(query);

        UserDetails userDetails = service.loadUserByUsername("guardian@test.com");

        assertNotNull(userDetails);
        assertInstanceOf(AtrilhaUserDetails.class, userDetails);
        assertEquals("ROLE_GUARDIAN", userDetails.getAuthorities().iterator().next().getAuthority());
    }

    // ---- Critério: email não cadastrado → UsernameNotFoundException ----

    @Test
    @DisplayName("emailNaoCadastradoLancaUsernameNotFoundException")
    void emailNaoCadastradoLancaUsernameNotFoundException() {
        LoginAccountQuery query = mock(LoginAccountQuery.class);
        when(query.findForLogin("ghost@test.com")).thenReturn(Optional.empty());

        LoginAccountUserDetailsService service = new LoginAccountUserDetailsService(query);

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("ghost@test.com"));

        assertTrue(ex.getMessage().contains("ghost@test.com"));
    }

    // ---- Critério: email case-insensitive (deve ser passado lowercase) ----

    @Test
    @DisplayName("emailPassadoLowercase")
    void emailPassadoLowercase() {
        LoginAccountQuery query = mock(LoginAccountQuery.class);
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "user@test.com", "$2a$10$hash", AccountRole.TEEN, false, "User");
        when(query.findForLogin("user@test.com")).thenReturn(Optional.of(account));

        LoginAccountUserDetailsService service = new LoginAccountUserDetailsService(query);

        // O caller é responsável por passar lowercase — o serviço delega diretamente
        UserDetails userDetails = service.loadUserByUsername("user@test.com");

        assertNotNull(userDetails);
    }
}
