package travel.rewardo.rewardapi.scraper.vs.model.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightRequest {
    private Slice slice;
    private List<String> passengers;
    private List<String> permittedCarriers;
    private List<Integer> years;
    private List<String> months;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Slice {
        private String origin;
        private String destination;
        private String departure;
    }
}
