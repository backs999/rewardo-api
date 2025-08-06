package travel.rewardo.rewardapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import travel.rewardo.rewardapi.routes.model.Route;
import travel.rewardo.rewardapi.routes.service.RoutesService;

import java.util.List;

/**
 * REST controller for routes-related endpoints.
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RoutesController {

    private final RoutesService routesService;

    /**
     * Get all available routes.
     *
     * @return List of Route objects
     */
    @GetMapping
    public List<Route> getAllRoutes() {
        return routesService.getRoutes();
    }

    /**
     * Refresh routes data from the API.
     *
     * @return Updated list of Route objects
     */
    @GetMapping("/refresh")
    public List<Route> refreshRoutes() {
        return routesService.refreshRoutes();
    }
}