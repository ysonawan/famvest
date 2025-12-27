package com.fam.vest.repository;
import com.fam.vest.entity.StraddleStrategyExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
@Repository
public interface StraddleStrategyExecutionRepository extends JpaRepository<StraddleStrategyExecution, Long> {

    @Query("SELECT exec FROM StraddleStrategyExecution exec WHERE exec.strategyId IN (SELECT strat.id FROM StraddleStrategy strat WHERE strat.createdBy = :createdBy) ORDER BY exec.executionDate DESC")
    List<StraddleStrategyExecution> findAllWithStrategyByCreatedBy(@Param("createdBy") String createdBy);

    @Query("SELECT exec FROM StraddleStrategyExecution exec WHERE exec.strategyId = :straddleId AND exec.strategyId IN (SELECT strat.id FROM StraddleStrategy strat WHERE strat.createdBy = :createdBy) ORDER BY exec.executionDate DESC")
    List<StraddleStrategyExecution> findAllWithStrategyByCreatedByAndStrategyId(@Param("createdBy") String createdBy, @Param("straddleId") Long straddleId);
}
