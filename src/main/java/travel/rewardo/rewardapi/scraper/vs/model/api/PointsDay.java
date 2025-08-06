package travel.rewardo.rewardapi.scraper.vs.model.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class PointsDay {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @JsonProperty("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDate dateFound;
    private Double minPrice;
    private String currency;
    private Integer minAwardPointsTotal;
    @OneToOne(cascade = CascadeType.ALL)
    private Seats seats;
}
