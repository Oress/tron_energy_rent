package org.ipan.nrgyrent.telegram.handlers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.ipan.nrgyrent.domain.exception.UserAlreadyHasGroupBalanceException;
import org.ipan.nrgyrent.domain.exception.UserIsDisabledException;
import org.ipan.nrgyrent.domain.exception.UserIsManagerException;
import org.ipan.nrgyrent.domain.exception.UsersMustBelongToTheSameGroupException;
import org.ipan.nrgyrent.domain.exception.UserNotRegisteredException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.repository.AppUserRepo;
import org.ipan.nrgyrent.domain.model.repository.TariffRepo;
import org.ipan.nrgyrent.domain.service.BalanceService;
import org.ipan.nrgyrent.domain.service.TariffService;
import org.ipan.nrgyrent.domain.service.commands.TgUserId;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.BalanceEdit;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.tariff.TariffSearchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.ManageGroupActionsView;
import org.ipan.nrgyrent.telegram.views.tariffs.TariffsSearchView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.UserShared;
import org.telegram.telegrambots.meta.api.objects.UsersShared;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import lombok.extern.slf4j.Slf4j;

@TransitionHandler
@Slf4j
public class ManageGroupActionsHandler {
    private final Integer pageSize;
    private final ManageGroupActionsView manageGroupActionsView;
    private final TariffService tariffService;
    private final TariffRepo tariffRepo;
    private final TariffsSearchView tariffsSearchView;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final AppUserRepo appUserRepo;
    private final BalanceService balanceService;
    private final ManageGroupSearchHandler manageGroupSearchHandler;

    public ManageGroupActionsHandler(@Value("${app.pagination.tariffs.page-size:20}")Integer pageSize,
     ManageGroupActionsView manageGroupActionsView,
            TariffService tariffService, TariffRepo tariffRepo, TariffsSearchView tariffsSearchView,
            TelegramState telegramState, TelegramMessages telegramMessages, AppUserRepo appUserRepo,
            BalanceService balanceService, ManageGroupSearchHandler manageGroupSearchHandler) {
        this.pageSize = pageSize;
        this.manageGroupActionsView = manageGroupActionsView;
        this.tariffService = tariffService;
        this.tariffRepo = tariffRepo;
        this.tariffsSearchView = tariffsSearchView;
        this.telegramState = telegramState;
        this.telegramMessages = telegramMessages;
        this.appUserRepo = appUserRepo;
        this.balanceService = balanceService;
        this.manageGroupSearchHandler = manageGroupSearchHandler;
    }

