package travel.rewardo.rewardapi.scraper.vs.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import travel.rewardo.rewardapi.scraper.vs.model.api.AwardCalendar;
import travel.rewardo.rewardapi.scraper.vs.model.api.FlightRequest;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * API client for Virgin Atlantic reward seat checker API.
 * Makes two sequential requests:
 * 1. POST request to the initial URL
 * 2. GET request to the Location header URL from the first response, using cookies from the first response
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VirginAtlanticApiClient {

    private static final String INITIAL_URL = "https://www.virginatlantic.com/travelplus/reward-seat-checker-api/";
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Fetches reward seat information by making two sequential requests.
     *
     * @param requestBody The JSON string request body for the initial POST request
     * @return List of AwardCalendar objects containing the parsed response
     * @throws IOException if an error occurs during the HTTP requests or response parsing
     */
    public List<AwardCalendar> fetchRewardSeatInfo(String requestBody) throws IOException {
        // First request - POST to initial URL
        Request initialRequest = new Request.Builder()
                .url(INITIAL_URL)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                .header("Content-Type", "application/json")
                .header("User-Agent", "PostmanRuntime/7.44.1")
                .header("Accept", "*/*")
                .header("Host", "www.virginatlantic.com")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .build();

        try (Response initialResponse = httpClient.newCall(initialRequest).execute()) {
            // Check if response is successful (2xx) or a redirect (3xx)
            if (!initialResponse.isSuccessful() && (initialResponse.code() < 300 || initialResponse.code() >= 400)) {
                throw new IOException("Unexpected response code: " + initialResponse.code());
            }

            // Extract Location header
            String locationUrl = initialResponse.header("Location");
            if (locationUrl == null) {
                throw new IOException("Location header not found in the response");
            }

            // Extract cookies
            List<String> cookies = initialResponse.headers("Set-Cookie");
            if (cookies.isEmpty()) {
                log.warn("No cookies found in the response");
            }

            // Build cookie string for the next request
            StringBuilder cookieHeader = new StringBuilder();
            for (String cookie : cookies) {
                // Extract just the name=value part of the cookie
                String cookiePart = cookie.split(";")[0];
                if (cookieHeader.length() > 0) {
                    cookieHeader.append("; ");
                }
                cookieHeader.append(cookiePart);
            }

            // Second request - GET to Location URL with cookies
            Request secondRequest = new Request.Builder()
                    .url(locationUrl)
                    .header("User-Agent", "PostmanRuntime/7.44.1")
                    .header("Accept", "*/*")
                    .header("Cookie", cookieHeader.toString())
                    .get()
                    .build();

            try (Response secondResponse = httpClient.newCall(secondRequest).execute()) {
                if (!secondResponse.isSuccessful()) {
                    throw new IOException("Unexpected response code from second request: " + secondResponse.code());
                }

                // Parse response body to POJO
                String responseBody = Objects.requireNonNull(secondResponse.body()).string();
                return objectMapper.readValue(responseBody, objectMapper.getTypeFactory().constructCollectionType(List.class, AwardCalendar.class));
            }
        }
    }

    /**
     * Fetches reward seat information by making two sequential requests.
     *
     * @param flightRequest The request body for the initial POST request
     * @return List of AwardCalendar objects containing the parsed response
     * @throws IOException if an error occurs during the HTTP requests or response parsing
     */
    public List<AwardCalendar> fetchRewardSeatInfo(FlightRequest flightRequest) throws IOException {
        // Convert FlightRequest to JSON string and delegate to the String-based method
        return fetchRewardSeatInfo(objectMapper.writeValueAsString(flightRequest));
    }
}