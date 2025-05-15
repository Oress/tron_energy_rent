package org.ipan.nrgyrent.telegram.state;

public interface TelegramState {
    UserState getOrCreateUserState(Long userId);
    UserState updateUserState(Long userId, UserState userState);

    AddGroupState getOrCreateAddGroupState(Long userId);
    AddGroupState removeAddGroupState(Long userId);
    AddGroupState updateAddGroupState(Long userId, AddGroupState addGroupState);
}
