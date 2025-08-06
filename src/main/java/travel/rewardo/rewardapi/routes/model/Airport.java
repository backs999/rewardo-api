package travel.rewardo.rewardapi.routes.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an airport with city, airport code, and country information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Airport {
    private String city;
    
    @JsonProperty("airportCode")
    private String airportCode;
    
    private String country;
}