    // TODO: implement search by label
    @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_CHANGE_TARIFF)
    public void startChangeTariff(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(0).withQuery(""));

        Page<Tariff> nextPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, "", PageRequest.of(0, pageSize));
        tariffsSearchView.updMenuToTariffSearchResult(nextPage, userState);

        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_CHANGE_TARIFF_SEARCHING));
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_CHANGE_TARIFF_SEARCHING, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_NEXT_PAGE)
    })
    public void tariffChange_searchingNextPage(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() + 1;
        String queryStr = searchState.getQuery();
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<Tariff> nextPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, queryStr, PageRequest.of(pageNumber, pageSize));
        tariffsSearchView.updMenuToTariffSearchResult(nextPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_CHANGE_TARIFF_SEARCHING, callbackData = InlineMenuCallbacks.MANAGE_TARIFFS_PREV_PAGE)
    })
    public void tariffChange_searchingPrevPage(UserState userState, Update update) {
        TariffSearchState searchState = telegramState.getOrCreateTariffSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() - 1;
        String queryStr = searchState.getQuery();
        telegramState.updateTariffSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<Tariff> prevPage = tariffRepo.findByActiveAndLabelContainingIgnoreCaseOrderById(true, queryStr, PageRequest.of(pageNumber, pageSize));
        tariffsSearchView.updMenuToTariffSearchResult(prevPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_CHANGE_TARIFF_SEARCHING, updateTypes = UpdateType.CALLBACK_QUERY),
    })
    public void openTariffFromSearch(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        if (data.startsWith(TariffsSearchView.OPEN_TARIFF)) {
            String tariffIdStr = data.split(TariffsSearchView.OPEN_TARIFF)[1];
            Long tariffId = Long.parseLong(tariffIdStr);

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
            tariffService.changeGroupTariff(openBalance.getSelectedBalanceId(), tariffId);
            manageGroupActionsView.userTariffChanged(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_CHANGE_TARIFF_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_CHANGE_MANAGER)
    public void startChangeManager_promptManager(UserState userState, Update update) {
        manageGroupActionsView.updMenuPromptManager(userState);
        Message message = manageGroupActionsView.sendPromptManager(userState);

        telegramState.updateUserState(userState.getTelegramId(),
                userState.withMessagesToDelete(List.of(message.getMessageId()))
                        .withState(States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_MANAGER));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_MANAGER, updateTypes = UpdateType.MESSAGE)
    public void handleManager_end(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasUserShared()) {
            telegramMessages.deleteMessage(message);
            UsersShared usersShared = message.getUsersShared();
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);
            logger.info("Changing manager to: {} for {}", telegramId, openBalance.getSelectedBalanceId());

            try {
                UserShared newManager = usersShared.getUsers().get(0);
                TgUserId id = new TgUserId(newManager.getUserId(), newManager.getUsername(), newManager.getFirstName());
                balanceService.changeManager(openBalance.getSelectedBalanceId(), id);
            } catch (UserNotRegisteredException e) {
                logger.error("Error changing manager of a group: {}", openBalance.getSelectedBalanceId(), e);
                manageGroupActionsView.someUsersAreNotRegistered(userState, e.getUserIds());
                return;
            } catch (UserAlreadyHasGroupBalanceException e) {
                logger.error("Error changing manager of a group: {}", openBalance.getSelectedBalanceId(), e);
                manageGroupActionsView.userBelongsToAnotherGroup(userState);
                return;
            } catch (UserIsDisabledException e) {
                logger.error("Error changing manager of a group: {}", openBalance.getSelectedBalanceId(), e);
                manageGroupActionsView.userDisabled(userState);
                return;
            } catch (Exception e) {
                logger.error("Error changing manager of a group: {}", openBalance.getSelectedBalanceId(), e);
                manageGroupActionsView.somethingWentWrong(userState);
                return;
            }

            telegramMessages.deleteMessages(userState.getChatId(), userState.getMessagesToDelete());
            userState = telegramState.updateUserState(telegramId, userState.withMessagesToDelete(null));

            manageGroupActionsView.updMenuManagerChanged(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_MANAGER_CHANGED_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADJUST_BALANCE_MANUALLY)
    public void startAdjustBalanceManually(UserState userState, Update update) {
        manageGroupActionsView.promptNewGroupBalance(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_BALANCE));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_RENAME)
    public void startRenaming(UserState userState, Update update) {
        manageGroupActionsView.promptNewGroupLabel(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_LABEL));
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_ADD_USERS)
    public void startAddUsers(UserState userState, Update update) {
        manageGroupActionsView.updMenuPromptToAddUsersToGroup(userState);
        Message msg = manageGroupActionsView.promptToAddUsersToGroup(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_ADD_USERS)
                        .withMessagesToDelete(List.of(msg.getMessageId())));
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_ADD_USERS, updateTypes = UpdateType.MESSAGE)
    public void handleAddNewUsers(UserState userState, Update update) {
        Message message = update.getMessage();
        UsersShared usersShared = message.getUsersShared();;
        if (message != null && usersShared.getUsers() != null && !usersShared.getUsers().isEmpty()) {
            logger.info("Adding new users to group: {}", message.getText());
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);

            List<TgUserId> userIds = usersShared.getUsers().stream().map(user -> new TgUserId(user.getUserId(), user.getUsername(), user.getFirstName())).toList();
            try {
                balanceService.addUsersToTheGroupBalance(openBalance.getSelectedBalanceId(), userIds);
            } catch (UserNotRegisteredException e) {
                logger.error("Error adding users to group: {}", openBalance.getSelectedBalanceId(), e);
                manageGroupActionsView.someUsersAreNotRegistered(userState, e.getUserIds());
                return;
            } catch (UserIsDisabledException e) {
                logger.error("Error adding users to group: {}", openBalance.getSelectedBalanceId(), e);
                manageGroupActionsView.userDisabled(userState);
                return;
            }catch ( UserAlreadyHasGroupBalanceException e) {
                logger.error("Error adding users to group: {}", openBalance.getSelectedBalanceId(), e);
                manageGroupActionsView.userBelongsToAnotherGroup(userState);
                return;
            } catch (Exception e) {
                logger.error("Error adding users to group: {}", openBalance.getSelectedBalanceId(), e);
                manageGroupActionsView.somethingWentWrong(userState);
                return;
            }

            telegramMessages.deleteMessages(userState.getChatId(), userState.getMessagesToDelete());
            userState = telegramState.updateUserState(telegramId, userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_ADD_USERS_SUCCESS).withMessagesToDelete(null));

            manageGroupActionsView.groupUsersAdded(userState);
        }
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_REMOVE_USERS)
    public void startRemoveUsers(UserState userState, Update update) {
        manageGroupActionsView.updMenuPromptToRemoveUsersFromGroup(userState);
        Message msg = manageGroupActionsView.promptToRemoveUsersToGroup(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS)
                        .withMessagesToDelete(List.of(msg.getMessageId())));
    }

    @MatchState(state = States.ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS, updateTypes = UpdateType.MESSAGE)
    public void handleRemoveUsers(UserState userState, Update update) {
        Message message = update.getMessage();
        UsersShared usersShared = message.getUsersShared();
        if (message != null && usersShared.getUsers() != null && !usersShared.getUsers().isEmpty()) {
            logger.info("Removing users from group: {}", message.getText());
            Long telegramId = userState.getTelegramId();

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);

            List<TgUserId> userIds = usersShared.getUsers().stream().map(user -> new TgUserId(user.getUserId(), user.getUsername(), user.getFirstName())).toList();

            try {
                balanceService.removeUsersFromTheGroupBalance(openBalance.getSelectedBalanceId(), userIds);
            } catch (UserNotRegisteredException e) {
                logger.error("Error removing users from group: {}", e.getMessage());
                manageGroupActionsView.someUsersAreNotRegistered(userState, e.getUserIds());
                return;
            } catch (UserIsManagerException e) {
                logger.error("Error removing users from group: {}", e.getMessage());
                manageGroupActionsView.cannotRemoveManager(userState);
                return;
            }  catch (UsersMustBelongToTheSameGroupException e) {
                logger.error("Error removing users from group: {}", e.getMessage());
                manageGroupActionsView.cannotRemoveManager(userState);
                return;
            } catch (Exception e) {
                logger.error("Error removing users from group: {}", e.getMessage());
                manageGroupActionsView.somethingWentWrong(userState);
                return;
            }

            telegramMessages.deleteMessages(userState.getChatId(), userState.getMessagesToDelete());
            userState = telegramState.updateUserState(telegramId, userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_REMOVE_USERS_SUCCESS).withMessagesToDelete(null));

            manageGroupActionsView.groupUsersRemoved(userState);
        }
    }

    
    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_VIEW_USERS)
    public void viewGroupUsers(UserState userState, Update update) {
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
        Set<AppUser> users = appUserRepo.findAllByGroupBalanceId(openBalance.getSelectedBalanceId());
        manageGroupActionsView.reviewGroupUsers(userState, users);

        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_USERS_REVIEW));
    }


    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_LABEL, updateTypes = UpdateType.MESSAGE)
    public void handleNewLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Renaming group with new label: {}", message.getText());
            String newLabel = message.getText();
            Long telegramId = userState.getTelegramId();

            if (newLabel.length() < 3) {
                logger.warn("New label is too short: {}", newLabel);
                manageGroupActionsView.groupNameIsTooShort(userState);
                return;
            }

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);
            balanceService.renameGroupBalance(openBalance.getSelectedBalanceId(), newLabel);

            manageGroupActionsView.groupRenamed(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_RENAMED_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PROMPT_NEW_BALANCE, updateTypes = UpdateType.MESSAGE)
    public void handleAdjustedBalance(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Adjusting group balance: {}", message.getText());
            String newBalance = message.getText();
            // TODO: catch NumberFormatException
            BigDecimal adjustedBalanceInTrx = new BigDecimal(newBalance);
            BigDecimal adjustedBalanceInSun = adjustedBalanceInTrx.multiply(AppConstants.trxToSunRate);
            Long telegramId = userState.getTelegramId();

            long adjustedBalanceInSunLong = adjustedBalanceInSun.longValue();

            if (adjustedBalanceInSunLong < 0) {
                logger.warn("Adjusted balance is negative: {}", adjustedBalanceInSunLong);
                manageGroupActionsView.groupBalanceIsNegative(userState);
                return;
            }

            BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(telegramId);
            balanceService.adjustBalance(openBalance.getSelectedBalanceId(), adjustedBalanceInSunLong, telegramId);

            manageGroupActionsView.groupBalanceAdjusted(userState);
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_BALANCE_ADJUSTED_SUCCESS));
        }
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_PREVIEW, callbackData = InlineMenuCallbacks.MANAGE_GROUPS_ACTION_DEACTIVATE)
    public void handleDeactivateGroup(UserState userState, Update update) {
        manageGroupActionsView.confirmDeactivateGroupMsg(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_YES)
    public void confirmGroupDeactivate(UserState userState, Update update) {
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
        balanceService.deactivateGroupBalance(openBalance.getSelectedBalanceId());
        manageGroupActionsView.groupDeleted(userState);
        telegramState.updateUserState(userState.getTelegramId(),
                userState.withState(States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_SUCCESS));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_GROUPS_ACTION_DEACTIVATE_CONFIRM, callbackData = InlineMenuCallbacks.CONFIRM_NO)
    public void declineGroupDeactivate(UserState userState, Update update) {
        BalanceEdit openBalance = telegramState.getOrCreateBalanceEdit(userState.getTelegramId());
        manageGroupSearchHandler.openGroupBalance(userState, openBalance.getSelectedBalanceId());
    }
}