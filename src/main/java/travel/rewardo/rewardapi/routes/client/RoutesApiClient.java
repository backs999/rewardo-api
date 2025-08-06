package travel.rewardo.rewardapi.routes.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import travel.rewardo.rewardapi.routes.model.Route;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * API client for fetching routes from the Rewardo Travel API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoutesApiClient {

    private static final String ROUTES_API_URL = "https://api.rewardo.travel/routes-api/v1/api/airlines/vs/routes";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Fetches the list of routes from the API.
     *
     * @return List of Route objects containing the parsed response
     * @throws IOException if an error occurs during the HTTP request or response parsing
     */
    public List<Route> fetchRoutes() throws IOException {
        log.info("Fetching routes from {}", ROUTES_API_URL);
        
        Request request = new Request.Builder()
                .url(ROUTES_API_URL)
                .get()
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to fetch routes. Response code: {}", response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            String responseBody = Objects.requireNonNull(response.body()).string();
            log.debug("Received routes response: {}", responseBody);
            
            Route[] routes = objectMapper.readValue(responseBody, Route[].class);
            return Arrays.asList(routes);
        } catch (InterruptedIOException e) {
            log.warn("Request interrupted, likely due to application shutdown", e);
            Thread.currentThread().interrupt(); // Preserve interrupt status
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching routes", e);
            return Collections.emptyList();
        }
    }
}