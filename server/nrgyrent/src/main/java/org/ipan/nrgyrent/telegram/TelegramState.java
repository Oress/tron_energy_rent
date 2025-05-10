package org.ipan.nrgyrent.telegram;

public interface TelegramState {
    UserState getOrCreateUserState(Long userId);
    UserState updateUserState(Long userId, UserState userState);
}
