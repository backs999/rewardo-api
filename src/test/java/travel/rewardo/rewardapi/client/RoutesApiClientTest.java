package travel.rewardo.rewardapi.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.rewardo.rewardapi.routes.client.RoutesApiClient;
import travel.rewardo.rewardapi.routes.model.Route;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutesApiClientTest {

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call call;

    private ObjectMapper objectMapper;
    private RoutesApiClient routesApiClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        routesApiClient = new RoutesApiClient(httpClient, objectMapper);
    }

    @Test
    void fetchRoutes_shouldReturnRoutesList() throws IOException {
        // Given
        String mockResponse = "[{\"origin\":{\"city\":\"London Heathrow\",\"airportCode\":\"LHR\",\"country\":\"United Kingdom\"},\"destinations\":[{\"city\":\"New York City\",\"airportCode\":\"JFK\",\"country\":\"United States\"}]}]";
        
        Response response = new Response.Builder()
                .request(new Request.Builder().url("https://api.rewardo.travel/routes-api/v1/api/airlines/vs/routes").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(mockResponse, okhttp3.MediaType.parse("application/json")))
                .build();

        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // When
        List<Route> routes = routesApiClient.fetchRoutes();

        // Then
        assertNotNull(routes);
        assertEquals(1, routes.size());
        
        Route route = routes.get(0);
        assertEquals("London Heathrow", route.getOrigin().getCity());
        assertEquals("LHR", route.getOrigin().getAirportCode());
        assertEquals("United Kingdom", route.getOrigin().getCountry());
        
        assertEquals(1, route.getDestinations().size());
        assertEquals("New York City", route.getDestinations().get(0).getCity());
        assertEquals("JFK", route.getDestinations().get(0).getAirportCode());
        assertEquals("United States", route.getDestinations().get(0).getCountry());
    }

    @Test
    void fetchRoutes_shouldHandleError() throws IOException {
        // Given
        Response response = new Response.Builder()
                .request(new Request.Builder().url("https://api.rewardo.travel/routes-api/v1/api/airlines/vs/routes").build())
                .protocol(Protocol.HTTP_1_1)
                .code(500)
                .message("Internal Server Error")
                .body(ResponseBody.create("", okhttp3.MediaType.parse("text/plain")))
                .build();

        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // When
        List<Route> routes = routesApiClient.fetchRoutes();

        // Then
        assertNotNull(routes);
        assertTrue(routes.isEmpty());
    }
}