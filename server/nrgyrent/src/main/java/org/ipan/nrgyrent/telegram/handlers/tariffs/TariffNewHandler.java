package org.ipan.nrgyrent.telegram.handlers.tariffs;

import org.ipan.nrgyrent.domain.service.TariffService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.tariff.AddTariffState;
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
public class TariffNewHandler {
    private final TelegramState telegramState;
    private final TariffService tariffService;
    private final ParseUtils parseUtils;

    private final TariffNewView tariffNewView;
    private final TariffActionsView tariffActionsView;

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFFS, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_ADD)
    public void start_promptLabel(UserState userState, Update update) {
        tariffNewView.updMenuToManageGroupsAddPromptLabel(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFFS_ADD_PROMPT_LABEL));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFFS_ADD_PROMPT_LABEL, updateTypes = UpdateType.MESSAGE)
    public void handleLabel_promptAmountForTxType1(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String newLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            if (newLabel.length() < 3) {
                logger.warn("New label is too short: {}", newLabel);
                tariffActionsView.tariffNameIsTooShort(userState);
                return;
            }

            AddTariffState addTariffState = telegramState.getOrCreateAddTariffState(telegramId);
            telegramState.updateAddTariffState(telegramId, addTariffState.withLabel(newLabel));

            tariffNewView.promptTxType1Amount(userState);
            telegramState.updateUserState(telegramId, userState.withState(States.ADMIN_MANAGE_TARIFF_ADD_PROMPT_TX1_AMOUNT));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ADD_PROMPT_TX1_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handleAmountForTxType1_promptAmountForTxType2(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String tx1AmountTrx = message.getText();
            Long telegramId = userState.getTelegramId();

            Long txType1Amount = null;
            try {
                txType1Amount = parseUtils.parseTrxStrToSunLong(tx1AmountTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", tx1AmountTrx);
                return;
            }

            AddTariffState addTariffState = telegramState.getOrCreateAddTariffState(telegramId);
            telegramState.updateAddTariffState(telegramId, addTariffState.withTxType1Amount(txType1Amount));

            tariffNewView.promptTxType2Amount(userState);
            telegramState.updateUserState(telegramId, userState.withState(States.ADMIN_MANAGE_TARIFF_ADD_PROMPT_TX2_AMOUNT));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFF_ADD_PROMPT_TX2_AMOUNT, updateTypes = UpdateType.MESSAGE)
    public void handleAmountForTxType2_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            String tx2AmountTrx = message.getText();
            Long telegramId = userState.getTelegramId();

            Long txType2Amount = null;
            try {
                txType2Amount = parseUtils.parseTrxStrToSunLong(tx2AmountTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", tx2AmountTrx);
                return;
            }

            AddTariffState addTariffState = telegramState.getOrCreateAddTariffState(telegramId);
            tariffService.createTariff(addTariffState.getLabel(), addTariffState.getTxType1Amount(), txType2Amount);

            tariffNewView.tariffAddSuccess(userState);
            telegramState.updateUserState(telegramId, userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_ADD_SUCCESS));
        }
    }
}