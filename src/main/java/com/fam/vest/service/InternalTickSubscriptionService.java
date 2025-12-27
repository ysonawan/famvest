package com.fam.vest.service;

import java.util.Set;

public interface InternalTickSubscriptionService {

    void subscribeToKiteWebsocket(Set<Long> instrumentTokens);
}


