package dev.zayt.atrilha;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotFoundPageTest {

    @LocalServerPort
    int port;

    @Test
    void rotaInexistenteRetorna404ComTemplateDedicado() {
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.TEXT_HTML));
        RequestEntity<Void> request = new RequestEntity<>(headers, org.springframework.http.HttpMethod.GET,
                URI.create("http://localhost:" + port + "/rota-inexistente"));
        try {
            rest.exchange(request, String.class);
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(e.getResponseBodyAsString()).contains("Página não encontrada");
            return;
        }
        fail("Esperava HTTP 404");
    }
}
