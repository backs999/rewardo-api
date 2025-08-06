package travel.rewardo.rewardapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.rewardo.rewardapi.routes.client.RoutesApiClient;
import travel.rewardo.rewardapi.routes.model.Airport;
import travel.rewardo.rewardapi.routes.model.Route;
import travel.rewardo.rewardapi.routes.service.RoutesService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoutesServiceTest {

    @Mock
    private RoutesApiClient routesApiClient;

    private RoutesService routesService;

    @BeforeEach
    void setUp() {
        routesService = new RoutesService(routesApiClient);
    }

    @Test
    void getRoutes_shouldReturnCachedRoutes() throws IOException {
        // Given
        List<Route> mockRoutes = createMockRoutes();
        when(routesApiClient.fetchRoutes()).thenReturn(mockRoutes);

        // When
        List<Route> firstCall = routesService.getRoutes();
        List<Route> secondCall = routesService.getRoutes();

        // Then
        assertNotNull(firstCall);
        assertEquals(1, firstCall.size());
        // We can't use assertSame here because the AtomicReference might return a different instance
        assertEquals(firstCall, secondCall); // Should return the same data
        verify(routesApiClient, times(1)).fetchRoutes(); // Should only call the API once
    }

    @Test
    void refreshRoutes_shouldUpdateCache() throws IOException {
        // Given
        List<Route> initialRoutes = createMockRoutes();
        List<Route> updatedRoutes = createUpdatedMockRoutes();
        
        when(routesApiClient.fetchRoutes())
            .thenReturn(initialRoutes)
            .thenReturn(updatedRoutes);

        // When
        List<Route> firstCall = routesService.getRoutes();
        List<Route> afterRefresh = routesService.refreshRoutes();
        List<Route> secondCall = routesService.getRoutes();

        // Then
        assertEquals(1, firstCall.size());
        assertEquals("London Heathrow", firstCall.get(0).getOrigin().getCity());
        
        assertEquals(2, afterRefresh.size());
        assertEquals("London Heathrow", afterRefresh.get(0).getOrigin().getCity());
        assertEquals("Manchester", afterRefresh.get(1).getOrigin().getCity());
        
        assertEquals(afterRefresh, secondCall); // Should return the updated cache
        verify(routesApiClient, times(2)).fetchRoutes();
    }

    @Test
    void refreshRoutes_shouldHandleError() throws IOException {
        // Given
        List<Route> initialRoutes = createMockRoutes();
        when(routesApiClient.fetchRoutes())
            .thenReturn(initialRoutes)
            .thenThrow(new IOException("API error"));

        // When
        List<Route> firstCall = routesService.getRoutes();
        List<Route> afterError = routesService.refreshRoutes();
        List<Route> secondCall = routesService.getRoutes();

        // Then
        assertEquals(1, firstCall.size());
        assertEquals(firstCall, afterError); // Should return the cached routes on error
        assertEquals(firstCall, secondCall); // Should still use the cached routes
        verify(routesApiClient, times(2)).fetchRoutes();
    }

    private List<Route> createMockRoutes() {
        Airport origin = new Airport("London Heathrow", "LHR", "United Kingdom");
        Airport destination = new Airport("New York City", "JFK", "United States");
        Route route = new Route(origin, Collections.singletonList(destination));
        return Collections.singletonList(route);
    }

    private List<Route> createUpdatedMockRoutes() {
        Airport origin1 = new Airport("London Heathrow", "LHR", "United Kingdom");
        Airport destination1 = new Airport("New York City", "JFK", "United States");
        Route route1 = new Route(origin1, Collections.singletonList(destination1));

        Airport origin2 = new Airport("Manchester", "MAN", "United Kingdom");
        Airport destination2 = new Airport("Orlando", "MCO", "United States");
        Route route2 = new Route(origin2, Collections.singletonList(destination2));

        return Arrays.asList(route1, route2);
    }
}