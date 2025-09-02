package org.ipan.nrgyrent.telegram;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.projections.ReferralDto;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.TransactionLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.ipan.nrgyrent.telegram.views.CommonViews;
import org.ipan.nrgyrent.telegram.views.ManageGroupNewGroupView;
import org.ipan.nrgyrent.telegram.views.ManageGroupSearchView;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessages;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.message.Message;
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
public class TelegramMessages {
    private final TelegramClient tgClient;
    private final ManageGroupNewGroupView manageGroupView;
    private final ManageGroupSearchView manageGroupSearchView;
    private final CommonLabels commonLabels;
    private final TransactionLabels transactionLabels;
    private final FormattingTools formattingTools;
    private final CommonViews commonViews;

    public ManageGroupNewGroupView manageGroupView() {
        return manageGroupView;
    }

    public ManageGroupSearchView manageGroupSearchView() {
        return manageGroupSearchView;
    }

    @SneakyThrows
    public void updMenuToReferalSummary(UserState userState, AppUser user, BalanceReferralProgram refProgram,
                                        List<ReferralDto> referals, Long pendingCommissionSun) {
        String referalsStr = referals.stream().map(u -> formattingTools.formatReferral(u))
                .collect(Collectors.joining("\n"));
        if (referalsStr.isBlank()) {
            referalsStr = commonLabels.noReferrals();
        }
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.referalsSummary(
                        FormattingTools.formatBalance(pendingCommissionSun),
                        refProgram == null ? commonLabels.refProgramNotSet() : formattingTools.formatRefProgmamWoDescription(refProgram),
                        referalsStr))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to updMenuToReferalSummary user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void updateMsgToSettings(UserState userState, AppUser user) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.settingsDescription())
                .replyMarkup(settingsMenuMarkup(user.getShowWalletsMenu()))
                .parseMode("MARKDOWN")
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to updateMsgToSettings user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void updateMsgToChangeLanguage(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(commonLabels.settingsChangeLanguage())
                .replyMarkup(changeLanguageNotficationMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to updateMsgToChangeLanguage user: {}", userState, e);
        }
    }

    @SneakyThrows
    public void sendTransactionRefundNotification(UserState userState, Order order) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(order.getChatId())
                .messageId(order.getMessageToUpdate())
                .text(getFailedTransactionMessage(userState, order))
                // .replyMarkup(getToMainMenuNotificationMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to sendTransactionRefundNotification user: {}", userState, e);
        }
    }

    public void sendTransactionSuccessNotification(UserState userState, Order order) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(order.getChatId())
                .messageId(order.getMessageToUpdate())
                .text(order.getBalance().isGroup()
                        ? getSuccessfulTransactionMessageGroup(userState, order)
                        : getSuccessfulTransactionMessage(userState, order)
                        )
                // .replyMarkup(getToMainMenuNotificationMarkup())
                 .parseMode("MARKDOWN")
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to sendTransactionSuccessNotification user: {}", userState, e);
        }
        return;
    }

    private String getSuccessfulTransactionMessage(UserState userState, Order order) {
        return transactionLabels.successIndividual(
                userState.getLocaleOrDefault(),
                order.getTxAmount(),
                FormattingTools.formatBalance(order.getSunAmount()),
                WalletTools.formatTronAddressMd(order.getReceiveAddress()),
                FormattingTools.formatBalance(order.getBalance().getSunBalance()),
                formattingTools.formatDtUtc(order.getCreatedAt()),
                FormattingTools.formatBalance(order.getOldBalance())
        );
    }

    private String getSuccessfulTransactionMessageGroup(UserState userState, Order order) {
        return transactionLabels.successGroup(
                userState.getLocaleOrDefault(),
                order.getTxAmount(),
                FormattingTools.formatBalance(order.getSunAmount()),
                WalletTools.formatTronAddressMd(order.getReceiveAddress()),
                formattingTools.formatDtUtc(order.getCreatedAt())
        );
    }

    private String getFailedTransactionMessage(UserState userState, Order order) {
        return transactionLabels.refunded(
                userState.getLocaleOrDefault(),
                order.getTxAmount(),
                FormattingTools.formatBalance(order.getSunAmount()),
                WalletTools.formatTronAddress(order.getReceiveAddress()));
    }

    @SneakyThrows
    public void sendWithdrawalSuccessful(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(commonLabels.withdrawSuccess(userState.getLocaleOrDefault()))
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendWithdrawalFail(UserState userState) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(commonLabels.withdrawFail(userState.getLocaleOrDefault()))
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendTopupNotification(UserState userState, Long depositSun) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(commonLabels.topup(userState.getLocaleOrDefault(), FormattingTools.formatBalance(depositSun)))
                .parseMode("MARKDOWN")
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendTopupUsdtNotification(UserState userState, DepositTransaction depositTransaction) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(commonLabels.topupUsdt(userState.getLocaleOrDefault(),
                        FormattingTools.formatUsdt(depositTransaction.getOriginalAmount()),
                        depositTransaction.getTrxToUsdtRate().toString(),
                        BigDecimal.valueOf(depositTransaction.getBybitFeeSun()).divide(AppConstants.trxToSunRate, 6, RoundingMode.DOWN).toString(),
                        FormattingTools.formatBalance(depositTransaction.getAmount()))
                )
                .parseMode("MARKDOWN")
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendReferalPaymentNotification(UserState userState, Long amountSun) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .disableNotification(true)
                .text(commonLabels.referalPayment(userState.getLocaleOrDefault(),
                        FormattingTools.formatBalance(amountSun)))
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendLowItrxBalanceAlert(UserState userState, Long currentBalance) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(commonLabels.alertItrxBalanceLow(userState.getLocaleOrDefault(),
                        FormattingTools.formatBalance(currentBalance)))
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendLowTrxxBalanceAlert(UserState userState, Long currentBalance) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(commonLabels.alertTrxxBalanceLow(userState.getLocaleOrDefault(),
                        FormattingTools.formatBalance(currentBalance)))
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void sendLowCatfeeBalanceAlert(UserState userState, Long currentBalance) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(commonLabels.alertCatfeeBalanceLow(userState.getLocaleOrDefault(),
                        FormattingTools.formatBalance(currentBalance)))
                .replyMarkup(getOkNotificationMarkup())
                .build();
        tgClient.execute(message);
    }


    public void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = DeleteMessage
                .builder()
                .chatId(chatId)
                .messageId(messageId)
                .build();
        try {
            tgClient.execute(deleteMessage);
        } catch (Exception e) {
            logger.error("Failed to delete message for chat msg: {} chatid: {}", messageId, chatId, e);
        }
    }

    public void deleteMessages(Long chatId, List<Integer> messageIds) {
        DeleteMessages deleteMessages = DeleteMessages
                .builder()
                .chatId(chatId)
                .messageIds(messageIds)
                .build();
        try {
            tgClient.execute(deleteMessages);
        } catch (Exception e) {
            logger.error("Failed to delete messages for chat msgs: {} chatid: {}", messageIds, chatId, e);
        }
    }

    public void deleteMessage(Message message) {
        DeleteMessage deleteMessage = DeleteMessage
                .builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .build();
        try {
            tgClient.execute(deleteMessage);
        } catch (Exception e) {
            logger.error("Failed to delete message {}", message.getMessageId(), e);
        }
    }

    public void deleteMessage(UserState userState, CallbackQuery callbackQuery) {
        DeleteMessage deleteMessage = DeleteMessage
                .builder()
                .chatId(userState.getChatId())
                .messageId(callbackQuery.getMessage().getMessageId())
                .build();
        try {
            tgClient.execute(deleteMessage);
        } catch (Exception e) {
            logger.error("Failed to delete callback message {}, userstate {}",
                    callbackQuery.getMessage().getMessageId(), userState, e);
        }
    }

    @SneakyThrows
    public Message sendUserMainMenuBasedOnRole(UserState userState, Long chatId, AppUser user, List<UserWallet> wallets) {
        UserRole role = user != null ? user.getRole() : UserRole.USER;

        Message newMenuMsg = switch (role) {
            case ADMIN -> sendAdminMainMenu(userState, chatId, user, wallets);
            default -> sendMainMenu(userState, chatId, user, wallets);
        };
        return newMenuMsg;
    }

    @Retryable
    @SneakyThrows
    public Message sendMainMenu(UserState userState, Long chatId, AppUser user, List<UserWallet> wallets) {
        Tariff tariff = user.getTariffToUse();
        boolean showWithdrawBtn = !user.isInGroup() || user.isGroupManager();

        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text(getMainMenuMessage(userState, user, tariff))
                .replyMarkup(getMainMenuReplyMarkup(userState.isManager(), false, tariff, showWithdrawBtn, wallets))
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .parseMode("MARKDOWN")
                .build();
        return tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public Message sendAdminMainMenu(UserState userState, Long chatId, AppUser user, List<UserWallet> wallets) {
        Tariff tariff = user.getTariffToUse();
        boolean showWithdrawBtn = !user.isInGroup() || user.isGroupManager();

        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .text(getMainMenuMessage(userState, user, tariff))
                .replyMarkup(getMainMenuReplyMarkup(userState.isManager(), true, tariff, showWithdrawBtn, wallets))
                .parseMode("MARKDOWN")
                .build();
        return tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public Message sendPromptLanguage(UserState userState, Long chatId) {
        SendMessage message = SendMessage
                .builder()
                .chatId(chatId)
                .text("""
                        👋 Добро пожаловать в Flash Tron Rent!
                        Пожалуйста, выберите предпочитаемый язык, чтобы продолжить.
                        Вы всегда можете изменить язык в меню настроек позже.
                        ────────
                        👋 Ласкаво просимо до Flash Tron Rent!
                        Будь ласка, виберіть потрібну мову, щоб продовжити.
                        Ви завжди можете змінити мову в меню налаштувань пізніше.
                        ────────
                        👋 Welcome to the Flash Tron Rent!
                        Please select your preferred language to continue.
                        You can always change the preferred language in settings menu later.
                        """)
                .replyMarkup(getLanguageNotficationMarkup())
                .parseMode("MARKDOWN")
                .build();
        return tgClient.execute(message);
    }

    @SneakyThrows
    public void updateUserMainMenuBasedOnRole(UserState userState, AppUser user, List<UserWallet> wallets) {
        UserRole role = user != null ? user.getRole() : UserRole.USER;

        switch (role) {
            case ADMIN -> updateMsgToAdminMainMenu(userState, user, wallets);
            default -> updateMsgToMainMenu(userState, user, wallets);
        }
    }

    @SneakyThrows
    public void updateMsgToMainMenu(UserState userState, AppUser user, List<UserWallet> wallets) {
        Tariff tariff = user.getTariffToUse();
        boolean showWithdrawBtn = !user.isInGroup() || user.isGroupManager();

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getMainMenuMessage(userState, user, tariff))
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .parseMode("MARKDOWN")
                .replyMarkup(getMainMenuReplyMarkup(userState.isManager(), false, tariff, showWithdrawBtn, wallets))
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void updateMsgToAdminMainMenu(UserState userState, AppUser user, List<UserWallet> userWallets) {
        Tariff tariff = user.getTariffToUse();
        boolean showWithdrawBtn = !user.isInGroup() || user.isGroupManager();

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getMainMenuMessage(userState, user, tariff))
                .parseMode("MARKDOWN")
                .linkPreviewOptions(LinkPreviewOptions.builder().isDisabled(true).build())
                .replyMarkup(getMainMenuReplyMarkup(userState.isManager(), true, tariff, showWithdrawBtn, userWallets))
                .build();
        tgClient.execute(message);
    }

    private String getMainMenuMessage(UserState userState, AppUser user, Tariff tariff) {
        Balance balanceToUse = user.getBalanceToUse();

        String balanceLabel = user.isInGroup()
                ? commonLabels.getCommonGroupBalance(userState.getLocaleOrDefault(), FormattingTools.formatBalance(balanceToUse.getSunBalance()))
                : commonLabels.getCommonPersonalBalance(userState.getLocaleOrDefault(), FormattingTools.formatBalance(balanceToUse.getSunBalance()));

        String mainWelcome = commonLabels.getMainWelcome(userState.getLocaleOrDefault(), balanceLabel,
                FormattingTools.formatBalance(tariff.getTransactionType1AmountSun())
                );

        return mainWelcome.formatted(balanceLabel);
    }

    private InlineKeyboardMarkup getLanguageNotficationMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("Русский")
                                        .callbackData("ru")
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("Українська")
                                        .callbackData("uk")
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("English")
                                        .callbackData("en")
                                        .build()))
                .build();
    }

    private InlineKeyboardMarkup changeLanguageNotficationMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("Русский")
                                        .callbackData("ru")
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("Українська")
                                        .callbackData("uk")
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("English")
                                        .callbackData("en")
                                        .build()))
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

    private InlineKeyboardMarkup settingsMenuMarkup(Boolean showWalletsInMenuEnabled) {
        var builder = InlineKeyboardMarkup.builder();

        if (showWalletsInMenuEnabled) {
                builder.keyboardRow(
                new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text(commonLabels.settingsShowWalletsEnabled())
                                .callbackData(InlineMenuCallbacks.OPT_SHOW_WALLET_DISABLE)
                                .build()));
        } else {
                builder.keyboardRow(
                new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text(commonLabels.settingsShowWalletsDisabled())
                                .callbackData(InlineMenuCallbacks.OPT_SHOW_WALLET_ENABLE)
                                .build()));
        }

        builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.getMenuManageReferals())
                                        .callbackData(InlineMenuCallbacks.MANAGE_REFERALS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.settingsTxHistory())
                                        .callbackData(InlineMenuCallbacks.HISTORY)
                                        .build()))

                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.settingsChangeLanguage())
                                        .callbackData(InlineMenuCallbacks.CHANGE_LANGUAGE)
                                        .build()));
                builder.keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()));
        return builder.build();
    }

    private InlineKeyboardMarkup getMainMenuReplyMarkup(Boolean isManager, Boolean isAdmin, Tariff tariff,
            boolean showWithdrawBtn, List<UserWallet> wallets) {
        var builder = InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.getTxType1(
                                                FormattingTools.formatBalance(tariff.getTransactionType1AmountSun())))
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_65k)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.getTxType2(
                                                FormattingTools.formatBalance(tariff.getTransactionType2AmountSun())))
                                        .callbackData(InlineMenuCallbacks.TRANSACTION_131k)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.getTxCustomAmnt(
                                                FormattingTools.formatBalance(tariff.getTransactionType1AmountSun())))
                                        .callbackData(InlineMenuCallbacks.CUSTOM_TRANSACTION_AMOUNT)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.getEstimateTxCost())
                                        .callbackData(InlineMenuCallbacks.ESTIMATE_TRANSACTION_COST)
                                        .build()));
            builder
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.getAutoDelegation())
                                        .callbackData(InlineMenuCallbacks.AUTOTOPUP)
                                        .build()));

        if (showWithdrawBtn) {
            builder.keyboardRow(
                    new InlineKeyboardRow(
                            InlineKeyboardButton
                                    .builder()
                                    .text(commonLabels.getMenuDeposit())
                                    .callbackData(InlineMenuCallbacks.DEPOSIT)
                                    .build(),
                            InlineKeyboardButton
                                    .builder()
                                    .text(commonLabels.getMenuWithdraw())
                                    .callbackData(InlineMenuCallbacks.WITHDRAW_TRX)
                                    .build()));
        } else {
            builder.keyboardRow(
                    new InlineKeyboardRow(
                            InlineKeyboardButton
                                    .builder()
                                    .text(commonLabels.getMenuDeposit())
                                    .callbackData(InlineMenuCallbacks.DEPOSIT)
                                    .build()));
        }

        builder.keyboardRow(
                new InlineKeyboardRow(
                        InlineKeyboardButton
                                .builder()
                                .text(commonLabels.menuSettings())
                                .callbackData(InlineMenuCallbacks.SETTINGS)
                                .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.getMenuWallets())
                                        .callbackData(InlineMenuCallbacks.WALLETS)
                                        .build()));

        if (isManager) {
            builder.keyboardRow(
                    new InlineKeyboardRow(
                            InlineKeyboardButton
                                    .builder()
                                    .text(commonLabels.getMenuManageGroup())
                                    .callbackData(InlineMenuCallbacks.MANAGE_GROUP)
                                    .build()));
        }

        if (isAdmin) {
            builder.keyboardRow(
                    new InlineKeyboardRow(
                            InlineKeyboardButton
                                    .builder()
                                    .text(commonLabels.getMenuAdminMenu())
                                    .callbackData(InlineMenuCallbacks.ADMIN_MENU)
                                    .build()));
        }

        if (!wallets.isEmpty()) {
            for (UserWallet wallet : wallets) {
                builder.keyboardRow(
                    new InlineKeyboardRow(
                            InlineKeyboardButton
                                    .builder()
                                    .text(WalletTools.formatTronAddressAndLabel(wallet.getAddress(), wallet.getLabel()))
                                    .callbackData(InlineMenuCallbacks.getQuickTxCallback(wallet.getId()))
                                    .build()));
            }

        }
        return builder.build();
    }

    private InlineKeyboardMarkup getOkNotificationMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("OK")
                                        .callbackData(InlineMenuCallbacks.NTFN_OK)
                                        .build())

                )
                .build();
    }
}
