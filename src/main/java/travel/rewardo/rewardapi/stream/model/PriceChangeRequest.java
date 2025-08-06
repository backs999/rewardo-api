package travel.rewardo.rewardapi.stream.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceChangeRequest {
    @JsonProperty("old_flight")
    private RewardFlightLatest oldFlight;
    
    @JsonProperty("new_flight")
    private RewardFlightLatest newFlight;
}