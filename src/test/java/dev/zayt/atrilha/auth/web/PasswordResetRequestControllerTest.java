package dev.zayt.atrilha.auth.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.repository.AccountReader;
import dev.zayt.atrilha.auth.verification.PasswordResetSender;
import dev.zayt.atrilha.auth.verification.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de unidade do {@link PasswordResetRequestController} (US-008-c).
 *
 * <p>Cobre: renderização do formulário, emissão de token + envio de e-mail para
 * conta existente, anti-oracle para e-mail inexistente, validação de e-mail
 * malformado e vazio.</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, PasswordResetRequestControllerTest.TestBeans.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PasswordResetRequestControllerTest {

    @Autowired
    private WebApplicationContext ctx;

    @Autowired
    private AccountReader accountReader;

    @Autowired
    private PasswordResetService passwordResetService;

    // Mock — verifica interações via Mockito.
    @Autowired
    private PasswordResetSender passwordResetSender;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    /**
     * Teste 1 — GET renderiza formulário.
     */
    @Test
    void shouldRenderFormWhenGet() throws Exception {
        mvc.perform(get("/esqueci-senha"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/esqueci-senha"))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("action=\"/esqueci-senha\"")));
    }

    /**
     * Teste 2 — POST com e-mail de conta existente emite token + envia e-mail.
     */
    @Test
    void shouldIssueTokenAndSendEmailWhenAccountExists() throws Exception {
        Account fakeAccount = new Account();
        fakeAccount.setId(UUID.randomUUID());
        fakeAccount.setEmail("ana@teste.com");
        fakeAccount.setType("ADOLESCENT");
        fakeAccount.setPasswordHash("$2a$12$dummyhashfortestonly0000000000000000000000000000000");
        fakeAccount.setCreatedAt(OffsetDateTime.now());
        when(accountReader.findByEmailIgnoreCase("ana@teste.com"))
                .thenReturn(java.util.Optional.of(fakeAccount));

        mvc.perform(post("/esqueci-senha")
                        .param("email", "ana@teste.com")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/esqueci-senha?enviado=1"));

        verify(passwordResetService).issueToken(any(Account.class));
        verify(passwordResetSender).sendReset(eq("ana@teste.com"), any(), any());
    }

    /**
     * Teste 3 — POST com e-mail não cadastrado NÃO emite token nem envia e-mail (anti-oracle).
     */
    @Test
    void shouldNotEmitTokenWhenEmailNotFound() throws Exception {
        when(accountReader.findByEmailIgnoreCase("inexistente@teste.com"))
                .thenReturn(java.util.Optional.empty());

        mvc.perform(post("/esqueci-senha")
                        .param("email", "inexistente@teste.com")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/esqueci-senha?enviado=1"));

        verify(passwordResetService, never()).issueToken(any(Account.class));
        verify(passwordResetSender, never()).sendReset(any(), any(), any());
    }

    /**
     * Teste 4 — POST com e-mail malformado retorna erro de validação.
     */
    @Test
    void shouldReturnValidationErrorsWhenEmailMalformed() throws Exception {
        mvc.perform(post("/esqueci-senha")
                        .param("email", "nao-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Verifique se o e-mail informado está correto.")));

        verify(passwordResetService, never()).issueToken(any(Account.class));
    }

    /**
     * Teste 5 — POST com e-mail vazio retorna erro de validação.
     */
    @Test
    void shouldReturnValidationErrorsWhenEmailEmpty() throws Exception {
        mvc.perform(post("/esqueci-senha")
                        .param("email", "")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Informe seu e-mail para continuar.")));

        verify(passwordResetService, never()).issueToken(any(Account.class));
    }

    /**
     * Configuração de beans para o contexto de teste.
     */
    @TestConfiguration
    static class TestBeans {

        @Bean
        AccountReader accountReader() {
            return mock(AccountReader.class);
        }

        @Bean
        PasswordResetService passwordResetService() {
            return mock(PasswordResetService.class);
        }

        // Mock simples — verifica interações via Mockito.
        // Sem @Primary: Spring escolhe por nome (campo autowired = "passwordResetSender").
        @Bean
        PasswordResetSender passwordResetSender() {
            return mock(PasswordResetSender.class);
        }

        @Bean
        MessageSource messageSource() {
            var ms = new ReloadableResourceBundleMessageSource();
            ms.setBasename("classpath:messages");
            ms.setDefaultEncoding("UTF-8");
            return ms;
        }

        @Bean
        LocalValidatorFactoryBean validator(MessageSource messageSource) {
            var v = new LocalValidatorFactoryBean();
            v.setProviderClass(org.hibernate.validator.HibernateValidator.class);
            v.setValidationMessageSource(messageSource);
            return v;
        }
    }
}
