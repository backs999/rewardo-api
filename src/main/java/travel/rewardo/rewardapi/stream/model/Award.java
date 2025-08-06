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
public class Award {
    private String id;
    
    @JsonProperty("cabin_points_value")
    private Integer cabinPointsValue;
    
    @JsonProperty("is_saver_award")
    private Boolean isSaverAward;
    
    @JsonProperty("cabin_class_seat_count")
    private Integer cabinClassSeatCount;
    
    @JsonProperty("cabin_class_seat_count_string")
    private String cabinClassSeatCountString;
}