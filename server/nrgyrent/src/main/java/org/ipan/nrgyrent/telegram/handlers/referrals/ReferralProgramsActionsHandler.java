package org.ipan.nrgyrent.telegram.handlers.referrals;

import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.domain.service.ReferalProgramService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.referral.RefProgramEdit;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.ParseUtils;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsActionsView;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsNewView;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
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
    private final ParseUtils parseUtils;

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, updateTypes = UpdateType.CALLBACK_QUERY)
    public void toggleSebes(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        InlineMenuCallbacks.ToggleRefProgramSebesPayload toggleRefProgramSebes = InlineMenuCallbacks.getToggleRefProgramSebes(data);

        if (toggleRefProgramSebes != null) {
            ReferralProgram referralProgram = referalProgramService.toggleSebes(toggleRefProgramSebes.getRefProgramId());
            referralProgramsActionsView.updMenuToManageRefProgramActionsMenu(userState, referralProgram);

        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX1)
    public void startChangingTx1_promptAmount(UserState userState, Update update) {
        referralProgramsNewView.promptBaseAmount(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX1));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX1, updateTypes = UpdateType.MESSAGE)
    public void handleNewTx1_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String tx2AmountTrx = message.getText();

            Long txType1Amount = null;
            try {
                txType1Amount = parseUtils.parseTrxStrToSunLong(tx2AmountTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", tx2AmountTrx);
                return;
            }
            RefProgramEdit openRefProgram = telegramState.getOrCreateRefProgramEdit(userState.getTelegramId());

            referalProgramService.changeTx1BaseAmount(openRefProgram.getSelectedRefProgramId(), txType1Amount);

            referralProgramsNewView.changeTxAmountSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX2)
    public void startChangingT2_promptAmount(UserState userState, Update update) {
        referralProgramsNewView.promptBaseAmount(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX2));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX2, updateTypes = UpdateType.MESSAGE)
    public void handleNewTx2_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String tx2AmountTrx = message.getText();

            Long txType2Amount = null;
            try {
                txType2Amount = parseUtils.parseTrxStrToSunLong(tx2AmountTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", tx2AmountTrx);
                return;
            }
            RefProgramEdit openRefProgram = telegramState.getOrCreateRefProgramEdit(userState.getTelegramId());

            referalProgramService.changeTx2BaseAmount(openRefProgram.getSelectedRefProgramId(), txType2Amount);

            referralProgramsNewView.changeTxAmountSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX1_AUTO)
    public void startChangingTx1Auto_promptAmount(UserState userState, Update update) {
        referralProgramsNewView.promptBaseAmount(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX1_AUTO));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX1_AUTO, updateTypes = UpdateType.MESSAGE)
    public void handleNewTx1Auto_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String tx2AmountTrx = message.getText();

            Long txType1AutoAmount = null;
            try {
                txType1AutoAmount = parseUtils.parseTrxStrToSunLong(tx2AmountTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", tx2AmountTrx);
                return;
            }
            RefProgramEdit openRefProgram = telegramState.getOrCreateRefProgramEdit(userState.getTelegramId());

            referalProgramService.changeTx1AutoBaseAmount(openRefProgram.getSelectedRefProgramId(), txType1AutoAmount);

            referralProgramsNewView.changeTxAmountSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX2_AUTO)
    public void startChangingTx2Auto_promptAmount(UserState userState, Update update) {
        referralProgramsNewView.promptBaseAmount(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX2_AUTO));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX2_AUTO, updateTypes = UpdateType.MESSAGE)
    public void handleNewTx2Auto_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String tx2AmountTrx = message.getText();

            Long txTypewAmount = null;
            try {
                txTypewAmount = parseUtils.parseTrxStrToSunLong(tx2AmountTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", tx2AmountTrx);
                return;
            }
            RefProgramEdit openRefProgram = telegramState.getOrCreateRefProgramEdit(userState.getTelegramId());

            referalProgramService.changeTx2AutoBaseAmount(openRefProgram.getSelectedRefProgramId(), txTypewAmount);

            referralProgramsNewView.changeTxAmountSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_PERCENTAGE)
    public void startChangingPercentage_promptPercentage(UserState userState, Update update) {
        referralProgramsNewView.promptPercentage(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PROMPT_PERCENTAGE));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PROMPT_PERCENTAGE, updateTypes = UpdateType.MESSAGE)
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
}