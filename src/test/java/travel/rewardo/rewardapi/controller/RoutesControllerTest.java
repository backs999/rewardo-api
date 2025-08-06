package travel.rewardo.rewardapi.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import travel.rewardo.rewardapi.routes.model.Airport;
import travel.rewardo.rewardapi.routes.model.Route;
import travel.rewardo.rewardapi.routes.service.RoutesService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoutesControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoutesService routesService;
    
    @InjectMocks
    private RoutesController routesController;

    @Test
    void getAllRoutes_shouldReturnRoutes() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.standaloneSetup(routesController).build();
        List<Route> mockRoutes = createMockRoutes();
        when(routesService.getRoutes()).thenReturn(mockRoutes);

        // When/Then
        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origin.city").value("London Heathrow"))
                .andExpect(jsonPath("$[0].origin.airportCode").value("LHR"))
                .andExpect(jsonPath("$[0].origin.country").value("United Kingdom"))
                .andExpect(jsonPath("$[0].destinations[0].city").value("New York City"))
                .andExpect(jsonPath("$[0].destinations[0].airportCode").value("JFK"))
                .andExpect(jsonPath("$[0].destinations[0].country").value("United States"));
    }

    @Test
    void refreshRoutes_shouldReturnRefreshedRoutes() throws Exception {
        // Given
        mockMvc = MockMvcBuilders.standaloneSetup(routesController).build();
        List<Route> mockRoutes = createMockRoutes();
        when(routesService.refreshRoutes()).thenReturn(mockRoutes);

        // When/Then
        mockMvc.perform(get("/api/routes/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origin.city").value("London Heathrow"))
                .andExpect(jsonPath("$[0].origin.airportCode").value("LHR"))
                .andExpect(jsonPath("$[0].origin.country").value("United Kingdom"))
                .andExpect(jsonPath("$[0].destinations[0].city").value("New York City"))
                .andExpect(jsonPath("$[0].destinations[0].airportCode").value("JFK"))
                .andExpect(jsonPath("$[0].destinations[0].country").value("United States"));
    }

    private List<Route> createMockRoutes() {
        Airport origin = new Airport("London Heathrow", "LHR", "United Kingdom");
        Airport destination = new Airport("New York City", "JFK", "United States");
        Route route = new Route(origin, Collections.singletonList(destination));
        return Collections.singletonList(route);
    }
}