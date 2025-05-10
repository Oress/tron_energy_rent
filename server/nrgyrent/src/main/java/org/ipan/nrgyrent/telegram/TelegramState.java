package org.ipan.nrgyrent.telegram;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@AllArgsConstructor
public class TelegramState {
    private final ConcurrentHashMap<Long, UserState> userStateMap = new ConcurrentHashMap<>();

    public UserState getOrCreateUserState(Long userId) {
        return userStateMap.computeIfAbsent(userId, k -> {
            UserState newUserState = new UserState();
            newUserState.setTelegramId(userId);
            newUserState.setCurrentState(States.START);
            return newUserState;
        });
    }

}
