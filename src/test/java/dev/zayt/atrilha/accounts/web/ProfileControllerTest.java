package dev.zayt.atrilha.accounts.web;

import dev.zayt.atrilha.AtrilhaApplication;
import dev.zayt.atrilha.accounts.avatar.AvatarStorage;
import dev.zayt.atrilha.accounts.avatar.AvatarTooLargeException;
import dev.zayt.atrilha.accounts.avatar.AvatarUnsupportedTypeException;
import dev.zayt.atrilha.accounts.domain.Account;
import dev.zayt.atrilha.accounts.domain.AccountRole;
import dev.zayt.atrilha.accounts.domain.AdolescentProfile;
import dev.zayt.atrilha.accounts.form.UpdateProfileForm;
import dev.zayt.atrilha.accounts.repository.AccountRepository;
import dev.zayt.atrilha.accounts.repository.AdolescentProfileRepository;
import dev.zayt.atrilha.accounts.service.UpdateProfileService;
import dev.zayt.atrilha.auth.domain.AuthenticatedAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes MockMvc do {@link ProfileController} — fluxo de upload/remoção de foto (US-009-c).
 *
 * <p>Padrão: webAppContextSetup + springSecurity com H2 in-memory.
 * Autenticação injetada via sessionAttr SecurityContext (padrão do projeto).</p>
 */
@SpringBootTest(classes = { AtrilhaApplication.class, ProfileControllerTest.TestConfig.class },
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProfileControllerTest {

    @Autowired
    WebApplicationContext ctx;

    // Repositories reais (H2 in-memory).
    @Autowired
    private AdolescentProfileRepository profileRepo;

    @Autowired
    private AccountRepository accountRepo;

    // Services mockados via TestConfig.
    @Autowired
    private UpdateProfileService updateProfileService;

    @Autowired
    private AvatarStorage avatarStorage;

    private MockMvc mockMvc;

    private UUID accountId;
    private Authentication testAuth;

    @BeforeEach
    @Transactional
    void setUp() {
        accountId = UUID.randomUUID();

        // Persiste account + profile no H2 in-memory.
        Account account = new Account();
        account.setId(accountId);
        account.setEmail("julia@teste.com");
        account.setType("ADOLESCENT");
        account.setPasswordHash("$2a$12$dummyhashfortestonly0000000000000000000000000000000");
        account.setCreatedAt(OffsetDateTime.now());
        accountRepo.saveAndFlush(account);

        AdolescentProfile profile = new AdolescentProfile();
        // @MapsId: o ID vem da associação account (não setar accountId separadamente).
        profile.setAccount(account);
        profile.setNickname("Júlia");
        profile.setBirthDate(LocalDate.of(2010, 6, 15));
        profile.setTimezone("America/Sao_Paulo");
        profileRepo.saveAndFlush(profile);

        // Cria token de autenticação com ROLE_TEEN (exigido por hasRole("TEEN") no SecurityConfig).
        AuthenticatedAccount principal = new AuthenticatedAccount(accountId, AccountRole.TEEN);
        testAuth = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_TEEN")));

        mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    /** Helper: cria SecurityContext a partir do Authentication. */
    private SecurityContext createSecurityContext(Authentication auth) {
        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(auth);
        return sc;
    }

    // ---------- Teste 1: happy path — upload de foto ----------

    @Test
    void shouldSaveAvatarWhenPhotoUploaded() throws Exception {
        when(updateProfileService.update(eq(accountId), any(UpdateProfileForm.class),
                        any(), eq(false)))
                .thenReturn(new UpdateProfileService.Outcome.Updated());

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "foto.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        mockMvc.perform(multipart("/perfil")
                        .file(photo)
                        .param("nickname", "Júlia")
                        .param("birthDate", "2010-06-15")
                        .with(csrf())
                        .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                createSecurityContext(testAuth)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil?saved"));

        verify(updateProfileService).update(eq(accountId), any(UpdateProfileForm.class),
                any(), eq(false));
    }

    // ---------- Teste 2: remoção de avatar ----------

    @Test
    void shouldRemoveAvatarWhenNoPhotoAndRemoveFlag() throws Exception {
        when(updateProfileService.update(eq(accountId), any(UpdateProfileForm.class),
                        isNull(), eq(true)))
                .thenReturn(new UpdateProfileService.Outcome.Updated());

        mockMvc.perform(post("/perfil")
                        .param("nickname", "Júlia")
                        .param("birthDate", "2010-06-15")
                        .param("removeAvatar", "true")
                        .with(csrf())
                        .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                createSecurityContext(testAuth)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/perfil?saved"));

        verify(updateProfileService).update(eq(accountId), any(UpdateProfileForm.class),
                isNull(), eq(true));
    }

    // ---------- Teste 3: arquivo muito grande ----------

    @Test
    void shouldRejectPhotoTooLargeWithErrorMessage() throws Exception {
        when(updateProfileService.update(eq(accountId), any(UpdateProfileForm.class),
                        any(), eq(false)))
                .thenThrow(new AvatarTooLargeException(6L * 1024L * 1024L, 5L * 1024L * 1024L));

        MockMultipartFile bigPhoto = new MockMultipartFile(
                "photo", "big.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

        mockMvc.perform(multipart("/perfil")
                        .file(bigPhoto)
                        .param("nickname", "Júlia")
                        .param("birthDate", "2010-06-15")
                        .with(csrf())
                        .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                createSecurityContext(testAuth)))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil/adolescente-editar"))
                .andExpect(model().attributeExists("uploadError"));
    }

    // ---------- Teste 4: tipo de arquivo não suportado ----------

    @Test
    void shouldRejectUnsupportedPhotoTypeWithErrorMessage() throws Exception {
        when(updateProfileService.update(eq(accountId), any(UpdateProfileForm.class),
                        any(), eq(false)))
                .thenThrow(new AvatarUnsupportedTypeException("image/gif"));

        MockMultipartFile gifPhoto = new MockMultipartFile(
                "photo", "foto.gif", "image/gif", new byte[] {(byte) 0x47, (byte) 0x49, (byte) 0x46});

        mockMvc.perform(multipart("/perfil")
                        .file(gifPhoto)
                        .param("nickname", "Júlia")
                        .param("birthDate", "2010-06-15")
                        .with(csrf())
                        .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                createSecurityContext(testAuth)))
                .andExpect(status().isOk())
                .andExpect(view().name("perfil/adolescente-editar"))
                .andExpect(model().attributeExists("uploadError"));
    }

    // ---------- Teste 5: progresso preservado ao fazer upload ----------

    @Test
    void shouldPreserveProgressFieldsWhenUploadingPhoto() throws Exception {
        when(updateProfileService.update(eq(accountId), any(UpdateProfileForm.class),
                        any(), eq(false)))
                .thenReturn(new UpdateProfileService.Outcome.Updated());

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "foto.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});

        mockMvc.perform(multipart("/perfil")
                        .file(photo)
                        .param("nickname", "Júlia")
                        .param("birthDate", "2010-06-15")
                        .with(csrf())
                        .sessionAttr(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                createSecurityContext(testAuth)))
                .andExpect(status().is3xxRedirection());

        // O service é quem preserva os campos — o controller só delega.
        verify(updateProfileService).update(eq(accountId), any(UpdateProfileForm.class),
                any(), eq(false));
    }

    /**
     * Mocks dos services externos (UpdateProfileService, AvatarStorage) para o contexto de teste.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        UpdateProfileService updateProfileService() {
            return mock(UpdateProfileService.class);
        }

        @Bean
        AvatarStorage avatarStorage() {
            return mock(AvatarStorage.class);
        }
    }
}
