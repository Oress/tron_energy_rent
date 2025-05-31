package org.ipan.nrgyrent.telegram.handlers;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.repository.TariffRepo;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.domain.service.TariffService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserEdit;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.tariff.TariffSearchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.ParseUtils;
import org.ipan.nrgyrent.telegram.views.ManageUserActionsView;
import org.ipan.nrgyrent.telegram.views.tariffs.TariffsSearchView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@Slf4j
public class UsersActionHandler {
    private final int pageSize;
    private final TariffService tariffService;
    private final TariffRepo tariffRepo;
    private final TelegramState telegramState;
    private final UserService userService;
    private final BalanceService balanceService;
    private final ManageUserActionsView manageUserActionsView;
    private final TariffsSearchView tariffsSearchView;
    private final ParseUtils parseUtils;
    
    public UsersActionHandler(@Value("${app.pagination.tariffs.page-size:20}") int pageSize,
            TelegramState telegramState,
            TariffRepo tariffRepo,
            ManageUserActionsView manageUserActionsView,
            BalanceService balanceService,
            UserService userService,
            TariffService tariffService,
            ParseUtils parseUtils,
            TariffsSearchView tariffsSearchView
            ) {
        this.pageSize = pageSize;
        this.telegramState = telegramState;
        this.tariffRepo = tariffRepo;
        this.balanceService = balanceService;
        this.manageUserActionsView = manageUserActionsView;
        this.userService = userService;
        this.tariffService = tariffService;
        this.tariffsSearchView = tariffsSearchView;
        this.parseUtils = parseUtils;
    }

    @MatchState(state = States.ADMIN_MANAGE_USERS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_USER_ACTION_CHANGE_TARIFF)
    public void startChangeTariff(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(0).withQuery(""));

        Page<Tariff> nextPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, "", PageRequest.of(0, pageSize));
        tariffsSearchView.updMenuToTariffSearchResult(nextPage, userState);

        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_USER_ACTION_CHANGE_TARIFF_SEARCHING));
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_CHANGE_TARIFF_SEARCHING, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_NEXT_PAGE)
    })
    public void nextPage(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() + 1;
        String queryStr = searchState.getQuery();
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<Tariff> nextPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, queryStr, PageRequest.of(pageNumber, pageSize));
        tariffsSearchView.updMenuToTariffSearchResult(nextPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_CHANGE_TARIFF_SEARCHING, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_PREV_PAGE)
    })
    public void prevPage(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() - 1;
        String queryStr = searchState.getQuery();
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<Tariff> prevPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, queryStr, PageRequest.of(pageNumber, pageSize));
        tariffsSearchView.updMenuToTariffSearchResult(prevPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_CHANGE_TARIFF_SEARCHING, updateTypes = UpdateType.CALLBACK_QUERY),
    })
    public void openTariffFromSearch(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        if (data.startsWith(TariffsSearchView.OPEN_TARIFF)) {
            String tariffIdStr = data.split(TariffsSearchView.OPEN_TARIFF)[1];
            Long tariffId = Long.parseLong(tariffIdStr);

            UserEdit openUser = telegramState.getOrCreateUserEdit(userState.getTelegramId());
            tariffService.changeIndividualTariff(openUser.getSelectedUserId(), tariffId);
            manageUserActionsView.userTariffChanged(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USER_ACTION_CHANGE_TARIFF_SUCCESS));
        }
    }


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
            String newBalanceTrx = message.getText();

            Long adjustedBalanceInSunLong;
            try {
                adjustedBalanceInSunLong = parseUtils.parseTrxStrToSunLong(newBalanceTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", newBalanceTrx);
                return;
            }

            Long telegramId = userState.getTelegramId();
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
