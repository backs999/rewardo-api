package travel.rewardo.rewardapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureWebTestClient
public class OpenApiConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testOpenApiDocsEndpoint() {
        webTestClient.get().uri("/api-docs")
                .exchange()
                .expectStatus().isPermanentRedirect()
                .expectHeader().valueEquals("Location", "/openapi.json");
    }

    @Test
    public void testSwaggerUiEndpoint() {
        webTestClient.get().uri("/swagger-ui")
                .exchange()
                .expectStatus().isPermanentRedirect()
                .expectHeader().valueEquals("Location", "/swagger-ui.html");
    }
    
    @Test
    public void testOpenApiJsonEndpoint() {
        webTestClient.get().uri("/openapi.json")
                .exchange()
                .expectStatus().isOk();
    }
    
    @Test
    public void testSwaggerUiHtmlEndpoint() {
        webTestClient.get().uri("/swagger-ui.html")
                .exchange()
                .expectStatus().isOk();
    }
}