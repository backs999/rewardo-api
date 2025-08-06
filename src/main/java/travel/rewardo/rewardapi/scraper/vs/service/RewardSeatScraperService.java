package travel.rewardo.rewardapi.scraper.vs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import travel.rewardo.rewardapi.routes.client.RoutesApiClient;
import travel.rewardo.rewardapi.routes.model.Airport;
import travel.rewardo.rewardapi.routes.model.Route;
import travel.rewardo.rewardapi.scraper.vs.client.VirginAtlanticApiClient;
import travel.rewardo.rewardapi.scraper.vs.model.api.AwardCalendar;
import travel.rewardo.rewardapi.scraper.vs.model.api.FlightRequest;
import travel.rewardo.rewardapi.scraper.vs.model.api.PointsDay;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatest;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatestHistoric;
import travel.rewardo.rewardapi.scraper.vs.repository.RewardFlightLatestHistoricRepository;
import travel.rewardo.rewardapi.scraper.vs.repository.RewardFlightLatestRepository;
import travel.rewardo.rewardapi.stream.model.Award;
import travel.rewardo.rewardapi.stream.service.PriceChangeEventService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for scraping reward seat availability from Virgin Atlantic.
 * Runs a scheduled task to fetch reward seats for all routes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardSeatScraperService {

    private final RoutesApiClient routesApiClient;
    private final VirginAtlanticApiClient virginAtlanticApiClient;
    private final RewardFlightLatestRepository rewardFlightLatestRepository;
    private final RewardFlightLatestHistoricRepository rewardFlightLatestHistoricRepository;
    private final PriceChangeEventService priceChangeEventService;
    
    private static final String ADULT_PASSENGER = "ADULT";
    private static final String VS_CARRIER = "VS";
    private static final int FIRST_API_SECOND_API_DELAY_MS = 5000; // 5 seconds
    private static final int ROUTE_PAIR_DELAY_MS = 20000; // 20 seconds
    
    /**
     * Scheduled task that fetches reward seats for all routes.
     * Starts with a 30-second delay and then runs at a fixed interval defined by scraper.vs.refresh-rate property.
     * Fetches data for 12 months from the current day.
     */
    @Scheduled(initialDelay = 30000, fixedDelayString = "${scraper.vs.refresh-rate:3600000}")
    public void fetchRewardSeats() {
        log.info("Starting scheduled task to fetch reward seats");
        
        // Get all routes
        List<Route> routes;
        try {
            routes = routesApiClient.fetchRoutes();
            if (routes.isEmpty()) {
                log.warn("No routes available. Skipping reward seat fetch.");
                return;
            }
        } catch (IOException e) {
            log.error("Failed to fetch routes", e);
            return;
        }
        
        log.info("Fetching reward seats for {} routes", routes.size());
        
        // Get current date in yyyy-MM-dd format
        LocalDate currentDate = LocalDate.now();
        String todayDate = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        
        // Process each route
        for (Route route : routes) {
            Airport origin = route.getOrigin();
            
            for (Airport destination : route.getDestinations()) {
                // Counters for route pair summary
                int priceChangesCount = 0;
                int seatChangesCount = 0;
                int totalProcessedDays = 0;
                
                // Process each month for the next 12 months
                for (int i = 0; i < 12; i++) {
                    LocalDate date = currentDate.plusMonths(i);
                    String month = date.getMonth().toString();
                    int year = date.getYear();
                    
                    try {
                        // Create flight request with single month and year
                        FlightRequest flightRequest = createFlightRequest(
                                origin.getAirportCode(),
                                destination.getAirportCode(),
                                todayDate,
                                year,
                                month
                        );
                        
                        // Fetch reward seats
                        log.info("Fetching reward seats for route: {} to {}, month: {}, year: {}", 
                                origin.getAirportCode(), destination.getAirportCode(), month, year);
                        List<AwardCalendar> awardCalendars = virginAtlanticApiClient.fetchRewardSeatInfo(flightRequest);
                        
                        // Process results
                        log.info("Processing results for {} to {}:", origin.getAirportCode(), destination.getAirportCode());
                        for (AwardCalendar awardCalendar : awardCalendars) {
                            log.debug(awardCalendar.toString());
                            
                            // Process each PointsDay in the AwardCalendar
                            if (awardCalendar.getPointsDays() != null) {
                                for (PointsDay pointsDay : awardCalendar.getPointsDays()) {
                                    boolean[] priceChanged = new boolean[1];
                                    boolean[] seatsChanged = new boolean[1];
                                    processPointsDay(pointsDay, origin.getAirportCode(), destination.getAirportCode(), priceChanged, seatsChanged);
                                    
                                    // Update counters
                                    totalProcessedDays++;
                                    if (priceChanged[0]) {
                                        priceChangesCount++;
                                    }
                                    if (seatsChanged[0]) {
                                        seatChangesCount++;
                                    }
                                }
                            }
                        }
                        
                        // Add delay between first and second API call (handled internally by VirginAtlanticApiClient)
                        Thread.sleep(FIRST_API_SECOND_API_DELAY_MS);
                        
                    } catch (IOException e) {
                        log.error("Error fetching reward seats for route: {} to {}, month: {}, year: {}", 
                                origin.getAirportCode(), destination.getAirportCode(), month, year, e);
                    } catch (InterruptedException e) {
                        log.error("Thread interrupted while waiting between API calls", e);
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                // Log summary for the route pair
                log.info("Route pair summary: {} to {} - Processing complete. Stats: {} days processed, {} price changes, {} seat changes", 
                        origin.getAirportCode(), destination.getAirportCode(), 
                        totalProcessedDays, priceChangesCount, seatChangesCount);
                
                // Add delay between route pairs
                try {
                    log.info("Waiting {} seconds before processing next route pair", ROUTE_PAIR_DELAY_MS / 1000);
                    Thread.sleep(ROUTE_PAIR_DELAY_MS);
                } catch (InterruptedException e) {
                    log.error("Thread interrupted while waiting between route pairs", e);
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        
        log.info("Completed fetching reward seats for all routes");
    }
    
    /**
     * Creates a FlightRequest object with the specified parameters.
     */
    private FlightRequest createFlightRequest(String origin, String destination, String departureDate, 
                                             Integer year, String month) {
        return FlightRequest.builder()
                .slice(FlightRequest.Slice.builder()
                        .origin(origin)
                        .destination(destination)
                        .departure(departureDate)
                        .build())
                .passengers(Collections.singletonList(ADULT_PASSENGER))
                .permittedCarriers(Collections.singletonList(VS_CARRIER))
                .years(Collections.singletonList(year))
                .months(Collections.singletonList(month))
                .build();
    }
    
    /**
     * Checks if the price or seat availability has changed between two RewardFlightLatest objects.
     * 
     * @param existing the existing RewardFlightLatest object from the database
     * @param newEntry the new RewardFlightLatest object created from the API response
     * @param priceChanged output parameter that will be set to true if prices have changed
     * @param seatsChanged output parameter that will be set to true if seat availability has changed
     * @return true if the price or seat availability has changed, false otherwise
     */
    private boolean hasPriceChanged(RewardFlightLatest existing, RewardFlightLatest newEntry, boolean[] priceChanged, boolean[] seatsChanged) {
        // Safely get values, handling potential nulls
        Integer existingEconomyPoints = existing.getAwardEconomy() != null ? existing.getAwardEconomy().getCabinPointsValue() : null;
        Integer newEconomyPoints = newEntry.getAwardEconomy() != null ? newEntry.getAwardEconomy().getCabinPointsValue() : null;
        
        Integer existingPremiumPoints = existing.getAwardPremiumEconomy() != null ? existing.getAwardPremiumEconomy().getCabinPointsValue() : null;
        Integer newPremiumPoints = newEntry.getAwardPremiumEconomy() != null ? newEntry.getAwardPremiumEconomy().getCabinPointsValue() : null;
        
        Integer existingBusinessPoints = existing.getAwardBusiness() != null ? existing.getAwardBusiness().getCabinPointsValue() : null;
        Integer newBusinessPoints = newEntry.getAwardBusiness() != null ? newEntry.getAwardBusiness().getCabinPointsValue() : null;
        
        // Check if points values have changed
        boolean pointsChanged = !Objects.equals(existingEconomyPoints, newEconomyPoints) ||
                               !Objects.equals(existingPremiumPoints, newPremiumPoints) ||
                               !Objects.equals(existingBusinessPoints, newBusinessPoints);
        
        // Safely get seat counts, handling potential nulls
        Integer existingEconomySeats = existing.getAwardEconomy() != null ? existing.getAwardEconomy().getCabinClassSeatCount() : null;
        Integer newEconomySeats = newEntry.getAwardEconomy() != null ? newEntry.getAwardEconomy().getCabinClassSeatCount() : null;
        
        Integer existingPremiumSeats = existing.getAwardPremiumEconomy() != null ? existing.getAwardPremiumEconomy().getCabinClassSeatCount() : null;
        Integer newPremiumSeats = newEntry.getAwardPremiumEconomy() != null ? newEntry.getAwardPremiumEconomy().getCabinClassSeatCount() : null;
        
        Integer existingBusinessSeats = existing.getAwardBusiness() != null ? existing.getAwardBusiness().getCabinClassSeatCount() : null;
        Integer newBusinessSeats = newEntry.getAwardBusiness() != null ? newEntry.getAwardBusiness().getCabinClassSeatCount() : null;
        
        // Check if seat counts have changed
        boolean seatsChangedValue = !Objects.equals(existingEconomySeats, newEconomySeats) ||
                                   !Objects.equals(existingPremiumSeats, newPremiumSeats) ||
                                   !Objects.equals(existingBusinessSeats, newBusinessSeats);
        
        // Set output parameters
        if (priceChanged != null && priceChanged.length > 0) {
            priceChanged[0] = pointsChanged;
        }
        
        if (seatsChanged != null && seatsChanged.length > 0) {
            seatsChanged[0] = seatsChangedValue;
        }
        
        return pointsChanged || seatsChangedValue;
    }
    
    /**
     * Creates a RewardFlightLatest object from a PointsDay object.
     * 
     * @param pointsDay the PointsDay object from the API response
     * @param origin the origin airport code
     * @param destination the destination airport code
     * @return a new RewardFlightLatest object
     */
    private RewardFlightLatest createRewardFlightLatest(PointsDay pointsDay, String origin, String destination) {
        // Create default empty award objects
        travel.rewardo.rewardapi.scraper.vs.model.data.AwardEconomy awardEconomy = 
            travel.rewardo.rewardapi.scraper.vs.model.data.AwardEconomy.builder().build();
        
        travel.rewardo.rewardapi.scraper.vs.model.data.AwardPremiumEconomy awardPremiumEconomy = 
            travel.rewardo.rewardapi.scraper.vs.model.data.AwardPremiumEconomy.builder().build();
        
        travel.rewardo.rewardapi.scraper.vs.model.data.AwardBusiness awardBusiness = 
            travel.rewardo.rewardapi.scraper.vs.model.data.AwardBusiness.builder().build();
            
        // Only populate if seats data is available
        if (pointsDay.getSeats() != null) {
            // Create award economy if available
            if (pointsDay.getSeats().getAwardEconomy() != null) {
                awardEconomy = travel.rewardo.rewardapi.scraper.vs.model.data.AwardEconomy.builder()
                    .cabinPointsValue(pointsDay.getSeats().getAwardEconomy().getCabinPointsValue())
                    .isSaverAward(pointsDay.getSeats().getAwardEconomy().getIsSaverAward())
                    .cabinClassSeatCount(pointsDay.getSeats().getAwardEconomy().getCabinClassSeatCount())
                    .cabinClassSeatCountString(pointsDay.getSeats().getAwardEconomy().getCabinClassSeatCountString())
                    .build();
            }
            
            // Create award premium economy if available
            if (pointsDay.getSeats().getAwardComfortPlusPremiumEconomy() != null) {
                awardPremiumEconomy = travel.rewardo.rewardapi.scraper.vs.model.data.AwardPremiumEconomy.builder()
                    .cabinPointsValue(pointsDay.getSeats().getAwardComfortPlusPremiumEconomy().getCabinPointsValue())
                    .isSaverAward(pointsDay.getSeats().getAwardComfortPlusPremiumEconomy().getIsSaverAward())
                    .cabinClassSeatCount(pointsDay.getSeats().getAwardComfortPlusPremiumEconomy().getCabinClassSeatCount())
                    .cabinClassSeatCountString(pointsDay.getSeats().getAwardComfortPlusPremiumEconomy().getCabinClassSeatCountString())
                    .build();
            }
            
            // Create award business if available
            if (pointsDay.getSeats().getAwardBusiness() != null) {
                awardBusiness = travel.rewardo.rewardapi.scraper.vs.model.data.AwardBusiness.builder()
                    .cabinPointsValue(pointsDay.getSeats().getAwardBusiness().getCabinPointsValue())
                    .isSaverAward(pointsDay.getSeats().getAwardBusiness().getIsSaverAward())
                    .cabinClassSeatCount(pointsDay.getSeats().getAwardBusiness().getCabinClassSeatCount())
                    .cabinClassSeatCountString(pointsDay.getSeats().getAwardBusiness().getCabinClassSeatCountString())
                    .build();
            }
        }
        
        // Create award first (null as it's not in the API response)
        travel.rewardo.rewardapi.scraper.vs.model.data.AwardFirst awardFirst = null;
        
        // Create and return the RewardFlightLatest object
        return RewardFlightLatest.builder()
                .origin(origin)
                .destination(destination)
                .departure(pointsDay.getDateFound())
                .carrierCode(VS_CARRIER)
                .scrapedAt(LocalDateTime.now())
                .awardEconomy(awardEconomy)
                .awardPremiumEconomy(awardPremiumEconomy)
                .awardBusiness(awardBusiness)
                .awardFirst(awardFirst)
                .build();
    }
    
    /**
     * Creates a RewardFlightLatestHistoric object from a RewardFlightLatest object.
     * 
     * @param rewardFlightLatest the RewardFlightLatest object
     * @return a new RewardFlightLatestHistoric object
     */
    private RewardFlightLatestHistoric createRewardFlightLatestHistoric(RewardFlightLatest rewardFlightLatest) {
        // Create default empty award objects
        travel.rewardo.rewardapi.scraper.vs.model.data.AwardEconomy awardEconomy = 
            travel.rewardo.rewardapi.scraper.vs.model.data.AwardEconomy.builder().build();
        
        travel.rewardo.rewardapi.scraper.vs.model.data.AwardPremiumEconomy awardPremiumEconomy = 
            travel.rewardo.rewardapi.scraper.vs.model.data.AwardPremiumEconomy.builder().build();
        
        travel.rewardo.rewardapi.scraper.vs.model.data.AwardBusiness awardBusiness = 
            travel.rewardo.rewardapi.scraper.vs.model.data.AwardBusiness.builder().build();
            
        // Only populate if award data is available
        if (rewardFlightLatest.getAwardEconomy() != null) {
            awardEconomy = travel.rewardo.rewardapi.scraper.vs.model.data.AwardEconomy.builder()
                .cabinPointsValue(rewardFlightLatest.getAwardEconomy().getCabinPointsValue())
                .isSaverAward(rewardFlightLatest.getAwardEconomy().getIsSaverAward())
                .cabinClassSeatCount(rewardFlightLatest.getAwardEconomy().getCabinClassSeatCount())
                .cabinClassSeatCountString(rewardFlightLatest.getAwardEconomy().getCabinClassSeatCountString())
                .build();
        }
        
        if (rewardFlightLatest.getAwardPremiumEconomy() != null) {
            // Create award premium economy
            awardPremiumEconomy = travel.rewardo.rewardapi.scraper.vs.model.data.AwardPremiumEconomy.builder()
                .cabinPointsValue(rewardFlightLatest.getAwardPremiumEconomy().getCabinPointsValue())
                .isSaverAward(rewardFlightLatest.getAwardPremiumEconomy().getIsSaverAward())
                .cabinClassSeatCount(rewardFlightLatest.getAwardPremiumEconomy().getCabinClassSeatCount())
                .cabinClassSeatCountString(rewardFlightLatest.getAwardPremiumEconomy().getCabinClassSeatCountString())
                .build();
        }
        
        if (rewardFlightLatest.getAwardBusiness() != null) {
            // Create award business
            awardBusiness = travel.rewardo.rewardapi.scraper.vs.model.data.AwardBusiness.builder()
                .cabinPointsValue(rewardFlightLatest.getAwardBusiness().getCabinPointsValue())
                .isSaverAward(rewardFlightLatest.getAwardBusiness().getIsSaverAward())
                .cabinClassSeatCount(rewardFlightLatest.getAwardBusiness().getCabinClassSeatCount())
                .cabinClassSeatCountString(rewardFlightLatest.getAwardBusiness().getCabinClassSeatCountString())
                .build();
        }
        
        // Create award first (null if it's not in the original)
        travel.rewardo.rewardapi.scraper.vs.model.data.AwardFirst awardFirst = null;
        if (rewardFlightLatest.getAwardFirst() != null) {
            awardFirst = travel.rewardo.rewardapi.scraper.vs.model.data.AwardFirst.builder()
                .cabinPointsValue(rewardFlightLatest.getAwardFirst().getCabinPointsValue())
                .isSaverAward(rewardFlightLatest.getAwardFirst().getIsSaverAward())
                .cabinClassSeatCount(rewardFlightLatest.getAwardFirst().getCabinClassSeatCount())
                .cabinClassSeatCountString(rewardFlightLatest.getAwardFirst().getCabinClassSeatCountString())
                .build();
        }
        
        // Create and return the RewardFlightLatestHistoric object
        return RewardFlightLatestHistoric.builder()
                .origin(rewardFlightLatest.getOrigin())
                .destination(rewardFlightLatest.getDestination())
                .departure(rewardFlightLatest.getDeparture())
                .carrierCode(rewardFlightLatest.getCarrierCode())
                .scrapedAt(rewardFlightLatest.getScrapedAt())
                .awardEconomy(awardEconomy)
                .awardPremiumEconomy(awardPremiumEconomy)
                .awardBusiness(awardBusiness)
                .awardFirst(awardFirst)
                .build();
    }
    
    /**
     * Processes a PointsDay object by creating RewardFlightLatest and RewardFlightLatestHistoric objects
     * and saving them to the database if needed.
     * 
     * @param pointsDay the PointsDay object from the API response
     * @param origin the origin airport code
     * @param destination the destination airport code
     * @param priceChanged output parameter that will be set to true if prices have changed
     * @param seatsChanged output parameter that will be set to true if seat availability has changed
     */
    private void processPointsDay(PointsDay pointsDay, String origin, String destination, boolean[] priceChanged, boolean[] seatsChanged) {
        log.debug("Processing PointsDay for {} to {} on {}", origin, destination, pointsDay.getDateFound());
        
        // Create a new RewardFlightLatest object from the PointsDay
        RewardFlightLatest newEntry = createRewardFlightLatest(pointsDay, origin, destination);
        
        // Check if an entry already exists in the database
        Optional<RewardFlightLatest> existingOptional = rewardFlightLatestRepository
                .findByOriginAndDestinationAndDepartureAndCarrierCode(
                        origin, destination, pointsDay.getDateFound(), VS_CARRIER);
        
        if (existingOptional.isPresent()) {
            // Entry exists, check if price or seats have changed
            RewardFlightLatest existing = existingOptional.get();
            
            if (hasPriceChanged(existing, newEntry, priceChanged, seatsChanged)) {
                log.info("Price or seat availability changed for {} to {} on {}", 
                        origin, destination, pointsDay.getDateFound());
                
                // Create a historic record of the existing entry before updating it
                RewardFlightLatestHistoric historic = createRewardFlightLatestHistoric(existing);
                rewardFlightLatestHistoricRepository.save(historic);
                
                // Update the ID of the new entry to match the existing one
                newEntry.setId(existing.getId());
                
                // Save the updated entry
                rewardFlightLatestRepository.save(newEntry);
                log.debug("Updated RewardFlightLatest and created historic record");
                
                // Send the old and new price to the PriceChangeEventService
                priceChangeEventService.emitPriceChange(
                    Optional.of(convertToStreamModel(existing)), 
                    Optional.of(convertToStreamModel(newEntry)));
                
                // Return the change information to the caller
                return;
            } else {
                log.debug("No change in price or seat availability, skipping update");
            }
        } else {
            // Entry doesn't exist, save the new entry
            rewardFlightLatestRepository.save(newEntry);
            log.debug("Created new RewardFlightLatest entry");
        }
    }
    
    /**
     * Converts a RewardFlightLatest from the scraper model to the stream model.
     * 
     * @param scraperModel the RewardFlightLatest from the scraper model
     * @return a RewardFlightLatest from the stream model
     */
    private travel.rewardo.rewardapi.stream.model.RewardFlightLatest convertToStreamModel(RewardFlightLatest scraperModel) {
        // Create award objects for each cabin class
        Award awardEconomy = null;
        if (scraperModel.getAwardEconomy() != null) {
            awardEconomy = Award.builder()
                .cabinPointsValue(scraperModel.getAwardEconomy().getCabinPointsValue())
                .isSaverAward(scraperModel.getAwardEconomy().getIsSaverAward())
                .cabinClassSeatCount(scraperModel.getAwardEconomy().getCabinClassSeatCount())
                .cabinClassSeatCountString(scraperModel.getAwardEconomy().getCabinClassSeatCountString())
                .build();
        }
        
        Award awardPremiumEconomy = null;
        if (scraperModel.getAwardPremiumEconomy() != null) {
            awardPremiumEconomy = Award.builder()
                .cabinPointsValue(scraperModel.getAwardPremiumEconomy().getCabinPointsValue())
                .isSaverAward(scraperModel.getAwardPremiumEconomy().getIsSaverAward())
                .cabinClassSeatCount(scraperModel.getAwardPremiumEconomy().getCabinClassSeatCount())
                .cabinClassSeatCountString(scraperModel.getAwardPremiumEconomy().getCabinClassSeatCountString())
                .build();
        }
        
        Award awardBusiness = null;
        if (scraperModel.getAwardBusiness() != null) {
            awardBusiness = Award.builder()
                .cabinPointsValue(scraperModel.getAwardBusiness().getCabinPointsValue())
                .isSaverAward(scraperModel.getAwardBusiness().getIsSaverAward())
                .cabinClassSeatCount(scraperModel.getAwardBusiness().getCabinClassSeatCount())
                .cabinClassSeatCountString(scraperModel.getAwardBusiness().getCabinClassSeatCountString())
                .build();
        }
        
        Award awardFirst = null;
        if (scraperModel.getAwardFirst() != null) {
            awardFirst = Award.builder()
                .cabinPointsValue(scraperModel.getAwardFirst().getCabinPointsValue())
                .isSaverAward(scraperModel.getAwardFirst().getIsSaverAward())
                .cabinClassSeatCount(scraperModel.getAwardFirst().getCabinClassSeatCount())
                .cabinClassSeatCountString(scraperModel.getAwardFirst().getCabinClassSeatCountString())
                .build();
        }
        
        // Create and return the stream model
        return travel.rewardo.rewardapi.stream.model.RewardFlightLatest.builder()
            .id(scraperModel.getId())
            .origin(scraperModel.getOrigin())
            .destination(scraperModel.getDestination())
            .departure(scraperModel.getDeparture())
            .carrierCode(scraperModel.getCarrierCode())
            .scrapedAt(scraperModel.getScrapedAt())
            .awardEconomy(awardEconomy)
            .awardPremiumEconomy(awardPremiumEconomy)
            .awardBusiness(awardBusiness)
            .awardFirst(awardFirst)
            .build();
    }
}