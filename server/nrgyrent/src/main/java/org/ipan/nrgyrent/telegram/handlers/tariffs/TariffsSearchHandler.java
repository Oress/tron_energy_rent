package org.ipan.nrgyrent.telegram.handlers.tariffs;

import java.util.Optional;

import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.Tariff_;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.TariffRepo;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.tariff.TariffEdit;
import org.ipan.nrgyrent.telegram.state.tariff.TariffSearchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.tariffs.TariffActionsView;
import org.ipan.nrgyrent.telegram.views.tariffs.TariffsSearchView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@Slf4j
public class TariffsSearchHandler {
    private final int pageSize;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final TariffRepo tariffRepo;
    private final TariffsSearchView tariffsSearchView;
    private final TariffActionsView tariffActionsView;

    public TariffsSearchHandler(@Value("${app.pagination.tariffs.page-size:20}") int pageSize,
            TelegramState telegramState, TelegramMessages telegramMessages,
            BalanceRepo balanceRepo, TariffRepo tariffRepo,
            TariffsSearchView tariffsSearchView, TariffActionsView tariffActionsView) {
        this.pageSize = pageSize;
        this.telegramState = telegramState;
        this.telegramMessages = telegramMessages;
        this.tariffRepo = tariffRepo;
        this.tariffsSearchView = tariffsSearchView;
        this.tariffActionsView = tariffActionsView;
    }

    @MatchStates({
            @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFFS, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_SEARCH),
            @MatchState(state = States.ADMIN_MANAGE_TARIFFS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_SEARCH_RESET)
    })
    public void resetSearch(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(0).withQuery(""));

        Page<Tariff> nextPage = tariffRepo.findAll(PageRequest.of(0, pageSize).withSort(Sort.by(Sort.Direction.ASC, Tariff_.ID)));
        tariffsSearchView.updMenuToTariffSearchResult(nextPage, userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFFS_SEARCH));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_TARIFFS_SEARCH, updateTypes = UpdateType.MESSAGE)
    public void searchTariffsByLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Searching for tariffs with label: {}", message.getText());
            String queryStr = message.getText();
            telegramMessages.deleteMessage(message);

            if (queryStr.length() < 3) {
                logger.info("Query string is too short: {}", queryStr);
                return;
            }

            TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
            telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withQuery(queryStr));
            Page<Tariff> firstPage = tariffRepo.findByLabelContainingIgnoreCaseOrderById(queryStr, PageRequest.of(0, pageSize));
            tariffsSearchView.updMenuToTariffSearchResult(firstPage, userState);
        }
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_TARIFFS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_NEXT_PAGE)
    })
    public void nextPage(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() + 1;
        String queryStr = searchState.getQuery();
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<Tariff> nextPage = tariffRepo.findByLabelContainingIgnoreCaseOrderById(queryStr, PageRequest.of(pageNumber, pageSize));
        tariffsSearchView.updMenuToTariffSearchResult(nextPage, userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFFS_SEARCH));
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_TARIFFS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_PREV_PAGE)
    })
    public void prevPage(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() - 1;
        String queryStr = searchState.getQuery();
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<Tariff> prevPage = tariffRepo.findByLabelContainingIgnoreCaseOrderById(queryStr, PageRequest.of(pageNumber, pageSize));
        tariffsSearchView.updMenuToTariffSearchResult(prevPage, userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_TARIFFS_SEARCH));
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_TARIFFS_SEARCH, updateTypes = UpdateType.CALLBACK_QUERY),
    })
    public void openTariffFromSearch(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        if (data.startsWith(TariffsSearchView.OPEN_TARIFF)) {
            String tariffIdStr = data.split(TariffsSearchView.OPEN_TARIFF)[1];
            Long tariffId = Long.parseLong(tariffIdStr);
            openTariff(userState, tariffId);
        }
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_NEW_LABEL, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_TARIFF_ACTION_RENAMED_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_TARIFF_ACTION_CHANGE_AMOUNT_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_TARIFF_ACTION_DEACTIVATE_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_TX1_AMOUNT, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_TARIFF_ACTION_PROMPT_TX2_AMOUNT, callbackData = InlineMenuCallbacks.GO_BACK),

    })
    public void openTariffFromBackBtn(UserState userState, Update update) {
        TariffEdit tariffEdit = telegramState.getOrCreateTariffEdit(userState.getTelegramId());

        if (tariffEdit.getSelectedTariffId() != null) {
            Long tariffId = tariffEdit.getSelectedTariffId();
            openTariff(userState, tariffId);
        }
    }

    public void openTariff(UserState userState, Long tariffId) {
        Optional<Tariff> tariff = tariffRepo.findById(tariffId);
        if (tariff.isPresent()) {
            Tariff foundTariff = tariff.get();
            tariffActionsView.updMenuToManageTariffActionsMenu(userState, foundTariff);
            telegramState.updateTariffEdit(userState.getTelegramId(), telegramState
                    .getOrCreateTariffEdit(userState.getTelegramId()).withSelectedTariffId(tariffId));
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_TARIFF_ACTION_PREVIEW));
        } else {
            logger.error("Tariff not found for ID: {}", tariffId);
        }
    }
}
