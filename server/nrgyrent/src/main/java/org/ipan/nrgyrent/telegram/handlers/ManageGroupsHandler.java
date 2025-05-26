package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.views.ManageGroupNewGroupView;
import org.telegram.telegrambots.meta.api.objects.Update;

import lombok.AllArgsConstructor;

@TransitionHandler
@AllArgsConstructor
public class ManageGroupsHandler {
    private final TelegramState telegramState;

    private final ManageGroupNewGroupView manageGroupNewGroupView;

    @MatchStates({
        @MatchState(state = States.ADMIN_MENU, callbackData = InlineMenuCallbacks.MANAGE_GROUPS),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_SEARCH, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_LABEL, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ADD_PROMPT_MANAGER, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.GO_BACK),
        @MatchState(state = States.ADMIN_MANAGE_GROUPS_ADD_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void showManageGroupsMenu(UserState userState, Update update) {
        manageGroupNewGroupView.updMenuToManageGroupsMenu(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS));
    }
}
