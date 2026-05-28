package dev.zayt.atrilha.accounts;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integração da tela de escolha de método de autenticação do responsável (US-004).
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.datasource.url=${TESTCONTAINERS_URL}",
        "spring.mail.host=localhost",
        "spring.mail.port=1025"
})
class GuardianEscolherMetodoIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturn200WithEscolherMetodoView() {
        this.webTestClient.get()
                .uri("/cadastro/responsavel/escolher-metodo")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(body -> {
                    Document doc = Jsoup.parse(body.getResponseBody());

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
    void shouldContainGuardianSpecificMicrocopy() {
        this.webTestClient.get()
                .uri("/cadastro/responsavel/escolher-metodo")
                .exchange()
                .expectBody(String.class)
                .consumeWith(body -> {
                    Document doc = Jsoup.parse(body.getResponseBody());
                    String text = doc.body().text();
                    assertThat(text).as("deve conter microcopy de responsável").contains("responsável");
                    assertThat(text).as("não deve conter texto do adolescente").doesNotContain("adolescente faz a trilha");
                });
    }
}
