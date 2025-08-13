package travel.rewardo.rewardapi.scraper.vs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatestHistoric;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RewardFlightLatestHistoricRepository extends JpaRepository<RewardFlightLatestHistoric, String> {

    Optional<Page<RewardFlightLatestHistoric>> findByOriginAndDestinationAndCarrierCodeAndDepartureOrderByScrapedAtAsc(String origin, String destination, String carrierCode, LocalDate startDate, Pageable pageable);

    @Query("SELECT COUNT(r) FROM RewardFlightLatestHistoric r WHERE r.carrierCode = :carrierCode")
    long countByCarrierCode(@Param("carrierCode") String carrierCode);
    
    /**
     * Finds the most common origin-destination pairs for the past 30 days
     * @return List of maps containing origin, destination, and count
     */
    @Query("SELECT r.origin as origin, r.destination as destination, COUNT(r) as count " +
           "FROM RewardFlightLatestHistoric r " +
           "WHERE r.scrapedAt >= :thirtyDaysAgo " +
           "GROUP BY r.origin, r.destination " +
           "ORDER BY COUNT(r) DESC")
    List<Map<String, Object>> findMostCommonOriginDestinationPairs(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
    
    /**
     * Finds the most common origin-destination pairs for a specific carrier for the past 30 days
     * @param carrierCode The airline carrier code
     * @param thirtyDaysAgo The date 30 days ago from today
     * @return List of maps containing origin, destination, and count
     */
    @Query("SELECT r.origin as origin, r.destination as destination, COUNT(r) as count " +
           "FROM RewardFlightLatestHistoric r " +
           "WHERE r.carrierCode = :carrierCode " +
           "AND r.scrapedAt >= :thirtyDaysAgo " +
           "GROUP BY r.origin, r.destination " +
           "ORDER BY COUNT(r) DESC")
    List<Map<String, Object>> findMostCommonOriginDestinationPairsByCarrier(@Param("carrierCode") String carrierCode, @Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
}