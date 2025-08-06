package travel.rewardo.rewardapi.stream.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class RewardFlightLatest {
    private String id;
    private String origin;
    private String destination;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate departure;
    
    @JsonProperty("carrier_code")
    private String carrierCode;
    
    @JsonProperty("scraped_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime scrapedAt;
    
    @JsonProperty("award_economy")
    private Award awardEconomy;
    
    @JsonProperty("award_business")
    private Award awardBusiness;
    
    @JsonProperty("award_premium_economy")
    private Award awardPremiumEconomy;
    
    @JsonProperty("award_first")
    private Award awardFirst;
}
