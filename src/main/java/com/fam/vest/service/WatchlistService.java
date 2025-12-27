package com.fam.vest.service;

import com.fam.vest.entity.ApplicationUser;
import com.fam.vest.entity.Instrument;
import com.fam.vest.entity.Watchlist;
import com.fam.vest.entity.WatchlistInstrument;
import com.fam.vest.dto.request.WatchlistRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WatchlistService {

    List<Watchlist> getWatchlist(UserDetails userDetails);

    Watchlist updateWatchlist(UserDetails userDetails, Long watchListId,
                              WatchlistRequest watchlistRequest);

    @Transactional
    List<Watchlist> deleteWatchlistInstrument(Long watchListId, Long watchlistInstrumentId, UserDetails userDetails);

    List<Watchlist> createWatchlistInstrument(Long watchListId, WatchlistInstrument watchlistInstrument, UserDetails userDetails);

    Watchlist saveWatchlist(String watchListName, ApplicationUser applicationUser);

    WatchlistInstrument saveWatchlistInstrument(WatchlistInstrument watchlistInstrument);

    int deleteAllWatchlistInstrumentByWatchlistId(Watchlist watchlist);

    Watchlist saveWatchlist(Watchlist watchlist);

    Optional<Watchlist> findWatchlistByUserIdAndName(Long userId, String watchlistName);

    List<Instrument> searchInstruments(String search);

    List<Watchlist> updateWatchlistInstrumentOrder(Long watchListId, List<Long> instrumentIds, UserDetails userDetails);

    void reloadWatchlist();
}
