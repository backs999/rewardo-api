package travel.rewardo.rewardapi.scraper.vs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatestHistoric;

@Repository
public interface RewardFlightLatestHistoricRepository extends JpaRepository<RewardFlightLatestHistoric, String> {
    // Basic CRUD operations are provided by JpaRepository
    // Additional custom queries can be added as needed
}