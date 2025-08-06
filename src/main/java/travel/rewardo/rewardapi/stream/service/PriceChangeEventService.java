package travel.rewardo.rewardapi.stream.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import travel.rewardo.rewardapi.stream.model.RewardFlightLatest;

import java.util.Optional;

@Service
@Slf4j
public class PriceChangeEventService {

    private final Sinks.Many<Pair<RewardFlightLatest, RewardFlightLatest>> priceModelChange = Sinks.many().replay().limit(10);

    public void emitPriceChange(Optional<RewardFlightLatest> oldPrice, Optional<RewardFlightLatest> updatedPrice) {
        if (oldPrice.isPresent() && updatedPrice.isPresent()) {
            RewardFlightLatest old = oldPrice.get();
            RewardFlightLatest updated = updatedPrice.get();

            log.info("Emitting price change: {}-{}, departure: {}, carrier: {}", 
                old.getOrigin(), old.getDestination(), old.getDeparture(), old.getCarrierCode());

            if (old.getAwardEconomy() != null && updated.getAwardEconomy() != null) {
                log.info("Economy points change: {} -> {}, seats: {} -> {}", 
                    old.getAwardEconomy().getCabinPointsValue(), 
                    updated.getAwardEconomy().getCabinPointsValue(),
                    old.getAwardEconomy().getCabinClassSeatCount(), 
                    updated.getAwardEconomy().getCabinClassSeatCount());
            }

            Sinks.EmitResult result = priceModelChange.tryEmitNext(Pair.of(old, updated));
            if (result.isFailure()) {
                log.warn("Failed to emit price change event: {}", result);
            }
        } else {
            log.warn("Attempted to emit price change with missing data: oldPrice present: {}, updatedPrice present: {}", 
                oldPrice.isPresent(), updatedPrice.isPresent());
        }
    }

    public Flux<Pair<RewardFlightLatest, RewardFlightLatest>> getPriceChangeEvents() {
        log.info("New subscription to price change events");
        return priceModelChange.asFlux()
            .doOnCancel(() -> log.info("Subscription to price change events cancelled"))
            .doOnError(error -> log.error("Error in price change events stream", error));
    }
}