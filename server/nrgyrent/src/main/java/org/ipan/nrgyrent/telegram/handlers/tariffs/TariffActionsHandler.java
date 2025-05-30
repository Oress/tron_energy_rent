package org.ipan.nrgyrent.telegram.handlers.tariffs;

import org.ipan.nrgyrent.domain.service.TariffService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.tariff.TariffEdit;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.ParseUtils;
import org.ipan.nrgyrent.telegram.views.tariffs.TariffActionsView;
import org.ipan.nrgyrent.telegram.views.tariffs.TariffNewView;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@AllArgsConstructor
@Slf4j
public class TariffActionsHandler {
    private final TelegramState telegramState;
    private final TariffService tariffService;
    private final TariffsSearchHandler tariffsSearchHandler;
    private final TariffActionsView tariffsActionsView;
    private final TariffNewView tariffNewView;
    private final ParseUtils parseUtils;

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_CHANGE_TX1_AMOUNT)
    public void startChangingTx1Amount_promptTxType1Amount(UserState userState, Update update) {
        tariffNewView.promptTxType1Amount(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_TX1_AMOUNT));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_TX1_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handleTxType1Amount_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String tx1AmountTrx = message.getText();

            Long txType1Amount = null;
            try {
                txType1Amount = parseUtils.parseTrxStrToSunLong(tx1AmountTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", tx1AmountTrx);
                return;
            }

            TariffEdit openTariff = telegramState.getOrCreateTariffEdit(userState.getTelegramId());

            tariffService.changeTxType1Amount(openTariff.getSelectedTariffId(), txType1Amount);

            tariffsActionsView.changeTxAmountSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_CHANGE_AMOUNT_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_CHANGE_TX2_AMOUNT)
    public void startChangingTx2Amount_promptTxType2Amount(UserState userState, Update update) {
        tariffNewView.promptTxType2Amount(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_TX2_AMOUNT));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_TX2_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handleTxType2Amount_end(UserState userState, Update update) {
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

            TariffEdit openTariff = telegramState.getOrCreateTariffEdit(userState.getTelegramId());

            tariffService.changeTxType2Amount(openTariff.getSelectedTariffId(), txType2Amount);

            tariffsActionsView.changeTxAmountSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_CHANGE_AMOUNT_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_RENAME)
    public void startRenaming(UserState userState, Update update) {
        tariffsActionsView.promptNewLabel(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_NEW_LABEL));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_NEW_LABEL, updateTypes = UpdateType.MESSAGE)
    public void handleNewLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Renaming tariff with new label: {}", message.getText());
            String newLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            if (newLabel.length() < 3) {
                logger.warn("New label is too short: {}", newLabel);
                tariffsActionsView.tariffNameIsTooShort(userState);
                return;
            }

            TariffEdit openTariff = telegramState.getOrCreateTariffEdit(telegramId);
            tariffService.renameTariff(openTariff.getSelectedTariffId(), newLabel);

            tariffsActionsView.renameSuccess(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_RENAMED_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_ACTION_DEACTIVATE)
    public void handleDeactivate(UserState userState, Update update) {
        tariffsActionsView.confirmDeactivateMsg(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_DEACTIVATE_CONFIRM));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_YES)
    public void confirmDeactivate(UserState userState, Update update) {
        TariffEdit openTariff = telegramState.getOrCreateTariffEdit(userState.getTelegramId());
        tariffService.deactivateTariff(openTariff.getSelectedTariffId());
        tariffsActionsView.deleteSuccess(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_DEACTIVATE_SUCCESS));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_NO)
    public void declineDeactivate(UserState userState, Update update) {
        TariffEdit openTariff = telegramState.getOrCreateTariffEdit(userState.getTelegramId());
        tariffsSearchHandler.openTariff(userState, openTariff.getSelectedTariffId());
    }
}