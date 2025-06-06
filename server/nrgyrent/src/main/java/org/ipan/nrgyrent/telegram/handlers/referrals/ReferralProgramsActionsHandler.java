package org.ipan.nrgyrent.telegram.handlers.referrals;

import org.ipan.nrgyrent.domain.service.ReferalProgramService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.referral.RefProgramEdit;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsActionsView;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsNewView;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class ReferralProgramsActionsHandler {
    private final TelegramState telegramState;
    private final ReferalProgramService referalProgramService;
    private final ReferralProgramsActionsView referralProgramsActionsView;
    private final ReferralProgramsNewView referralProgramsNewView;

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_PERCENTAGE)
    public void startChangingPercentage_promptPercentage(UserState userState, Update update) {
        referralProgramsNewView.promptPercentage(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PROMPT_TX1_AMOUNT));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PROMPT_TX1_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handlePercentage_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String percentageStr = message.getText();

            Integer percentage = null;
            try {
                percentage = Integer.parseInt(percentageStr);

                if (percentage <= 0 && percentage >= 100) {
                    throw new IllegalArgumentException("Percentage is not in the bounds of (0-100)");
                }
            } catch (Exception e) {
                logger.warn("Invalid percentage amount: {}", percentageStr);
                return;
            }

            RefProgramEdit openRefProgram = telegramState.getOrCreateRefProgramEdit(userState.getTelegramId());

            referalProgramService.changePercentage(openRefProgram.getSelectedRefProgramId(), percentage);

            referralProgramsActionsView.changeTxAmountSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_AMOUNT_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_RENAME)
    public void startRenaming(UserState userState, Update update) {
        referralProgramsActionsView.promptNewLabel(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PROMPT_NEW_LABEL));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PROMPT_NEW_LABEL, updateTypes = UpdateType.MESSAGE)
    public void handleNewLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Renaming ref program with new label: {}", message.getText());
            String newLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            if (newLabel.length() < 3) {
                logger.warn("New label is too short: {}", newLabel);
                referralProgramsActionsView.nameIsTooShort(userState);
                return;
            }

            RefProgramEdit openRefProgram = telegramState.getOrCreateRefProgramEdit(telegramId);
            referalProgramService.renameRefProgram(openRefProgram.getSelectedRefProgramId(), newLabel);

            referralProgramsActionsView.renameSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_RENAMED_SUCCESS));
        }
    }

/*     @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_DEACTIVATE)
    public void handleDeactivate(UserState userState, Update update) {
        tariffsActionsView.confirmDeactivateMsg(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_DEACTIVATE_CONFIRM));
    } 

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_YES)
    public void confirmDeactivate(UserState userState, Update update) {
        RefProgramEdit openTariff = telegramState.getOrCreateRefProgramEdit(userState.getTelegramId());
        tariffService.deactivateTariff(openTariff.getSelectedTariffId());
        tariffsActionsView.deleteSuccess(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_DEACTIVATE_SUCCESS));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_NO)
    public void declineDeactivate(UserState userState, Update update) {
        RefProgramEdit openTariff = telegramState.getOrCreateRefProgramEdit(userState.getTelegramId());
        tariffsSearchHandler.openTariff(userState, openTariff.getSelectedTariffId());
    }
*/
}