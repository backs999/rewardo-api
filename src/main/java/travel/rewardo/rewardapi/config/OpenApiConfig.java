package travel.rewardo.rewardapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Configuration for OpenAPI documentation
 * 
 * This class configures the OpenAPI documentation for the Rewardo API.
 * The API provides endpoints for accessing reward flight information
 * and streaming price change events.
 */
@Configuration
public class OpenApiConfig implements WebFluxConfigurer {

    /**
     * This class provides configuration for OpenAPI documentation.
     * The actual OpenAPI specification is generated automatically by springdoc-openapi
     * based on the controllers and models in the application.
     * 
     * Main API endpoints:
     * - /api/v1/airline/vs/reward-flights/* - Endpoints for retrieving reward flight information
     * - /price-changes/airlines - Streaming endpoint for price change events
     * 
     * Additional configuration is provided in application.yml under the springdoc section.
     */
}