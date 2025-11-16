package org.ipan.nrgyrent.telegram.views;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.DepositTransaction;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@AllArgsConstructor
public class DepositTransactionsSearchView {
    private final TelegramClient tgClient;
    private final CommonLabels commonLabels;

    private String formatTransactionEntry(DepositTransaction tx) {
        StringBuilder entry = new StringBuilder();
        if (tx.getBybitUsdtTx() != null) {
            entry.append(String.format("USDT: %s, ", FormattingTools.formatUsdt(tx.getOriginalAmount())));
        }
        entry.append(String.format("TRX: %s, ", FormattingTools.formatBalance(tx.getAmount())));
        entry.append(String.format("TX ID: %s, ", tx.getTxId()));
        entry.append(String.format("%s", FormattingTools.formatDateToUtc(Instant.ofEpochMilli(tx.getTimestamp()))));
        return entry.toString();
    }

    public void updMenuToSearchResult(Page<DepositTransaction> page, UserState userState) {
        String text;
        if (page.isEmpty()) {
            text = commonLabels.searchNoResults();
        } else {
            StringBuilder content = new StringBuilder(commonLabels.searchResults());
            content.append("\n\n");
            page.getContent().forEach(tx ->
                    content.append(formatTransactionEntry(tx)).append("\n"));
            text = content.toString();
        }

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(text)
                .replyMarkup(getTariffSearchPageMarkup(page))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updMenuToSearchResult userstate {}", userState, e);
        }
    }


    private InlineKeyboardMarkup getTariffSearchPageMarkup(Page<DepositTransaction> page) {
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup
                .builder();
        boolean hasPrev = page.hasPrevious();
        boolean hasNext = page.hasNext();

        if (hasPrev || hasNext) {
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            if (hasPrev) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(commonLabels.searchPrevPage())
                                .callbackData(InlineMenuCallbacks.DEPOSIT_PREV_PAGE)
                                .build());
            }
            if (hasNext) {
                buttons.add(InlineKeyboardButton
                                .builder()
                                .text(commonLabels.searchNextPage())
                                .callbackData(InlineMenuCallbacks.DEPOSIT_NEXT_PAGE)
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
}
