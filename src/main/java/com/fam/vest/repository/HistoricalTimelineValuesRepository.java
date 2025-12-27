package com.fam.vest.repository;

import com.fam.vest.entity.HistoricalTimelineValues;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricalTimelineValuesRepository extends JpaRepository<HistoricalTimelineValues, Long> {

    Optional<HistoricalTimelineValues> findHistoricalTimelineValuesByDate(Date snapshotDate);

    @Query("SELECT h FROM HistoricalTimelineValues h ORDER BY h.date DESC")
    List<HistoricalTimelineValues> findLatestHistoricalTimelineValues(Pageable pageable);

    List<HistoricalTimelineValues> findAllByOrderByDate();

}
