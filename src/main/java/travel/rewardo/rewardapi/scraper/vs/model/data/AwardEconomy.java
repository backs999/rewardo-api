package travel.rewardo.rewardapi.scraper.vs.model.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "REWARD_FLIGHT_LATEST_AWARD_ECONOMY")
@Table(name = "REWARD_FLIGHT_LATEST_AWARD_ECONOMY")
public class AwardEconomy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Integer cabinPointsValue;
    private Boolean isSaverAward;
    private Integer cabinClassSeatCount;
    private String cabinClassSeatCountString;
}
