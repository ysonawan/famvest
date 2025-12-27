package com.fam.vest.repository;

import com.fam.vest.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> getWatchlistByUserIdOrderById(Long userId);

    Watchlist findWatchlistById(Long id);

    Optional<Watchlist> findWatchlistByUserIdAndName(Long userId, String name);

    Watchlist findWatchlistByIdAndUserId(Long id, Long userId);
}
