package travel.rewardo.rewardapi.scraper.vs.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatestHistoric;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RewardFlightLatestHistoricRepository extends JpaRepository<RewardFlightLatestHistoric, String> {

    Optional<Page<RewardFlightLatestHistoric>> findByOriginAndDestinationAndCarrierCodeAndDepartureOrderByScrapedAtAsc(String origin, String destination, String carrierCode, LocalDate startDate, Pageable pageable);

    @Query("SELECT COUNT(r) FROM RewardFlightLatestHistoric r WHERE r.carrierCode = :carrierCode")
    long countByCarrierCode(@Param("carrierCode") String carrierCode);
}