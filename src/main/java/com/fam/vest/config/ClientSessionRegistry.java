package com.fam.vest.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ClientSessionRegistry {

    private final Map<String, Set<Long>> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionMapping = new ConcurrentHashMap<>();

    public void addTokens(String userName, String sessionId, Set<Long> instrumentTokens) {
        sessions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .addAll(instrumentTokens);
        sessionMapping.put(sessionId, userName);
    }

    public void removeTokens(String sessionId, Set<Long> instrumentTokens) {
        if (sessions.containsKey(sessionId)) {
            sessions.get(sessionId).removeAll(instrumentTokens);
            if (sessions.get(sessionId).isEmpty()) {
                sessions.remove(sessionId);
                if(sessionMapping.containsKey(sessionId)) {
                    sessionMapping.remove(sessionId);
                }
            }
        }
    }

    public Set<String> sessionsForToken(Long instrumentToken) {
        return sessions.entrySet().stream()
                .filter(e -> e.getValue().contains(instrumentToken))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Set<Long> tokensExclusivelyForSession(String sessionId, Set<Long> instrumentTokens) {
        return instrumentTokens.stream().filter(token ->
                (this.sessionsForToken(token).size() == 1 && this.sessionsForToken(token).contains(sessionId))
        ).collect(Collectors.toSet());
    }

    public Set<Long> tokensForSessionId(String sessionId) {
        return sessions.get(sessionId);
    }

    public Set<String> getAllUserNames() {
        return sessionMapping.values().stream().collect(Collectors.toSet());
    }

    public Set<String> getAllSessionIds() {
        return sessions.keySet();
    }

    public String userNameForSession(String sessionId) {
        return sessionMapping.get(sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
        sessionMapping.remove(sessionId);
    }
}
