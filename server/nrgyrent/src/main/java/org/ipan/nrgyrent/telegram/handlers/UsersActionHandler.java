package org.ipan.nrgyrent.telegram.handlers;

import java.util.List;
import java.util.Optional;

import org.ipan.nrgyrent.application.service.EnergyService;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.Tariff_;
import org.ipan.nrgyrent.domain.model.repository.ReferralProgramRepo;
import org.ipan.nrgyrent.domain.model.repository.TariffRepo;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.domain.service.ReferalProgramService;
import org.ipan.nrgyrent.domain.service.TariffService;
import org.ipan.nrgyrent.domain.service.UserService;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserEdit;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.referral.RefProgramSearchState;
import org.ipan.nrgyrent.telegram.state.tariff.TariffSearchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.utils.ParseUtils;
import org.ipan.nrgyrent.telegram.views.ManageUserActionsView;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsSearchView;
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
public class UsersActionHandler {
    private final int pageSize;
    private final TariffService tariffService;
    private final ReferalProgramService referalProgramService;
    private final ReferralProgramRepo referralProgramRepo;
    private final TariffRepo tariffRepo;
    private final TelegramState telegramState;
    private final UserService userService;
    private final BalanceService balanceService;
    private final ManageUserActionsView manageUserActionsView;
    private final TariffsSearchView tariffsSearchView;
    private final ReferralProgramsSearchView referralProgramsSearchView;
    private final ParseUtils parseUtils;
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;
    private final EnergyService energyService;
    
    public UsersActionHandler(@Value("${app.pagination.tariffs.page-size:20}") int pageSize,
            TelegramState telegramState,
            TariffRepo tariffRepo,
            ReferralProgramRepo referralProgramRepo,
            ManageUserActionsView manageUserActionsView,
            BalanceService balanceService,
            UserService userService,
            TariffService tariffService,
            ParseUtils parseUtils,
            TariffsSearchView tariffsSearchView,
            ReferralProgramsSearchView referralProgramsSearchView,
            ReferalProgramService referalProgramService,
            AutoDelegationSessionRepo autoDelegationSessionRepo,
            EnergyService energyService
            ) {
        this.pageSize = pageSize;
        this.telegramState = telegramState;
        this.tariffRepo = tariffRepo;
        this.referralProgramRepo = referralProgramRepo;
        this.balanceService = balanceService;
        this.manageUserActionsView = manageUserActionsView;
        this.userService = userService;
        this.tariffService = tariffService;
        this.tariffsSearchView = tariffsSearchView;
        this.referralProgramsSearchView = referralProgramsSearchView;
        this.parseUtils = parseUtils;
        this.referalProgramService = referalProgramService;
        this.autoDelegationSessionRepo = autoDelegationSessionRepo;
        this.energyService = energyService;
    }

