package org.ipan.nrgyrent.telegram.views;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.domain.model.UserWallet;
import org.ipan.nrgyrent.itrx.dto.ApiUsageResponse;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.AdminLabels;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@Component
@AllArgsConstructor
public class AdminViews {
    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final AdminLabels adminLabels;

    @Retryable
    @SneakyThrows
    public void withdrawTrxInProgress(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(adminLabels.withdrawInProgress())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void promptAmountAgainNotEnoughBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(adminLabels.withdrawNotEnough())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void withdrawTrxPromptAmount(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(adminLabels.withdrawPromptAmount())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void withdrawTrx(List<UserWallet> wallets, UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(adminLabels.withdrawPromptWallet())
                .replyMarkup(getTransactionsMenuMarkup(wallets))
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void itrxBalance(UserState userState, ApiUsageResponse apiUsageResponse) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getItrxBalanceMessage(apiUsageResponse))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void currentEnergyProvider(UserState userState, EnergyProviderName energyProviderName) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(adminLabels.energyProvider(energyProviderName.toString()))
                .replyMarkup(getEnergyProvidersReplyMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void currentAutoEnergyProvider(UserState userState, EnergyProviderName energyProviderName) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(adminLabels.autoEnergyProvider(energyProviderName.toString()))
                .replyMarkup(getEnergyProvidersReplyMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void sweepWalletsBalance(UserState userState, Map<CollectionWallet, Long> results) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getSweepBalanceMessage(results))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @Retryable
    @SneakyThrows
    public void updMenuToAdminMenu(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(adminLabels.manage())
                .replyMarkup(getAdminMenuReplyMarkup())
                .build();
        tgClient.execute(message);
    }

    private InlineKeyboardMarkup getEnergyProvidersReplyMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("itrx.io")
                                        .callbackData(InlineMenuCallbacks.MANAGE_ENERGY_PROVIDER_CHOOSE_ITRX)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text("catfee.io")
                                        .callbackData(InlineMenuCallbacks.MANAGE_ENERGY_PROVIDER_CHOOSE_CATFEE)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private InlineKeyboardMarkup getAdminMenuReplyMarkup() {
        return InlineKeyboardMarkup
                .builder()
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuManageUsers())
                                        .callbackData(InlineMenuCallbacks.MANAGE_USERS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuManageGroups())
                                        .callbackData(InlineMenuCallbacks.MANAGE_GROUPS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuItrxBalance())
                                        .callbackData(InlineMenuCallbacks.MANAGE_ITRX_BALANCE)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuSweepStats())
                                        .callbackData(InlineMenuCallbacks.MANAGE_SWEEP_BALANCE)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuEnergyProvider())
                                        .callbackData(InlineMenuCallbacks.MANAGE_ENERGY_PROVIDER)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuAutoEnergyProvider())
                                        .callbackData(InlineMenuCallbacks.MANAGE_AUTO_ENERGY_PROVIDER)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuWithdrawSweep())
                                        .callbackData(InlineMenuCallbacks.MANAGE_WITHDRAW_TRX)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuTariffs())
                                        .callbackData(InlineMenuCallbacks.MANAGE_TARIFFS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(adminLabels.menuRefPrograms())
                                        .callbackData(InlineMenuCallbacks.MANAGE_REFERRAL_PROGRAMS)
                                        .build()))
                .keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build()))
                .build();
    }

    private String getItrxBalanceMessage(ApiUsageResponse apiUsageResponse) {
        return adminLabels.itrxStats(
                FormattingTools.formatBalance(apiUsageResponse.getBalance()),
                FormattingTools.formatNumber(apiUsageResponse.getTotal_count()),
                FormattingTools.formatNumber(apiUsageResponse.getTotal_sum_energy()),
                FormattingTools.formatBalance(apiUsageResponse.getTotal_sum_trx()));
    }

    private String getSweepBalanceMessage(Map<CollectionWallet, Long> results) {
        return adminLabels.sweepStats(
                results.entrySet().stream()
                        .map(kv -> adminLabels.sweepStatsItem(kv.getKey().getWalletAddress(), FormattingTools.formatBalance(kv.getValue())))
                        .collect(Collectors.joining("\n\n")));
    }

    private InlineKeyboardMarkup getTransactionsMenuMarkup(List<UserWallet> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(wallet -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(WalletTools.formatTronAddressAndLabel(wallet.getAddress(), wallet.getLabel()))
                            .callbackData(wallet.getAddress())
                            .build());
            return row;
        }).toList();
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        walletRows.forEach(builder::keyboardRow);

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
}
