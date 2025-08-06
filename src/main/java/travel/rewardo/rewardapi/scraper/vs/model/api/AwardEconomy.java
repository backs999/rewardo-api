package travel.rewardo.rewardapi.scraper.vs.model.api;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AwardEconomy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Integer cabinPointsValue;
    private Boolean isSaverAward;
    private Integer cabinClassSeatCount;
    private String cabinClassSeatCountString;
}
