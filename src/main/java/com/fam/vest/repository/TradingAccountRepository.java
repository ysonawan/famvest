package com.fam.vest.repository;

import com.fam.vest.entity.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> {

    List<TradingAccount> findAllByOrderByIdAsc();

    @Query("SELECT t FROM TradingAccount t WHERE LOWER(t.userId) = LOWER(:userId)")
    TradingAccount findTradingAccountByUserId(@Param("userId") String userId);

    @Query("SELECT t FROM TradingAccount t WHERE LOWER(t.name) = LOWER(:name)")
    TradingAccount findTradingAccountByName(@Param("name") String name);

    @Modifying
    @Transactional
    @Query("UPDATE TradingAccount t SET t.requestToken = NULL")
    void resetRequestTokens();

    TradingAccount getTradingAccountByUserId(String userId);

}
