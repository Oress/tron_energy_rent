package org.ipan.nrgyrent.telegram.handlers.referrals;

import org.ipan.nrgyrent.domain.service.ReferalProgramService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.referral.AddRefProgramState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.ParseUtils;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsActionsView;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsNewView;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class ReferralProgramsNewHandler {
    private final TelegramState telegramState;
    private final ReferalProgramService referalProgramService;
    private final ParseUtils parseUtils;

    private final ReferralProgramsNewView referralProgramsNewView;
    private final ReferralProgramsActionsView referralProgramsActionsView;

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAMS, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ADD)
    public void start_promptLabel(UserState userState, Update update) {
        referralProgramsNewView.updMenuToManageGroupsAddPromptLabel(userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_REF_PROGRAMS_ADD_PROMPT_LABEL));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAMS_ADD_PROMPT_LABEL, updateTypes = UpdateType.MESSAGE)
    public void handleLabel_promptPercentage(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String newLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            if (newLabel.length() < 3) {
                logger.warn("New label is too short: {}", newLabel);
                referralProgramsActionsView.nameIsTooShort(userState);
                return;
            }

            AddRefProgramState addRefProgramState = telegramState.getOrCreateAddRefProgramState(telegramId);
            telegramState.updateAddRefProgramState(telegramId, addRefProgramState.withLabel(newLabel));

            referralProgramsNewView.promptPercentage(userState);
            telegramState.updateUserState(telegramId, userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ADD_PROMPT_PERCENTAGE));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ADD_PROMPT_PERCENTAGE, updateTypes = UpdateType.MESSAGE)
    public void handlePercentage_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String percentageStr = message.getText();
            Long telegramId = userState.getTelegramId();

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

            AddRefProgramState addRefProgramState = telegramState.getOrCreateAddRefProgramState(telegramId);
            referalProgramService.createReferalProgram(addRefProgramState.getLabel(), percentage);

            referralProgramsNewView.refProgramAddSuccess(userState);
            telegramState.updateUserState(telegramId, userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_ADD_SUCCESS));
        }
    }
}