package org.ipan.nrgyrent.telegram.views;

import java.util.ArrayList;
import java.util.List;

import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.ManageUserLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.utils.ParseUtils;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@AllArgsConstructor
public class ManageUserActionsView {
    public static final String OPEN_BALANCE = "/balance/";

    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final ManageUserLabels manageUserLabels;
    private final FormattingTools formattingTools;

    public void updMenuToManageUsersSearchResult(Page<AppUser> page, UserState userState) {
        String text = page.isEmpty() ? commonLabels.searchNoResults()
                : commonLabels.searchResults();

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .replyMarkup(getUsersSearchPageMarkup(page))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updMenuToManageUsersSearchResult userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void updMenuToManageUserActionsMenu(UserState userState, AppUser appUser, BalanceReferralProgram refProgram) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getBalanceDescription(appUser, refProgram))
                .parseMode("MARKDOWN")
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .replyMarkup(getManageUserActionsMarkup(!appUser.getDisabled()))
                .build();
        tgClient.execute(message);
    }

    public void groupBalanceIsNegative(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeBalanceNegative())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not groupBalanceIsNegative userstate {}", userState, e);
        }
    }

    public void withdrawLimitIsNegative(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeWithdrawLimitNegative())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not withdrawLimitIsNegative userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void userDeleted(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.deactivateSuccess())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void userBalanceAdjusted(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeBalanceSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void userWithdrawAdjusted(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeWithdrawSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }


    @SneakyThrows
    public void userTariffChanged(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeTariffSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void userRefProgramChanged(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeRefProgramSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void promptNewUserBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeBalancePromptAmount())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void promptNewUserWithdrawLimit(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.changeWithdrawLimitPromptAmount())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void confirmDeactivateUserMsg(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.deactivateConfirm())
                .replyMarkup(confirmDeleteGroupMarkup(userState))
                .build();
        tgClient.execute(message);
    }

    public void updMenuToUserAutoDelegation(
            UserState userState,
            AppUser user,
            List<AutoDelegationSession> activeSessions) {
        String configuredProvider = user.getAutoDelegationProvider() == null
                ? manageUserLabels.autoDelegationDefaultProvider()
                : user.getAutoDelegationProvider().name();
        String effectiveProvider = user.getAutoDelegationProviderToUse() == null
                ? manageUserLabels.autoDelegationDefaultProvider()
                : user.getAutoDelegationProviderToUse().name();
        String sessionsText = activeSessions.isEmpty()
                ? manageUserLabels.autoDelegationNoActiveSessions()
                : manageUserLabels.autoDelegationActiveSessions();

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(String.join("\n\n",
                        manageUserLabels.autoDelegationDescription(),
                        manageUserLabels.autoDelegationConfiguredProvider(configuredProvider),
                        manageUserLabels.autoDelegationEffectiveProvider(effectiveProvider),
                        manageUserLabels.autoDelegationWarningActiveUnaffected(),
                        sessionsText))
                .replyMarkup(getUserAutoDelegationMarkup(activeSessions))
                .build();
        executeMenuUpdate(message, "updMenuToUserAutoDelegation", userState);
    }

    public void updMenuToUserAutoDelegationSession(UserState userState, AutoDelegationSession session) {
        EnergyProviderName targetProvider = session.getEnergyProvider() == EnergyProviderName.ITRX
                ? EnergyProviderName.TRXX
                : EnergyProviderName.ITRX;
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.autoDelegationSession(
                        session.getId().toString(),
                        WalletTools.formatTronAddress(session.getAddress()),
                        session.getEnergyProvider().name(),
                        FormattingTools.formatDtUtc(session.getCreatedAt())))
                .replyMarkup(InlineKeyboardMarkup
                        .builder()
                        .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton
                                .builder()
                                .text(manageUserLabels.autoDelegationChangeProvider(targetProvider.name()))
                                .callbackData(InlineMenuCallbacks.getUserAutoSwitchCallback(session.getId(), targetProvider))
                                .build()))
                        .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton
                                .builder()
                                .text(manageUserLabels.autoDelegationDeactivate())
                                .callbackData(InlineMenuCallbacks.getUserAutoDeactivateCallback(session.getId()))
                                .build()))
                        .keyboardRow(new InlineKeyboardRow(
                                InlineKeyboardButton.builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build(),
                                InlineKeyboardButton.builder()
                                        .text(commonLabels.goBack())
                                        .callbackData(InlineMenuCallbacks.GO_BACK)
                                        .build()))
                        .build())
                .build();
        executeMenuUpdate(message, "updMenuToUserAutoDelegationSession", userState);
    }

    public boolean confirmUserAutoDelegationSwitch(
            UserState userState,
            AutoDelegationSession session,
            EnergyProviderName targetProvider) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.autoDelegationSwitchConfirm(targetProvider.name()))
                .replyMarkup(confirmUserAutoDelegationActionMarkup(
                        InlineMenuCallbacks.getUserAutoSessionCallback(session.getId()),
                        InlineMenuCallbacks.getUserAutoSwitchConfirmCallback(session.getId(), targetProvider)))
                .build();
        return executeMenuUpdate(message, "confirmUserAutoDelegationSwitch", userState);
    }

    public boolean confirmUserAutoDelegationDeactivate(UserState userState, AutoDelegationSession session) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.autoDelegationDeactivateConfirm())
                .replyMarkup(confirmUserAutoDelegationActionMarkup(
                        InlineMenuCallbacks.getUserAutoSessionCallback(session.getId()),
                        InlineMenuCallbacks.getUserAutoDeactivateConfirmCallback(session.getId())))
                .build();
        return executeMenuUpdate(message, "confirmUserAutoDelegationDeactivate", userState);
    }

    public void userAutoDelegationActionFailed(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(manageUserLabels.autoDelegationActionFailed())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        executeMenuUpdate(message, "userAutoDelegationActionFailed", userState);
    }

    public InlineKeyboardMarkup confirmDeleteGroupMarkup(UserState userState) {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.no())
                                        .callbackData(InlineMenuCallbacks.CONFIRM_NO)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.yes())
                                        .callbackData(InlineMenuCallbacks.CONFIRM_YES)
                                        .build()))
                .build();
    }

    private String getBalanceDescription(AppUser user, BalanceReferralProgram refProgram) {
        Tariff tariff = user.getBalance().getTariff();
        String tariffLabel = "";
        if (tariff == null) {
            logger.error("Tariff is null for user: {}", user.getTelegramId());
        } else {
            tariffLabel = String.format("%s (%s TRX, %s TRX)", 
            tariff.getLabel(), 
            FormattingTools.formatBalance(tariff.getTransactionType1AmountSun()),
            FormattingTools.formatBalance(tariff.getTransactionType2AmountSun()));
        }

        // TODO: view group if present
        return manageUserLabels.preview(
                user.getTelegramId().toString(),
                ParseUtils.escapeMarkdown(FormattingTools.valOrDash(user.getTelegramUsername())),
                ParseUtils.escapeMarkdown(FormattingTools.valOrDash(user.getTelegramFirstName())),
                ParseUtils.escapeMarkdown(tariffLabel),
                user.getDisabled() ? commonLabels.cross() : commonLabels.check(),
                user.getBalance().getDepositAddress(),
                FormattingTools.formatBalance(user.getBalance().getSunBalance()),
                FormattingTools.formatBalance(user.getBalance().getDailyWithdrawalLimitSun()),
                FormattingTools.formatBalance(user.getBalance().getDailyWithdrawalRemainingSun()),
                refProgram == null ? "" : ParseUtils.escapeMarkdown(formattingTools.formatRefProgmam(refProgram))
                );
    }

    private InlineKeyboardMarkup getUserAutoDelegationMarkup(List<AutoDelegationSession> activeSessions) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(manageUserLabels.autoDelegationUseDefault())
                                .callbackData(InlineMenuCallbacks.MANAGE_USER_AUTODELEGATION_PROVIDER_DEFAULT)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(EnergyProviderName.ITRX.name())
                                .callbackData(InlineMenuCallbacks.MANAGE_USER_AUTODELEGATION_PROVIDER_ITRX)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(EnergyProviderName.TRXX.name())
                                .callbackData(InlineMenuCallbacks.MANAGE_USER_AUTODELEGATION_PROVIDER_TRXX)
                                .build()));
        activeSessions.forEach(session -> builder.keyboardRow(new InlineKeyboardRow(
                InlineKeyboardButton.builder()
                        .text(manageUserLabels.autoDelegationSession(
                                session.getId().toString(),
                                WalletTools.formatTronAddress(session.getAddress()),
                                session.getEnergyProvider().name(),
                                FormattingTools.formatDtUtc(session.getCreatedAt())))
                        .callbackData(InlineMenuCallbacks.getUserAutoSessionCallback(session.getId()))
                        .build())));
        return builder
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.toMainMenu())
                                .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(commonLabels.goBack())
                                .callbackData(InlineMenuCallbacks.GO_BACK)
                                .build()))
                .build();
    }

    private InlineKeyboardMarkup confirmUserAutoDelegationActionMarkup(String cancelCallback, String confirmCallback) {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text(commonLabels.no())
                                .callbackData(cancelCallback)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text(commonLabels.yes())
                                .callbackData(confirmCallback)
                                .build()))
                .build();
    }

    private boolean executeMenuUpdate(EditMessageText message, String operation, UserState userState) {
        try {
            tgClient.execute(message);
            return true;
        } catch (Exception e) {
            logger.error("Could not {} userstate {}", operation, userState, e);
            return false;
        }
    }

    private InlineKeyboardMarkup getManageUserActionsMarkup(Boolean showDeactivateBtn) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageUserLabels.menuChangeRefProgram())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_CHANGE_REF_PROGRAM)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageUserLabels.menuChangeTariff())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_CHANGE_TARIFF)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageUserLabels.menuChangeBalance())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_ADJUST_BALANCE_MANUALLY)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageUserLabels.menuChangeWithdrawLimit())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_ADJUST_WITHDRAW_LIMIT)
                                        .build()));

        builder.keyboardRow(
                new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text(manageUserLabels.menuAutoDelegation())
                                .callbackData(InlineMenuCallbacks.MANAGE_USER_AUTODELEGATION)
                                .build()));

        if (showDeactivateBtn) {
                builder.keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(manageUserLabels.menuDeactivate())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USER_ACTION_DEACTIVATE)
                                        .build()));
        }
        return builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.goBack())
                                        .callbackData(InlineMenuCallbacks.GO_BACK)
                                        .build()))
                .build();
    }

    private InlineKeyboardMarkup getUsersSearchPageMarkup(Page<AppUser> page) {
        List<InlineKeyboardRow> users = page.getContent().stream().map(user -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(formattingTools.formatUserForSearch(user))
                            .callbackData(openBalanceRequest(user.getTelegramId()))
                            .build());
            return row;
        }).toList();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        users.forEach(builder::keyboardRow);

        builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.searchReset())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USERS_SEARCH_RESET)
                                        .build()));

        boolean hasPrev = page.hasPrevious();
        boolean hasNext = page.hasNext();

        if (hasPrev || hasNext) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (hasPrev) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(commonLabels.searchPrevPage())
                                .callbackData(InlineMenuCallbacks.MANAGE_USERS_PREV_PAGE)
                                .build());
            }
            if (hasNext) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(commonLabels.searchNextPage())
                                .callbackData(InlineMenuCallbacks.MANAGE_USERS_NEXT_PAGE)
                                .build());
            }
            builder.keyboardRow(new InlineKeyboardRow(buttons));
        }

        return builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build(),
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.goBack())
                                        .callbackData(InlineMenuCallbacks.GO_BACK)
                                        .build()))
                .build();
    }

    private String openBalanceRequest(Long userId) {
        return OPEN_BALANCE + userId;
    }
}