    @MatchState(state = States.ADMIN_MANAGE_USERS_ACTION_PREVIEW,
            callbackData = InlineMenuCallbacks.MANAGE_USER_AUTODELEGATION)
    public void openUserAutoDelegation(UserState userState, Update update) {
        renderUserAutoDelegation(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION));
    }

    @MatchState(state = States.ADMIN_MANAGE_USER_AUTODELEGATION, updateTypes = UpdateType.CALLBACK_QUERY)
    public void handleUserAutoDelegation(UserState userState, Update update) {
        String data = update.getCallbackQuery().getData();
        Long selectedUserId = selectedUserId(userState);

        EnergyProviderName preference = getAutoDelegationPreference(data);
        if (preference != null || InlineMenuCallbacks.MANAGE_USER_AUTODELEGATION_PROVIDER_DEFAULT.equals(data)) {
            userService.setAutoDelegationProvider(selectedUserId, preference);
            renderUserAutoDelegation(userState);
            return;
        }

        try {
            Long sessionId = InlineMenuCallbacks.getUserAutoSessionId(data);
            if (sessionId == null) {
                return;
            }
            findActiveUserSession(sessionId, selectedUserId).ifPresent(session -> {
                manageUserActionsView.updMenuToUserAutoDelegationSession(userState, session);
                telegramState.updateUserState(userState.getTelegramId(),
                        userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION_SESSION));
            });
        } catch (Exception e) {
            logger.error("Could not open auto-delegation session {} for user {}", data, selectedUserId, e);
            manageUserActionsView.userAutoDelegationActionFailed(userState);
        }
    }

    @MatchState(state = States.ADMIN_MANAGE_USER_AUTODELEGATION_SESSION, updateTypes = UpdateType.CALLBACK_QUERY)
    public void handleUserAutoDelegationSession(UserState userState, Update update) {
        String data = update.getCallbackQuery().getData();
        Long selectedUserId = selectedUserId(userState);

        if (InlineMenuCallbacks.GO_BACK.equals(data)) {
            renderUserAutoDelegation(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION));
            return;
        }

        try {
            Long switchSessionId = InlineMenuCallbacks.getUserAutoSwitchSessionId(data);
            if (switchSessionId != null) {
                EnergyProviderName targetProvider = InlineMenuCallbacks.getUserAutoSwitchProvider(data);
                Optional<AutoDelegationSession> session = findActiveUserSession(switchSessionId, selectedUserId);
                if (session.isEmpty() || !isValidSwitchTarget(session.get(), targetProvider)) {
                    renderUserAutoDelegationAndSetState(userState);
                    return;
                }
                if (manageUserActionsView.confirmUserAutoDelegationSwitch(userState, session.get(), targetProvider)) {
                    telegramState.updateUserState(userState.getTelegramId(),
                            userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION_SWITCH_CONFIRM));
                }
                return;
            }

            Long deactivateSessionId = InlineMenuCallbacks.getUserAutoDeactivateSessionId(data);
            if (deactivateSessionId != null) {
                Optional<AutoDelegationSession> session = findActiveUserSession(deactivateSessionId, selectedUserId);
                if (session.isEmpty()) {
                    renderUserAutoDelegationAndSetState(userState);
                    return;
                }
                if (manageUserActionsView.confirmUserAutoDelegationDeactivate(userState, session.get())) {
                    telegramState.updateUserState(userState.getTelegramId(),
                            userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION_DEACTIVATE_CONFIRM));
                }
            }
        } catch (Exception e) {
            logger.error("Could not prepare auto-delegation action {} for user {}", data, selectedUserId, e);
            manageUserActionsView.userAutoDelegationActionFailed(userState);
        }
    }

    @MatchState(state = States.ADMIN_MANAGE_USER_AUTODELEGATION_SWITCH_CONFIRM,
            updateTypes = UpdateType.CALLBACK_QUERY)
    public void confirmUserAutoDelegationSwitch(UserState userState, Update update) {
        String data = update.getCallbackQuery().getData();
        Long selectedUserId = selectedUserId(userState);
        Long sessionId = null;

        try {
            Long backSessionId = InlineMenuCallbacks.getUserAutoSessionId(data);
            if (backSessionId != null) {
                if (!showUserAutoDelegationSession(userState, selectedUserId, backSessionId)) {
                    renderUserAutoDelegationAndSetState(userState);
                }
                return;
            }

            sessionId = InlineMenuCallbacks.getUserAutoSwitchConfirmSessionId(data);
            EnergyProviderName targetProvider = InlineMenuCallbacks.getUserAutoSwitchConfirmProvider(data);
            if (sessionId == null || !findActiveUserSession(sessionId, selectedUserId)
                    .map(session -> isValidSwitchTarget(session, targetProvider)).orElse(false)) {
                renderUserAutoDelegationAndSetState(userState);
                return;
            }

            AutoDelegationSession session = energyService.switchSessionProvider(sessionId, selectedUserId, targetProvider);
            manageUserActionsView.updMenuToUserAutoDelegationSession(userState, session);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION_SESSION));
        } catch (Exception e) {
            logger.error("Could not switch auto-delegation session {} for user {}", sessionId, selectedUserId, e);
            manageUserActionsView.userAutoDelegationActionFailed(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION_SESSION));
        }
    }

    @MatchState(state = States.ADMIN_MANAGE_USER_AUTODELEGATION_DEACTIVATE_CONFIRM,
            updateTypes = UpdateType.CALLBACK_QUERY)
    public void confirmUserAutoDelegationDeactivate(UserState userState, Update update) {
        String data = update.getCallbackQuery().getData();
        Long selectedUserId = selectedUserId(userState);
        Long sessionId = null;

        try {
            Long backSessionId = InlineMenuCallbacks.getUserAutoSessionId(data);
            if (backSessionId != null) {
                if (!showUserAutoDelegationSession(userState, selectedUserId, backSessionId)) {
                    renderUserAutoDelegationAndSetState(userState);
                }
                return;
            }

            sessionId = InlineMenuCallbacks.getUserAutoDeactivateConfirmSessionId(data);
            if (sessionId == null || findActiveUserSession(sessionId, selectedUserId).isEmpty()) {
                renderUserAutoDelegationAndSetState(userState);
                return;
            }

            energyService.deactivateSessionByAdmin(sessionId, selectedUserId);
            renderUserAutoDelegation(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION));
        } catch (Exception e) {
            logger.error("Could not deactivate auto-delegation session {} for user {}", sessionId, selectedUserId, e);
            manageUserActionsView.userAutoDelegationActionFailed(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION_SESSION));
        }
    }

    private Long selectedUserId(UserState userState) {
        return telegramState
                .getOrCreateUserEdit(userState.getTelegramId())
                .getSelectedUserId();
    }

    private EnergyProviderName getAutoDelegationPreference(String data) {
        if (InlineMenuCallbacks.MANAGE_USER_AUTODELEGATION_PROVIDER_ITRX.equals(data)) {
            return EnergyProviderName.ITRX;
        }
        if (InlineMenuCallbacks.MANAGE_USER_AUTODELEGATION_PROVIDER_TRXX.equals(data)) {
            return EnergyProviderName.TRXX;
        }
        return null;
    }

    private Optional<AutoDelegationSession> findActiveUserSession(Long sessionId, Long selectedUserId) {
        return autoDelegationSessionRepo.findByIdAndUserTelegramIdAndActive(sessionId, selectedUserId, true);
    }

    private boolean isValidSwitchTarget(AutoDelegationSession session, EnergyProviderName targetProvider) {
        return (targetProvider == EnergyProviderName.ITRX || targetProvider == EnergyProviderName.TRXX)
                && targetProvider != session.getEnergyProvider();
    }

    private boolean showUserAutoDelegationSession(UserState userState, Long selectedUserId, Long sessionId) {
        Optional<AutoDelegationSession> session = findActiveUserSession(sessionId, selectedUserId);
        if (session.isEmpty()) {
            return false;
        }
        manageUserActionsView.updMenuToUserAutoDelegationSession(userState, session.get());
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION_SESSION));
        return true;
    }

    private void renderUserAutoDelegation(UserState userState) {
        Long selectedUserId = selectedUserId(userState);
        AppUser user = userService.getById(selectedUserId);
        List<AutoDelegationSession> activeSessions = autoDelegationSessionRepo
                .findByUserTelegramIdAndActiveOrderByCreatedAtAsc(selectedUserId, true);
        manageUserActionsView.updMenuToUserAutoDelegation(userState, user, activeSessions);
    }

    private void renderUserAutoDelegationAndSetState(UserState userState) {
        renderUserAutoDelegation(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USER_AUTODELEGATION));
    }

    @MatchState(state = States.ADMIN_MANAGE_USERS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_USER_ACTION_CHANGE_REF_PROGRAM)
    public void startChangeRefProgram(UserState userState, Update update) {
        RefProgramSearchState searchState = telegramState.getOrCreateRefProgramSearchState(userState.getTelegramId());
        telegramState.updateRefProgramSearchState(userState.getTelegramId(), searchState.withCurrentPage(0).withQuery(""));

        Page<ReferralProgram> nextPage = referralProgramRepo.findByLabelContainingIgnoreCaseOrderById("", PageRequest.of(0, pageSize));
        referralProgramsSearchView.updMenuToSearchResult(nextPage, userState);

        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_USER_ACTION_CHANGE_REF_PROGRAM_SEARCHING));
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_CHANGE_REF_PROGRAM_SEARCHING, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_NEXT_PAGE)
    })
    public void nextPageRefProgram(UserState userState, Update update) {
        RefProgramSearchState searchState = telegramState.getOrCreateRefProgramSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() + 1;
        String queryStr = searchState.getQuery();
        telegramState.updateRefProgramSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<ReferralProgram> nextPage = referralProgramRepo.findByLabelContainingIgnoreCaseOrderById(queryStr, PageRequest.of(pageNumber, pageSize));
        referralProgramsSearchView.updMenuToSearchResult(nextPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_CHANGE_REF_PROGRAM_SEARCHING, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_PREV_PAGE)
    })
    public void prevPageRefProgram(UserState userState, Update update) {
        RefProgramSearchState searchState = telegramState.getOrCreateRefProgramSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() - 1;
        String queryStr = searchState.getQuery();
        telegramState.updateRefProgramSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<ReferralProgram> prevPage = referralProgramRepo.findByLabelContainingIgnoreCaseOrderById(queryStr, PageRequest.of(pageNumber, pageSize));
        referralProgramsSearchView.updMenuToSearchResult(prevPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_CHANGE_REF_PROGRAM_SEARCHING, updateTypes = UpdateType.CALLBACK_QUERY),
    })
    public void openRefProgramFromSearch(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        try {
            if (data.startsWith(ReferralProgramsSearchView.OPEN_REF_PROGRAM)) {
                String refProgramIdStr = data.split(ReferralProgramsSearchView.OPEN_REF_PROGRAM)[1];
                Long refProgramId = Long.parseLong(refProgramIdStr);

                UserEdit openUser = telegramState.getOrCreateUserEdit(userState.getTelegramId());
                referalProgramService.createReferalProgramForUser(openUser.getSelectedUserId(), refProgramId);
                manageUserActionsView.userRefProgramChanged(userState);
                telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_USER_ACTION_CHANGE_REF_PROGRAM_SUCCESS));
            }
        } catch (Exception e) {
            logger.error("Cannot execute openRefProgramFromSearch", e);
        }
    }

    @MatchState(state = States.ADMIN_MANAGE_USERS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_USER_ACTION_CHANGE_TARIFF)
    public void startChangeTariff(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(0).withQuery(""));

        Page<Tariff> nextPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, "", PageRequest.of(0, pageSize)
                .withSort(Sort.Direction.ASC, Tariff_.TRANSACTION_TYPE1_AMOUNT_SUN, Tariff_.TRANSACTION_TYPE2_AMOUNT_SUN));
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
        Page<Tariff> nextPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, queryStr, PageRequest.of(pageNumber, pageSize)
                .withSort(Sort.Direction.ASC, Tariff_.TRANSACTION_TYPE1_AMOUNT_SUN));
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
        Page<Tariff> prevPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, queryStr, PageRequest.of(pageNumber, pageSize)
                .withSort(Sort.Direction.ASC, Tariff_.TRANSACTION_TYPE1_AMOUNT_SUN));
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

    @MatchState(state = States.ADMIN_MANAGE_USERS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_USER_ACTION_ADJUST_WITHDRAW_LIMIT)
    public void startAdjustWithdrawLimit(UserState userState, Update update) {
        manageUserActionsView.promptNewUserWithdrawLimit(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_USER_ACTION_PROMPT_WITHDRAW_LIMIT));
    }

    @MatchState(state = States.ADMIN_MANAGE_USER_ACTION_PROMPT_WITHDRAW_LIMIT, updateTypes = UpdateType.MESSAGE)
    public void handleNewWithdrawLimit(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Adjusting user balance: {}", message.getText());
            String newLimitTrx = message.getText();

            Long adjustedLimitInSunLong;
            try {
                adjustedLimitInSunLong = parseUtils.parseTrxStrToSunLong(newLimitTrx);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid TRX amount: {}", newLimitTrx);
                return;
            }

            Long telegramId = userState.getTelegramId();
            if (adjustedLimitInSunLong < 0) {
                logger.warn("Adjusted withdraw limit is negative: {}", adjustedLimitInSunLong);
                manageUserActionsView.withdrawLimitIsNegative(userState);
                return;
            }

            UserEdit userEdit = telegramState.getOrCreateUserEdit(telegramId);
            AppUser byId = userService.getById(userEdit.getSelectedUserId());

            balanceService.adjustWithdrawLimit(byId.getBalance().getId(), adjustedLimitInSunLong);

            manageUserActionsView.userWithdrawAdjusted(userState);
        }
    }
}
