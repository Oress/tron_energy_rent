package org.ipan.nrgyrent.tron.node.events;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.repository.AutoAmlSessionRepo;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
@Slf4j
public class AutoAmlWatchlist {
    // address -> (userId -> SessionInfo). At most one active session per (address, user).
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, SessionInfo>> sessions = new ConcurrentHashMap<>();
    private final AutoAmlSessionRepo autoAmlSessionRepo;

    @PostConstruct
    public void init() {
        autoAmlSessionRepo.findAllByActive(true).forEach(session -> {
            Long userId = session.getUser().getTelegramId();
            sessions.computeIfAbsent(session.getAddress(), a -> new ConcurrentHashMap<>())
                    .put(userId, new SessionInfo(session.getId(), userId, session.getThresholdSun()));
        });
        logger.info("AutoAmlWatchlist initialized with {} addresses", sessions.size());
    }

    public void addSession(String address, Long sessionId, Long userId, Long thresholdSun) {
        sessions.computeIfAbsent(address, a -> new ConcurrentHashMap<>())
                .put(userId, new SessionInfo(sessionId, userId, thresholdSun));
    }

    public void removeSession(String address, Long userId) {
        sessions.computeIfPresent(address, (a, inner) -> {
            inner.remove(userId);
            return inner.isEmpty() ? null : inner;
        });
    }

    public Collection<SessionInfo> getSessions(String address) {
        if (address == null) {
            return Collections.emptyList();
        }
        ConcurrentHashMap<Long, SessionInfo> inner = sessions.get(address);
        return inner == null ? Collections.emptyList() : inner.values();
    }

    public boolean contains(String address) {
        return address != null && sessions.containsKey(address);
    }

    @Getter
    @AllArgsConstructor
    public static class SessionInfo {
        private final Long sessionId;
        private final Long userId;
        private final Long thresholdSun;
    }
}
