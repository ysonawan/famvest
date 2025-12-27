package com.fam.vest.repository;

import com.fam.vest.entity.WatchlistInstrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface WatchlistInstrumentRepository extends JpaRepository<WatchlistInstrument, Long> {

    List<WatchlistInstrument> findWatchlistInstrumentByWatchlistIdOrderBySortOrder(Long watchlistId);

    @Modifying
    @Transactional
    void deleteByWatchlistIdAndId(Long watchlistId, Long id);

    @Modifying
    @Transactional
    int deleteAllByWatchlistId(Long watchlistId);

    @Query("SELECT MAX(wi.sortOrder) FROM WatchlistInstrument wi WHERE wi.watchlistId = :watchListId")
    Integer findMaxSortOrderByWatchlistId(@Param("watchListId") Long watchListId);

    @Modifying
    @Transactional
    @Query("UPDATE WatchlistInstrument wi SET wi.sortOrder = :sortOrder WHERE wi.id = :watchlistInstrumentId")
    void updateSortOrderById(@Param("watchlistInstrumentId") Long watchlistInstrumentId,
                             @Param("sortOrder") int sortOrder);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM app_schema.watchlist_instrument wi WHERE wi.trading_symbol IN" +
            " (SELECT i.trading_symbol FROM app_schema.instrument i WHERE wi.trading_symbol=i.trading_symbol AND" +
            " wi.exchange=i.exchange AND expiry IS NOT NULL AND expiry <= (CURRENT_DATE-1));", nativeQuery = true)
    void deleteExpiredWatchlistInstruments();
}
