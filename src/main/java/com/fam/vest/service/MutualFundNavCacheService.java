package com.fam.vest.service;

import com.fam.vest.pojo.MutualFundNav;

public interface MutualFundNavCacheService {

    void cacheMutualFundNav(String key, MutualFundNav mutualFundNav);

    MutualFundNav getCachedMutualFundNav(String key);

    void emptyMutualFundNavCache();

    void updateMutualFundNavCache();

    MutualFundNav getMutualFundNav(String isin);
}
