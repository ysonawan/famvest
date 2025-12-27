package com.fam.vest.controller;

import com.fam.vest.entity.Instrument;
import com.fam.vest.entity.Watchlist;
import com.fam.vest.entity.WatchlistInstrument;
import com.fam.vest.enums.REST_RESPONSE_STATUS;
import com.fam.vest.dto.request.WatchlistRequest;
import com.fam.vest.service.WatchlistService;
import com.fam.vest.util.RestResponse;
import com.fam.vest.util.UserDetailsUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rest/v1/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping()
    public ResponseEntity<Object> getWatchlist() {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching watchlist for: {}", userDetails.getUsername());
        List<Watchlist> watchlist = watchlistService.getWatchlist(userDetails);
        RestResponse<List<Watchlist>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), watchlist);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{watchListId}")
    public ResponseEntity<Object> updateWatchlistName(@PathVariable("watchListId") Long watchListId,
                                                      @RequestBody WatchlistRequest watchlistRequest) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Updating watchlist name for: {} to: {} by: {}", watchListId, watchlistRequest, userDetails.getUsername());
        Watchlist updatedWatchlist = watchlistService.updateWatchlist(userDetails, watchListId, watchlistRequest);
        RestResponse<Watchlist> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), updatedWatchlist);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{watchListId}/watchlistInstruments")
    public ResponseEntity<Object> createWatchlistInstrument(@PathVariable("watchListId") Long watchListId,
                                                            @RequestBody WatchlistInstrument watchlistInstrument) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Creating watchlist instrument {} for: {}", watchlistInstrument, userDetails.getUsername());
        List<Watchlist> updatedWatchlist = watchlistService.createWatchlistInstrument(watchListId, watchlistInstrument, userDetails);
        RestResponse<List<Watchlist>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), updatedWatchlist);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping("/{watchListId}/watchlistInstruments/{watchlistInstrumentId}")
    public ResponseEntity<Object> deleteWatchlistInstrument(@PathVariable("watchListId") Long watchListId,
                                                            @PathVariable("watchlistInstrumentId") Long watchlistInstrumentId) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Deleting watchlist instrument {} for: {}", watchlistInstrumentId, userDetails.getUsername());
        List<Watchlist> updatedWatchlist = watchlistService.deleteWatchlistInstrument(watchListId, watchlistInstrumentId, userDetails);
        RestResponse<List<Watchlist>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), updatedWatchlist);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/{watchListId}/reorder")
    public ResponseEntity<Object> reorderWatchlistInstruments(@PathVariable("watchListId") Long watchListId,
                                                              @RequestBody List<Long> watchlistInstrumentIds) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Reordering instruments for watchlist id: {}, instruments: {}, by: {}", watchListId, watchlistInstrumentIds, userDetails.getUsername());
        List<Watchlist> updatedWatchlist = watchlistService.updateWatchlistInstrumentOrder(watchListId, watchlistInstrumentIds, userDetails);
        RestResponse<List<Watchlist>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), updatedWatchlist);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/instruments")
    public ResponseEntity<Object> searchInstruments(@RequestParam(value = "search", required = true) String search) {
        UserDetails userDetails = UserDetailsUtil.getCurrentUserDetails();
        log.info("Fetching instruments for search: {} by: {}", search, userDetails.getUsername());
        List<Instrument> instruments = watchlistService.searchInstruments(search);
        RestResponse<List<Instrument>> response = new RestResponse<>(REST_RESPONSE_STATUS.SUCCESS, null,
                String.valueOf(HttpStatus.OK.value()), instruments);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
