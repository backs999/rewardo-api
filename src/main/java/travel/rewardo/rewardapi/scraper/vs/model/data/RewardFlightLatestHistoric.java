package travel.rewardo.rewardapi.scraper.vs.model.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class RewardFlightLatestHistoric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String origin;
    private String destination;
    private LocalDate departure;
    private String carrierCode;
    private LocalDateTime scrapedAt;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AwardEconomy awardEconomy;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AwardBusiness awardBusiness;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AwardPremiumEconomy awardPremiumEconomy;
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private AwardFirst awardFirst;
}


