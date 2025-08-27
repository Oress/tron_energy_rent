package org.ipan.nrgyrent.telegram.handlers.referrals;

import java.util.Optional;

import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.domain.model.ReferralProgram_;
import org.ipan.nrgyrent.domain.model.repository.BalanceRepo;
import org.ipan.nrgyrent.domain.model.repository.ReferralProgramRepo;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.TelegramMessages;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.state.referral.RefProgramEdit;
import org.ipan.nrgyrent.telegram.state.referral.RefProgramSearchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.TransitionHandler;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsActionsView;
import org.ipan.nrgyrent.telegram.views.referrals.ReferralProgramsSearchView;
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
public class ReferralProgramsSearchHandler {
    private final int pageSize;
    private final TelegramState telegramState;
    private final TelegramMessages telegramMessages;
    private final ReferralProgramRepo referralProgramRepo;
    private final ReferralProgramsSearchView referralProgramsSearchView;
    private final ReferralProgramsActionsView referralProgramsActionsView;

    public ReferralProgramsSearchHandler(
            @Value("${app.pagination.ref-programs.page-size:20}") int pageSize,
            TelegramState telegramState,
            TelegramMessages telegramMessages,
            BalanceRepo balanceRepo,
            ReferralProgramRepo referralProgramRepo,
            ReferralProgramsSearchView referralProgramsSearchView,
            ReferralProgramsActionsView referralProgramsActionsView) {
        this.pageSize = pageSize;
        this.telegramState = telegramState;
        this.telegramMessages = telegramMessages;
        this.referralProgramRepo = referralProgramRepo;
        this.referralProgramsSearchView = referralProgramsSearchView;
        this.referralProgramsActionsView = referralProgramsActionsView;
    }

    @MatchStates({
            @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAMS, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_SEARCH),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAMS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_SEARCH_RESET)
    })
    public void resetSearch(UserState userState, Update update) {
        RefProgramSearchState searchState = telegramState.getOrCreateRefProgramSearchState(userState.getTelegramId());
        telegramState.updateRefProgramSearchState(userState.getTelegramId(), searchState.withCurrentPage(0).withQuery(""));

        Page<ReferralProgram> nextPage = referralProgramRepo.findAll(PageRequest.of(0, pageSize).withSort(Sort.by(ReferralProgram_.ID)));
        referralProgramsSearchView.updMenuToSearchResult(nextPage, userState);
        telegramState.updateUserState(userState.getTelegramId(), userState.withState(States.ADMIN_MANAGE_REF_PROGRAMS_SEARCH));
    }

    @MatchState(forAdmin = true, state = States.ADMIN_MANAGE_REF_PROGRAMS_SEARCH, updateTypes = UpdateType.MESSAGE)
    public void searchByLabel(UserState userState, Update update) {
        Message message = update.getMessage();
        if (message.hasText()) {
            logger.info("Searching for ReferralPrograms with label: {}", message.getText());
            String queryStr = message.getText();
            telegramMessages.deleteMessage(message);

            if (queryStr.length() < 2) {
                logger.info("Query string is too short: {}", queryStr);
                return;
            }

            RefProgramSearchState searchState = telegramState.getOrCreateRefProgramSearchState(userState.getTelegramId());
            telegramState.updateRefProgramSearchState(userState.getTelegramId(), searchState.withQuery(queryStr));
            Page<ReferralProgram> firstPage = referralProgramRepo.findByLabelContainingIgnoreCaseOrderById(queryStr, PageRequest.of(0, pageSize));
            referralProgramsSearchView.updMenuToSearchResult(firstPage, userState);
        }
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAMS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_NEXT_PAGE)
    })
    public void nextPage(UserState userState, Update update) {
        RefProgramSearchState searchState = telegramState.getOrCreateRefProgramSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() + 1;
        String queryStr = searchState.getQuery();
        telegramState.updateRefProgramSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<ReferralProgram> nextPage = referralProgramRepo.findByLabelContainingIgnoreCaseOrderById(queryStr, PageRequest.of(pageNumber, pageSize));
        referralProgramsSearchView.updMenuToSearchResult(nextPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAMS_SEARCH, callbackData = InlineMenuCallbacks.MANAGE_REF_PROGRAMS_PREV_PAGE)
    })
    public void prevPage(UserState userState, Update update) {
        RefProgramSearchState searchState = telegramState.getOrCreateRefProgramSearchState(userState.getTelegramId());
        int pageNumber = searchState.getCurrentPage() - 1;
        String queryStr = searchState.getQuery();
        telegramState.updateRefProgramSearchState(userState.getTelegramId(), searchState.withCurrentPage(pageNumber));
        Page<ReferralProgram> prevPage = referralProgramRepo.findByLabelContainingIgnoreCaseOrderById(queryStr, PageRequest.of(pageNumber, pageSize));
        referralProgramsSearchView.updMenuToSearchResult(prevPage, userState);
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAMS_SEARCH, updateTypes = UpdateType.CALLBACK_QUERY),
    })
    public void openRefProgramFromSearch(UserState userState, Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        if (data.startsWith(ReferralProgramsSearchView.OPEN_REF_PROGRAM)) {
            String refProgramIdStr = data.split(ReferralProgramsSearchView.OPEN_REF_PROGRAM)[1];
            Long refProgramId = Long.parseLong(refProgramIdStr);
            openRefProgram(userState, refProgramId);
        }
    }

    @MatchStates({
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PROMPT_NEW_LABEL, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_RENAMED_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_AMOUNT_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_DEACTIVATE_SUCCESS, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PROMPT_PERCENTAGE, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX1, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX2, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX1_AUTO, callbackData = InlineMenuCallbacks.GO_BACK),
            @MatchState(state = States.ADMIN_MANAGE_REF_PROGRAM_ACTION_CHANGE_BASE_TX1_AUTO, callbackData = InlineMenuCallbacks.GO_BACK),
    })
    public void openRefProgramFromBackBtn(UserState userState, Update update) {
        RefProgramEdit refProgramEdit = telegramState.getOrCreateRefProgramEdit(userState.getTelegramId());

        if (refProgramEdit.getSelectedRefProgramId() != null) {
            Long refProgramId = refProgramEdit.getSelectedRefProgramId();
            openRefProgram(userState, refProgramId);
        }
    }

    public void openRefProgram(UserState userState, Long refProgramId) {
        Optional<ReferralProgram> refProgram = referralProgramRepo.findById(refProgramId);
        if (refProgram.isPresent()) {
            ReferralProgram foundRefProgram = refProgram.get();
            referralProgramsActionsView.updMenuToManageRefProgramActionsMenu(userState, foundRefProgram);
            telegramState.updateRefProgramEdit(userState.getTelegramId(), telegramState
                    .getOrCreateRefProgramEdit(userState.getTelegramId()).withSelectedRefProgramId(refProgramId));
            telegramState.updateUserState(userState.getTelegramId(),
                    userState.withState(States.ADMIN_MANAGE_REF_PROGRAM_ACTION_PREVIEW));
        } else {
            logger.error("ReferralProgram not found for ID: {}", refProgramId);
        }
    }
}
