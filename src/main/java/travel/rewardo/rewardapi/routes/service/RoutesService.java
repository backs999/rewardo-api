package travel.rewardo.rewardapi.routes.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import travel.rewardo.rewardapi.routes.client.RoutesApiClient;
import travel.rewardo.rewardapi.routes.model.Route;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for retrieving and caching airline routes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoutesService {

    private final RoutesApiClient routesApiClient;
    private final AtomicReference<List<Route>> cachedRoutes = new AtomicReference<>(Collections.emptyList());

    /**
     * Retrieves the list of routes, using cached data if available.
     *
     * @return List of Route objects
     */
    @Cacheable("routes")
    public List<Route> getRoutes() {
        List<Route> routes = cachedRoutes.get();
        if (routes.isEmpty()) {
            routes = refreshRoutes();
        }
        return routes;
    }

    /**
     * Refreshes the cached routes data by fetching from the API.
     * This method is called automatically on a schedule and can also be called manually.
     *
     * @return List of Route objects
     */
    @Scheduled(fixedDelayString = "${routes.cache.refresh-rate:3600000}", initialDelay = 5000)
    public List<Route> refreshRoutes() {
        try {
            List<Route> routes = routesApiClient.fetchRoutes();
            if (!routes.isEmpty()) {
                cachedRoutes.set(routes);
                log.info("Routes cache refreshed with {} routes", routes.size());
                
                // Log summary of routes data
                int totalDestinations = routes.stream()
                        .mapToInt(route -> route.getDestinations().size())
                        .sum();
                
                log.info("Route summary: {} origin airports with {} total destinations", 
                        routes.size(), totalDestinations);
                
                // Log details for each origin
                routes.forEach(route -> {
                    log.info("Origin: {} ({}, {}) has {} destinations", 
                            route.getOrigin().getAirportCode(),
                            route.getOrigin().getCity(),
                            route.getOrigin().getCountry(),
                            route.getDestinations().size());
                    
                    // Log destination details (limited to avoid excessive logging)
                    if (route.getDestinations().size() <= 10) {
                        route.getDestinations().forEach(dest -> 
                            log.debug("  → Destination: {} ({}, {})", 
                                    dest.getAirportCode(), 
                                    dest.getCity(), 
                                    dest.getCountry())
                        );
                    } else {
                        log.debug("  → First 10 of {} destinations:", route.getDestinations().size());
                        route.getDestinations().stream().limit(10).forEach(dest -> 
                            log.debug("    → {} ({}, {})", 
                                    dest.getAirportCode(), 
                                    dest.getCity(), 
                                    dest.getCountry())
                        );
                    }
                });
            }
            return routes;
        } catch (IOException e) {
            log.error("Failed to refresh routes cache", e);
            return cachedRoutes.get();
        }
    }
}