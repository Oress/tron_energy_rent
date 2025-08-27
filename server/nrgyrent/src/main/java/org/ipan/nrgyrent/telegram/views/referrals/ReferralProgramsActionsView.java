package org.ipan.nrgyrent.telegram.views.referrals;

import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.domain.model.ReferralProgramCalcType;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.i18n.RefProgramLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.views.CommonViews;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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
public class ReferralProgramsActionsView {
    private final TelegramClient tgClient;
    private final CommonViews commonViews;
    private final CommonLabels commonLabels;
    private final RefProgramLabels refProgramLabels;

    @SneakyThrows
    public void updMenuToManageRefProgramActionsMenu(UserState userState, ReferralProgram refProgram) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(getBalanceDescription(refProgram))
                .replyMarkup(getManageRefProgramActionsMarkup(refProgram))
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void renameSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(refProgramLabels.actionsRenameSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public void changeTxAmountSuccess(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(refProgramLabels.actionsChangePercentageSuccess())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }

    public void nameIsTooShort(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(refProgramLabels.actionsRenameLabelTooShort())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not tariffNameIsTooShort userstate {}", userState, e);
        }
    }

    @SneakyThrows
    public void promptNewLabel(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(refProgramLabels.actionsRenamePromptLabel())
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        tgClient.execute(message);
    }



    public InlineKeyboardMarkup confirmDeleteTariffMarkup() {
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

    private String getBalanceDescription(ReferralProgram refProgram) {
        return refProgramLabels.actionsPreview(
                refProgram.getLabel(),
                refProgram.getPercentage(),
                getCalcTypeLabel(refProgram.getCalcType()),
                refProgram.getPredefined() ? commonLabels.yes() : commonLabels.no(),
                FormattingTools.formatDateToUtc(refProgram.getCreatedAt()),
                        FormattingTools.formatBalance3(refProgram.getSubtractAmountTx1Itrx()),
                        FormattingTools.formatBalance3(refProgram.getSubtractAmountTx2Itrx()),
                        FormattingTools.formatBalance3(refProgram.getSubtractAmountTx1AutoItrx()),
                        FormattingTools.formatBalance3(refProgram.getSubtractAmountTx2AutoItrx())
        );
    }

    private String getCalcTypeLabel(ReferralProgramCalcType calcType) {
        return switch(calcType) {
                case ReferralProgramCalcType.PERCENT_FROM_PROFIT -> refProgramLabels.percent_from_profit();
                case ReferralProgramCalcType.PERCENT_FROM_REVENUE -> refProgramLabels.percent_from_revenue();
                default -> "-";
        };
    }

    private InlineKeyboardMarkup getManageRefProgramActionsMarkup(ReferralProgram refProgram) {
        boolean canChange = !refProgram.getPredefined();
        boolean sebesState = refProgram.getSubtractAmountUseProviderAmount();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder = InlineKeyboardMarkup.builder();
        if (canChange) {
            builder.keyboardRow(
                    new InlineKeyboardRow(
                            InlineKeyboardButton
                                    .builder()
                                    .text(refProgramLabels.actionsRename())
                                    .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_RENAME)
                                    .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(sebesState ? refProgramLabels.enableSebes() : refProgramLabels.disableSebes())
                                            .callbackData(InlineMenuCallbacks.createToggleRefProgramSebesCallback(refProgram.getId()))
                                            .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(refProgramLabels.changeTx1SubtractAmount())
                                            .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX1)
                                            .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(refProgramLabels.changeTx2SubtractAmount())
                                            .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX2)
                                            .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(refProgramLabels.changeTx1AutoSubtractAmount())
                                            .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX1_AUTO)
                                            .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(refProgramLabels.changeTx2AutoSubtractAmount())
                                            .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_BASE_TX2_AUTO)
                                            .build()))
                    .keyboardRow(
                            new InlineKeyboardRow(
                                    InlineKeyboardButton
                                            .builder()
                                            .text(refProgramLabels.actionsChangePercentage())
                                            .callbackData(InlineMenuCallbacks.MANAGE_REF_PROGRAMS_ACTION_CHANGE_PERCENTAGE)
                                            .build()))
                                        ;
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
