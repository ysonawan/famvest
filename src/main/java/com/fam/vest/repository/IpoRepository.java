package com.fam.vest.repository;

import com.fam.vest.entity.Ipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IpoRepository extends JpaRepository<Ipo, Long> {

    List<Ipo> findAllByOrderByEndDateAsc();

    @Query(value = "SELECT * FROM app_schema.ipo " +
            "WHERE status = 'LIVE' " +
            "AND end_date >= CURRENT_DATE " +
            "AND end_date < CURRENT_DATE + INTERVAL '2 days' " +
            "ORDER BY end_date ASC", nativeQuery = true)
    List<Ipo> findLiveIposClosingInTwoDays();
}
