package com.fam.vest.dto.request;

import java.util.Set;

public record SubscriptionRequest(Set<Long> instrumentTokens) {}
