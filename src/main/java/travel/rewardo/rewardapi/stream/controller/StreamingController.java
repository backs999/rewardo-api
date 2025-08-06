package travel.rewardo.rewardapi.stream.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import travel.rewardo.rewardapi.stream.model.RewardFlightLatest;
import travel.rewardo.rewardapi.stream.service.PriceChangeEventService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StreamingController {
    private final PriceChangeEventService priceChangeEventService;

    @GetMapping(value = "/price-changes/airlines", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Pair<RewardFlightLatest, RewardFlightLatest>>> streamEvents() {
        log.info("SSE connection established for price-changes stream");
        return priceChangeEventService.getPriceChangeEvents()
                .doOnNext(event -> log.info("Emitting price change event: {} -> {}, {} -> {}, {}",
                        event.getFirst().getOrigin() + "-" + event.getFirst().getDestination(),
                        event.getFirst().getAwardEconomy() != null ? event.getFirst().getAwardEconomy().getCabinPointsValue() : "N/A",
                        event.getSecond().getOrigin() + "-" + event.getSecond().getDestination(),
                        event.getSecond().getAwardEconomy() != null ? event.getSecond().getAwardEconomy().getCabinPointsValue() : "N/A", event))
                .doOnCancel(() -> log.info("SSE connection cancelled for price-changes stream"))
                .doOnError(error -> log.error("Error in price-changes stream", error))
                .map(event -> ServerSentEvent.builder(event)
                        .event("price-change")
                        .build());
    }
}