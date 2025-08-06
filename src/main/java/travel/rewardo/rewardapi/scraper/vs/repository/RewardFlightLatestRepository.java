package travel.rewardo.rewardapi.scraper.vs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatest;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RewardFlightLatestRepository extends JpaRepository<RewardFlightLatest, String> {
    
    /**
     * Find a reward flight by origin, destination, departure date, and carrier code.
     *
     * @param origin the origin airport code
     * @param destination the destination airport code
     * @param departure the departure date
     * @param carrierCode the carrier code
     * @return an Optional containing the reward flight if found, or empty if not found
     */
    Optional<RewardFlightLatest> findByOriginAndDestinationAndDepartureAndCarrierCode(
            String origin, String destination, LocalDate departure, String carrierCode);

    Optional<Page<RewardFlightLatest>> findByOriginAndDestinationAndCarrierCodeAndDepartureBetween(String origin, String destination, String carrierCode, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("""
        SELECT r FROM RewardFlightLatest r WHERE 
        CASE :cabinType 
            WHEN 'ECONOMY' THEN r.awardEconomy.cabinPointsValue 
            WHEN 'PREMIUM_ECONOMY' THEN r.awardPremiumEconomy.cabinPointsValue 
            WHEN 'BUSINESS' THEN r.awardBusiness.cabinPointsValue 
        END IS NOT NULL AND
        (:cabinType = 'ECONOMY' AND r.awardEconomy.cabinClassSeatCount > 0 OR
         :cabinType = 'PREMIUM_ECONOMY' AND r.awardPremiumEconomy.cabinClassSeatCount > 0 OR
         :cabinType = 'BUSINESS' AND r.awardBusiness.cabinClassSeatCount > 0)
        ORDER BY 
        CASE :cabinType 
            WHEN 'ECONOMY' THEN r.awardEconomy.cabinPointsValue 
            WHEN 'PREMIUM_ECONOMY' THEN r.awardPremiumEconomy.cabinPointsValue 
            WHEN 'BUSINESS' THEN r.awardBusiness.cabinPointsValue 
        END ASC""")
    Optional<Page<RewardFlightLatest>> findAllOrderedByLowestCabinPoints(@Param("cabinType") String cabinType, Pageable pageable);

    @Query("""
        SELECT r FROM RewardFlightLatest r WHERE 
        r.origin = :origin AND r.destination = :destination AND
        CASE :cabinType 
            WHEN 'ECONOMY' THEN r.awardEconomy.cabinPointsValue 
            WHEN 'PREMIUM_ECONOMY' THEN r.awardPremiumEconomy.cabinPointsValue 
            WHEN 'BUSINESS' THEN r.awardBusiness.cabinPointsValue 
        END IS NOT NULL AND
        (:cabinType = 'ECONOMY' AND r.awardEconomy.cabinClassSeatCount > 0 OR
         :cabinType = 'PREMIUM_ECONOMY' AND r.awardPremiumEconomy.cabinClassSeatCount > 0 OR
         :cabinType = 'BUSINESS' AND r.awardBusiness.cabinClassSeatCount > 0)
        ORDER BY 
        CASE :cabinType 
            WHEN 'ECONOMY' THEN r.awardEconomy.cabinPointsValue 
            WHEN 'PREMIUM_ECONOMY' THEN r.awardPremiumEconomy.cabinPointsValue 
            WHEN 'BUSINESS' THEN r.awardBusiness.cabinPointsValue 
        END ASC""")
    Optional<Page<RewardFlightLatest>> findAllOrderedByLowestCabinPointsAndOriginAndDestination(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("cabinType") String cabinType,
            Pageable pageable);
}