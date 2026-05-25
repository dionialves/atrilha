package dev.zayt.atrilha.auth.login;

import dev.zayt.atrilha.accounts.domain.AccountRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtrilhaUserDetailsTest {

    // ---- Critério: getRole retorna o papel da conta ----

    @Test
    @DisplayName("getRoleRetornaTEEN")
    void getRoleRetornaTeen() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "teen@test.com", "$2a$10$hash", AccountRole.TEEN, false, "Juca");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertEquals(AccountRole.TEEN, details.getRole());
    }

    @Test
    @DisplayName("getRoleRetornaGUARDIAN")
    void getRoleRetornaGuardian() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "guardian@test.com", "$2a$10$hash", AccountRole.GUARDIAN, true, "Maria");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertEquals(AccountRole.GUARDIAN, details.getRole());
    }

    // ---- Critério: hasGuardianLink retorna o valor correto ----

    @Test
    @DisplayName("hasGuardianLinkRetornaTrue")
    void hasGuardianLinkRetornaTrue() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "guardian@test.com", "$2a$10$hash", AccountRole.GUARDIAN, true, "Maria");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertTrue(details.hasGuardianLink());
    }

    @Test
    @DisplayName("hasGuardianLinkRetornaFalse")
    void hasGuardianLinkRetornaFalse() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "teen@test.com", "$2a$10$hash", AccountRole.TEEN, false, "Juca");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertFalse(details.hasGuardianLink());
    }

    // ---- Critério: getAccount retorna a conta original ----

    @Test
    @DisplayName("getAccountRetornaContaOriginal")
    void getAccountRetornaContaOriginal() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "user@test.com", "$2a$10$hash", AccountRole.TEEN, false, "User");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertSame(account, details.getAccount());
    }

    // ---- Critério: getAuthorities retorna ROLE_TEEN / ROLE_GUARDIAN ----

    @Test
    @DisplayName("getAuthoritiesRetornaROLETeen")
    void getAuthoritiesRetornaRoleTeen() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "teen@test.com", "$2a$10$hash", AccountRole.TEEN, false, "Juca");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_TEEN", authorities.iterator().next().getAuthority());
    }

    @Test
    @DisplayName("getAuthoritiesRetornaROLEGuardian")
    void getAuthoritiesRetornaRoleGuardian() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "guardian@test.com", "$2a$10$hash", AccountRole.GUARDIAN, true, "Maria");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        Collection<? extends GrantedAuthority> authorities = details.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_GUARDIAN", authorities.iterator().next().getAuthority());
    }

    // ---- Critério: getPassword retorna o hash bcrypt ----

    @Test
    @DisplayName("getPasswordRetornaHashBcrypt")
    void getPasswordRetornaHashBcrypt() {
        String expectedHash = "$2a$10$rKY9bXZ3qJ5wN8pLmVcOxeF2gH4jK6lMnOpQrStUvWxYz";
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "user@test.com", expectedHash, AccountRole.TEEN, false, "User");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertEquals(expectedHash, details.getPassword());
    }

    // ---- Critério: getUsername retorna o email ----

    @Test
    @DisplayName("getUsernameRetornaEmail")
    void getUsernameRetornaEmail() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "user@test.com", "$2a$10$hash", AccountRole.TEEN, false, "User");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertEquals("user@test.com", details.getUsername());
    }

    // ---- Critério: isAccountNonLocked retorna true ----

    @Test
    @DisplayName("isAccountNonLockedRetornaTrue")
    void isAccountNonLockedRetornaTrue() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "user@test.com", "$2a$10$hash", AccountRole.TEEN, false, "User");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertTrue(details.isAccountNonLocked());
    }

    // ---- Critério: isEnabled retorna true ----

    @Test
    @DisplayName("isEnabledRetornaTrue")
    void isEnabledRetornaTrue() {
        LoginAccountQuery.LoginAccount account = new LoginAccountQuery.LoginAccount(
                "user@test.com", "$2a$10$hash", AccountRole.TEEN, false, "User");
        AtrilhaUserDetails details = new AtrilhaUserDetails(account);

        assertTrue(details.isEnabled());
    }
}
