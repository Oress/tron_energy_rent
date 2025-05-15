package org.ipan.nrgyrent.telegram.state;

import java.util.List;

import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.telegram.States;

public interface UserState {
    Long getTelegramId();
    States getState();
    Long getChatId();
    Integer getMenuMessageId();
    UserRole getRole();
    List<Integer> getMessagesToDelete();

    UserState withTelegramId(Long value);
    UserState withState(States value);
    UserState withChatId(Long value);
    UserState withMenuMessageId(Integer value);
    UserState withRole(UserRole value);
    UserState withMessagesToDelete(List<Integer> value);
}
