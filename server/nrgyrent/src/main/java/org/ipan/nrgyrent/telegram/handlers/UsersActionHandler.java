package org.ipan.nrgyrent.telegram.handlers;

import java.math.BigDecimal;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserEdit;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.ManageUserActionsView;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class UsersActionHandler {
    private final TelegramState telegramState;
    private final UserService userService;
    private final BalanceService balanceService;
    private final ManageUserActionsView manageUserActionsView;
    private final ManageUsersSearchHandler manageUsersSearchHandler;


    @MatchState(state = States.ADMIN_MANAGE_USERS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_USER_ACTION_DEACTIVATE)
    public void startDeactivateUser(UserState userState, Update update) {
        manageUserActionsView.confirmDeactivateUserMsg(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USER_ACTION_DEACTIVATE_CONFIRM));
    }

    @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_YES)
    public void confirmDeactivateUser(UserState userState, Update update) {
        UserEdit openUser = telegramState.getOrCreateUserEdit(userState.getTelegramId());
        userService.deactivateUser(openUser.getSelectedUserId());
        manageUserActionsView.userDeleted(userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_USER_ACTION_DEACTIVATE_SUCCESS));
    }

    // @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_NO)
    public void declineDeactivateUser(UserState userState, Update update) {
        UserEdit openUser = telegramState.getOrCreateUserEdit(userState.getTelegramId());
        manageUsersSearchHandler.openUser(userState, openUser.getSelectedUserId());
    }

    @MatchState(state = States.ADMIN_MANAGE_USERS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_USER_ACTION_ADJUST_BALANCE_MANUALLY)
    public void startAdjustBalanceManually(UserState userState, Update update) {
        manageUserActionsView.promptNewUserBalance(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USER_ACTION_PROMPT_NEW_BALANCE));
    }

    @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_PROMPT_NEW_BALANCE, updateTypes = UpdateType.MESSAGE)
    public void handleNewBalanceManually(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Adjusting user balance: {}", message.getText());
            String newBalance = message.getText();
            // TODO: catch NumberFormatException
            BigDecimal adjustedBalanceInTrx = new BigDecimal(newBalance);
            BigDecimal adjustedBalanceInSun = adjustedBalanceInTrx.multiply(AppConstants.trxToSunRate);
            Long telegramId = userState.getTelegramId();

            Long adjustedBalanceInSunLong = adjustedBalanceInSun.longValue();
            if (adjustedBalanceInSunLong < 0) {
                logger.warn("Adjusted balance is negative: {}", adjustedBalanceInSunLong);
                manageUserActionsView.groupBalanceIsNegative(userState);
                return;
            }

            UserEdit userEdit = telegramState.getOrCreateUserEdit(telegramId);
            AppUser byId = userService.getById(userEdit.getSelectedUserId());

            balanceService.adjustBalance(byId.getBalance().getId(), adjustedBalanceInSunLong, telegramId);

            manageUserActionsView.userBalanceAdjusted(userState);
        }
    }
}
