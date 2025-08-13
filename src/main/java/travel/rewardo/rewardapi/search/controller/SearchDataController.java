package travel.rewardo.rewardapi.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import travel.rewardo.rewardapi.routes.service.RoutesService;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatestHistoric;
import travel.rewardo.rewardapi.scraper.vs.repository.RewardFlightLatestHistoricRepository;
import travel.rewardo.rewardapi.scraper.vs.repository.RewardFlightLatestRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/v1/search-data")
@RequiredArgsConstructor
public class SearchDataController {

    private final RoutesService routeService;
    private final RewardFlightLatestHistoricRepository rewardFlightLatestHistoricRepository;
    private final RewardFlightLatestRepository rewardFlightLatestRepository;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Count total routes
        long totalRoutes = routeService.getRoutes().size();
        summary.put("totalRoutes", totalRoutes);

        // Count total scrapes
        long totalScrapes = rewardFlightLatestHistoricRepository.count();
        summary.put("totalScrapes", totalScrapes);

        // Count historic and current flights
        long totalHistoricFlights = rewardFlightLatestHistoricRepository.count();
        summary.put("totalHistoricFlights", totalHistoricFlights);

        long currentFlights = rewardFlightLatestRepository.count();
        summary.put("currentFlights", currentFlights);

        // Add airline information (currently only Virgin Atlantic)
        Map<String, Object> airlineInfo = new HashMap<>();
        airlineInfo.put("code", "VS");
        airlineInfo.put("name", "Virgin Atlantic");
        airlineInfo.put("totalRoutes", totalRoutes);
        airlineInfo.put("totalScrapes", totalScrapes);

        summary.put("airlines", Map.of("VS", airlineInfo));

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/routes/count")
    public ResponseEntity<Map<String, Object>> getRouteCount() {
        Map<String, Object> routeStats = new HashMap<>();

        long totalRoutes = routeService.getRoutes().size();
        routeStats.put("totalRoutes", totalRoutes);

        return ResponseEntity.ok(routeStats);
    }

    @GetMapping("/historic-flights/count")
    public ResponseEntity<Map<String, Object>> getHistoricFlightsCount() {
        Map<String, Object> flightStats = new HashMap<>();

        // Count total historic flights
        long totalHistoricFlights = rewardFlightLatestHistoricRepository.count();
        flightStats.put("totalHistoricFlights", totalHistoricFlights);

        // Count current flights
        long currentFlights = rewardFlightLatestRepository.count();
        flightStats.put("currentFlights", currentFlights);

        return ResponseEntity.ok(flightStats);
    }

    /**
     * Returns routes with the most changes for Virgin Atlantic with pagination support.
     * The response format matches the example in the requirements:
     * 
     * LHR → JFK VS 42 changes
     * LAX → SYD VS 36 changes
     * etc.
     * 
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return List of routes with the most changes
     */
    @GetMapping("/routes/most-changes")
    public ResponseEntity<Map<String, Object>> getRoutesMostChanges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Create pageable object for pagination
        Pageable pageable = PageRequest.of(page, size);
        
        // Get all historic reward flights for Virgin Atlantic
        List<RewardFlightLatestHistoric> historicRewardFlights = 
            StreamSupport.stream(rewardFlightLatestHistoricRepository.findAll().spliterator(), false)
                .toList();
        
        // Count changes by origin-destination pair
        Map<String, Long> changesByRoute = historicRewardFlights.stream()
            .collect(Collectors.groupingBy(
                flight -> flight.getOrigin() + "-" + flight.getDestination(),
                Collectors.counting()
            ));
        
        // Convert to list of route objects for sorting and pagination
        List<Map<String, Object>> routesList = changesByRoute.entrySet().stream()
            .map(entry -> {
                Map<String, Object> route = new HashMap<>();
                String[] parts = entry.getKey().split("-");
                route.put("origin", parts[0]);
                route.put("destination", parts[1]);
                route.put("airline", "VS"); // Virgin Atlantic
                route.put("changes", entry.getValue());
                
                // Add formatted display string matching the example
                route.put("display", String.format("%s\n→\n%s\nVS\n%d changes", 
                    parts[0], parts[1], entry.getValue()));
                
                return route;
            })
            .sorted((r1, r2) -> ((Long)r2.get("changes")).compareTo((Long)r1.get("changes")))
            .collect(Collectors.toList());
        
        // Apply pagination
        int start = (int)pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), routesList.size());
        
        List<Map<String, Object>> pageContent = 
            start < end ? routesList.subList(start, end) : new ArrayList<>();
        
        // Build response
        response.put("routes", pageContent);
        response.put("currentPage", page);
        response.put("totalItems", routesList.size());
        response.put("totalPages", (int) Math.ceil((double) routesList.size() / size));
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Returns the most common origin-destination pairs for the past 30 days
     * This uses a SQL query to efficiently group and count the pairs directly in the database
     * 
     * @param carrierCode Optional carrier code to filter by airline (e.g., "VS" for Virgin Atlantic)
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return List of origin-destination pairs with counts
     */
    @GetMapping("/routes/most-common-pairs")
    public ResponseEntity<Map<String, Object>> getMostCommonOriginDestinationPairs(
            @RequestParam(required = false) String carrierCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Get the most common origin-destination pairs for the past 30 days
        List<Map<String, Object>> pairs;
        // Calculate date 30 days ago from today
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
        
        if (carrierCode != null && !carrierCode.isEmpty()) {
            pairs = rewardFlightLatestHistoricRepository.findMostCommonOriginDestinationPairsByCarrier(carrierCode, thirtyDaysAgo);
        } else {
            pairs = rewardFlightLatestHistoricRepository.findMostCommonOriginDestinationPairs(thirtyDaysAgo);
        }
        
        // Apply pagination
        Pageable pageable = PageRequest.of(page, size);
        int start = (int)pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), pairs.size());
        
        List<Map<String, Object>> pageContent = 
            start < end ? pairs.subList(start, end) : new ArrayList<>();
        
        // Format the response
        List<Map<String, Object>> formattedPairs = pageContent.stream()
            .map(pair -> {
                Map<String, Object> formattedPair = new HashMap<>();
                formattedPair.put("origin", pair.get("origin"));
                formattedPair.put("destination", pair.get("destination"));
                formattedPair.put("count", pair.get("count"));
                
                // Add airline info if provided
                if (carrierCode != null && !carrierCode.isEmpty()) {
                    formattedPair.put("airline", carrierCode);
                }
                
                // Add formatted display string
                String displayFormat = "%s → %s (%d pairs)";
                formattedPair.put("display", String.format(displayFormat, 
                    pair.get("origin"), pair.get("destination"), pair.get("count")));
                
                return formattedPair;
            })
            .collect(Collectors.toList());
        
        // Build response
        response.put("pairs", formattedPairs);
        response.put("currentPage", page);
        response.put("totalItems", pairs.size());
        response.put("totalPages", (int) Math.ceil((double) pairs.size() / size));
        
        return ResponseEntity.ok(response);
    }
}
