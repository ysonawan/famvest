package com.fam.vest.repository;

import com.fam.vest.entity.StraddleStrategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StraddleStrategyRepository extends JpaRepository<StraddleStrategy, Long> {

    Optional<StraddleStrategy> findStraddleStrategiesByCreatedByAndId(String username, Long id);

    List<StraddleStrategy> findStraddleStrategiesByCreatedByOrderByCreatedDate(String username);
}
