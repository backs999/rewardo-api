package travel.rewardo.rewardapi.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatest;
import travel.rewardo.rewardapi.scraper.vs.model.data.RewardFlightLatestHistoric;
import travel.rewardo.rewardapi.scraper.vs.model.data.cabintype.CabinType;
import travel.rewardo.rewardapi.scraper.vs.repository.RewardFlightLatestHistoricRepository;
import travel.rewardo.rewardapi.scraper.vs.repository.RewardFlightLatestRepository;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/airline/vs/reward-flights")
@RequiredArgsConstructor
public class RewardFlightController {

    private final RewardFlightLatestRepository rewardFlightLatestRepository;
    private final RewardFlightLatestHistoricRepository rewardFlightLatestHistoricRepository;

    @GetMapping("/origin/{origin}/destination/{destination}/from/{from}/to/{to}")
    public ResponseEntity<Page<RewardFlightLatest>> latestRewardFlights(@PathVariable("origin") final String origin, @PathVariable final String destination, @PathVariable final String from, @PathVariable final String to,
                                                                        @RequestParam("page-number") final int pageNumber, @RequestParam("page-size") final int pageSize) {
        Sort sort = Sort.by(Sort.Direction.fromString("ASC"), "departure");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        return ResponseEntity.ok(rewardFlightLatestRepository.findByOriginAndDestinationAndCarrierCodeAndDepartureBetween(origin, destination, "VS", LocalDate.parse(from), LocalDate.parse(to), pageable).orElseThrow());
    }

    @GetMapping("/origin/{origin}/destination/{destination}/cabin/{cabinType}/cheapest")
    public ResponseEntity<Page<RewardFlightLatest>> cheapest(@PathVariable("origin") final String origin, @PathVariable final String destination, @PathVariable("cabinType") final CabinType cabinType, @RequestParam(defaultValue = "0", value = "page-number") final int pageNumber, @RequestParam(value = "page-size", defaultValue = "50") final int pageSize) {
        Sort sort = Sort.by(Sort.Direction.fromString("ASC"), "departure");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        return ResponseEntity.ok(rewardFlightLatestRepository.findAllOrderedByLowestCabinPointsAndOriginAndDestination(origin, destination, cabinType.name(), pageable).orElseThrow());
    }

    @GetMapping("/origin/{origin}/destination/{destination}/on/{on}/historic")
    public ResponseEntity<Page<RewardFlightLatestHistoric>> historicRewardFlight(@PathVariable("origin") final String origin, @PathVariable final String destination, @PathVariable final String on,
                                                                                 @RequestParam("page-number") final int pageNumber, @RequestParam("page-size") final int pageSize) {
        Sort sort = Sort.by(Sort.Direction.fromString("ASC"), "departure");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        return ResponseEntity.ok(rewardFlightLatestHistoricRepository.findByOriginAndDestinationAndCarrierCodeAndDepartureOrderByScrapedAtAsc(origin, destination, "VS", LocalDate.parse(on), pageable).orElseThrow());
    }
}
