package com.fam.vest.repository;

import com.fam.vest.entity.ApplicationUserTradingAccountId;
import com.fam.vest.entity.ApplicationUserTradingAccountMapping;
import com.fam.vest.entity.TradingAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationUserTradingAccountMappingRepository extends JpaRepository<ApplicationUserTradingAccountMapping, ApplicationUserTradingAccountId> {

    @Query("SELECT mapping.tradingAccount FROM ApplicationUserTradingAccountMapping mapping WHERE mapping.applicationUser.id = :applicationUserId ORDER BY mapping.tradingAccount.id")
    List<TradingAccount> findTradingAccountsByApplicationUserId(@Param("applicationUserId") Long applicationUserId);

    @Query("SELECT mapping.tradingAccount FROM ApplicationUserTradingAccountMapping mapping WHERE mapping.applicationUser.id = :applicationUserId AND mapping.tradingAccount.id = :tradingAccountId")
    Optional<TradingAccount> findTradingAccountByApplicationUserIdAndTradingAccountId(@Param("applicationUserId") Long applicationUserId, @Param("tradingAccountId") Long tradingAccountId);
}
