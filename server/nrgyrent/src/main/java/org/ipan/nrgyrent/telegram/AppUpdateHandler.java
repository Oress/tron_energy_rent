package org.ipan.nrgyrent.telegram;

import org.ipan.nrgyrent.telegram.state.UserState;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface AppUpdateHandler {
    public void handleUpdate(UserState userState, Update update);
}
