package com.fam.vest.repository;

import com.fam.vest.entity.AccountSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountSnapshotRepository extends JpaRepository<AccountSnapshot, Long> {

    Optional<AccountSnapshot> findAccountSnapshotBySnapshotDate(Date snapshotDate);

    @Query("SELECT a FROM AccountSnapshot a ORDER BY a.snapshotDate DESC")
    List<AccountSnapshot> findLatestSnapshot(Pageable pageable);

    List<AccountSnapshot> findBySnapshotDateGreaterThanEqual(Date snapshotDate);
}
