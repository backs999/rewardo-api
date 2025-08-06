package travel.rewardo.rewardapi.routes.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a route with an origin airport and a list of destination airports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {
    private Airport origin;
    private List<Airport> destinations;
}