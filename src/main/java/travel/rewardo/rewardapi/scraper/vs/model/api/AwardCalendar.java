package travel.rewardo.rewardapi.scraper.vs.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AwardCalendar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @JsonProperty("date")
    private LocalDate dateFound;
    private Double minPrice;
    private String currency;
    private Integer minAwardPointsTotal;
    @OneToOne(cascade = CascadeType.ALL)
    private Seats seats;
    @OneToMany(cascade = CascadeType.ALL)
    private List<PointsDay> pointsDays;
    @JsonProperty("month")
    private String monthFound;
    @JsonProperty("year")
    private String yearFound;
    private Integer totalAwardsSeatsForMonth;
    private String originPrettyName;
    private String destinationPrettyName;
}

