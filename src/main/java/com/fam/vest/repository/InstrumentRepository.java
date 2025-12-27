package com.fam.vest.repository;

import com.fam.vest.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    @Query(value = "SELECT * FROM app_schema.instrument " +
            "WHERE to_tsvector('english', display_name) @@ to_tsquery('english', :search) " +
            "ORDER BY " +
            "  CASE segment " +
            "    WHEN 'INDICES' THEN 1 " +
            "    WHEN 'NSE' THEN 2 " +
            "    WHEN 'BSE' THEN 3 " +
            "    WHEN 'NFO-OPT' THEN 4 " +
            "    WHEN 'NFO-FUT' THEN 5 " +
            "    WHEN 'BFO-OPT' THEN 6 " +
            "    WHEN 'BFO-FUT' THEN 7 " +
            "    ELSE 8 " +
            "  END, " +
            "  expiry ASC " +
            "LIMIT :limit", nativeQuery = true)
    List<Instrument> searchInstrumentsWithFullText(@Param("search") String search, @Param("limit") int limit);

    Optional<Instrument> findByTradingSymbolAndExchange(String symbol, String exchange);

    @Query(value = "SELECT * " +
            "FROM app_schema.instrument " +
            "WHERE expiry = ( " +
            "    SELECT MIN(expiry) " +
            "    FROM app_schema.instrument " +
            "    WHERE expiry >= CURRENT_DATE " +
            "      AND segment = :segment " +
            "      AND name = :index " +
            ")" +
            "AND segment = :segment " +
            "AND name = :index  ORDER BY instrument_type DESC, strike", nativeQuery = true)
    List<Instrument> findCurrentWeekOptionExpiryInstruments(@Param("segment") String segment, @Param("index") String index);

    @Query(value = "SELECT * " +
            "FROM app_schema.instrument " +
            "WHERE expiry = ( " +
            "    SELECT expiry " +
            "    FROM ( " +
            "        SELECT DISTINCT expiry " +
            "        FROM app_schema.instrument " +
            "        WHERE expiry >= CURRENT_DATE " +
            "          AND segment = :segment " +
            "          AND name = :index " +
            "        ORDER BY expiry ASC " +
            "        LIMIT 2 " +
            "    ) AS nearest_expiries " +
            "    ORDER BY expiry DESC " +
            "    LIMIT 1 " +
            ") " +
            "AND segment = :segment " +
            "AND name = :index ORDER BY instrument_type DESC, strike", nativeQuery = true)
    List<Instrument> findNextWeekOptionExpiryInstruments(@Param("segment") String segment, @Param("index") String index);

    Optional<Instrument> findByInstrumentToken(Long instrumentToken);

    List<Instrument> findByStrike(String strike);

    Optional<Instrument> findByTradingSymbol(String tradingSymbol);
}
