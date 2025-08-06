package travel.rewardo.rewardapi.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Controller to serve Swagger UI documentation
 */
@RestController
public class SwaggerController {

    /**
     * Redirects /api-docs to the static OpenAPI JSON file
     * @param response the server HTTP response
     * @return empty Mono after setting up the redirect
     */
    @GetMapping("/api-docs")
    public Mono<Void> apiDocs(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
        response.getHeaders().setLocation(URI.create("/openapi.json"));
        return Mono.empty();
    }

    /**
     * Redirects /swagger-ui to the Swagger UI HTML page
     * @param response the server HTTP response
     * @return empty Mono after setting up the redirect
     */
    @GetMapping("/swagger-ui")
    public Mono<Void> swaggerUi(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
        response.getHeaders().setLocation(URI.create("/swagger-ui.html"));
        return Mono.empty();
    }
}