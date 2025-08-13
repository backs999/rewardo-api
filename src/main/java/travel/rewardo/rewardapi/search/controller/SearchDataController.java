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
     * Returns the most common origin-destination pairs for the past 30 days
     * This uses a SQL query to efficiently group and count the pairs directly in the database
     * 
     * @param carrierCode Optional carrier code to filter by airline (e.g., "VS" for Virgin Atlantic)
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return List of origin-destination pairs with counts
     */
    @GetMapping("/routes/most-changes")
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
                String displayFormat = "%s → %s (%d changes)";
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
