package travel.rewardo.rewardapi.scraper.vs.model.data.cabintype;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum CabinType {
    ECONOMY("awardEconomy"),
    PREMIUM_ECONOMY("awardPremiumEconomy"),
    BUSINESS("awardBusiness"),
    FIRST("awardFirst");

    @Getter
    private final String fieldName;
}