package travel.rewardo.rewardapi.scraper.vs.model.api;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Seats {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @OneToOne(cascade = CascadeType.ALL)
    private AwardEconomy awardEconomy;
    @OneToOne(cascade = CascadeType.ALL)
    private AwardComfortPlusPremiumEconomy awardComfortPlusPremiumEconomy;
    @OneToOne(cascade = CascadeType.ALL)
    private AwardBusiness awardBusiness;
}
