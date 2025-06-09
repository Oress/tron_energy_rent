package org.ipan.nrgyrent.telegram.state;

import java.util.List;
import java.util.Locale;

import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.telegram.States;

public interface UserState {
    Long getTelegramId();
    States getState();
    Long getChatId();
    Integer getMenuMessageId();
    Long getManagingGroupId();
    UserRole getRole();
    List<Integer> getMessagesToDelete();
    String getLanguageCode();

    UserState withTelegramId(Long value);
    UserState withState(States value);
    UserState withChatId(Long value);
    UserState withMenuMessageId(Integer value);
    UserState withRole(UserRole value);
    UserState withMessagesToDelete(List<Integer> value);
    UserState withManagingGroupId(Long value);
    UserState withLanguageCode(String value);

    default boolean isManager() {
        return getManagingGroupId() != null;
    }

    default Locale getLocaleOrDefault() {
        String code = getLanguageCode();
        if (code == null) {
            code = "ru";
        }
        return Locale.of(code);
    }

}
