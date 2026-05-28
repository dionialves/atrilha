package dev.zayt.atrilha.accounts;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.zayt.atrilha.notifications.RecordingEmailSenderTestConfig;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração da tela de escolha de método de autenticação do responsável (US-004).
 */
@Testcontainers
@SpringBootTest
@Import(RecordingEmailSenderTestConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class GuardianEscolherMetodoIT {

    @Autowired
    private WebApplicationContext ctx;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldReturn200WithEscolherMetodoView() throws Exception {
        mvc.perform(get("/cadastro/responsavel/escolher-metodo"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    Document doc = Jsoup.parse(result.getResponse().getContentAsString());

                    Element googleBtn = doc.selectFirst("button[data-test=cta-google-disabled]");
                    assertThat(googleBtn).as("botão Google deve existir").isNotNull();
                    assertThat(googleBtn.hasAttr("disabled")).as("botão Google deve estar disabled").isTrue();
                    assertThat(googleBtn.attr("aria-disabled")).isEqualTo("true");

                    Element appleBtn = doc.selectFirst("button[data-test=cta-apple-disabled]");
                    assertThat(appleBtn).as("botão Apple deve existir").isNotNull();
                    assertThat(appleBtn.hasAttr("disabled")).as("botão Apple deve estar disabled").isTrue();

                    Element emailLink = doc.selectFirst("a[href*=/cadastro/responsavel]");
                    assertThat(emailLink).as("link e-mail deve existir").isNotNull();
                });
    }

    @Test
    void shouldContainGuardianSpecificMicrocopy() throws Exception {
        mvc.perform(get("/cadastro/responsavel/escolher-metodo"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    Document doc = Jsoup.parse(result.getResponse().getContentAsString());
                    String text = doc.body().text();
                    assertThat(text).as("deve conter microcopy de responsável").contains("responsável");
                    assertThat(text).as("não deve conter texto do adolescente")
                            .doesNotContain("adolescente faz a trilha");
                });
    }
}
