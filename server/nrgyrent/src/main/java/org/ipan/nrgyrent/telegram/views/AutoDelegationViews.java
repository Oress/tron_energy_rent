package org.ipan.nrgyrent.telegram.views;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationEvent;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSessionStatus;
import org.ipan.nrgyrent.domain.model.projections.WalletWithAutoTopupSession;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.i18n.AutoDelegateLabels;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.ipan.nrgyrent.telegram.utils.WalletTools;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class AutoDelegationViews {
    private final TelegramClient tgClient;
    private final CommonLabels commonLabels;
    private final CommonViews commonViews;
    private final AutoDelegateLabels autoDelegateLabels;

    public void walletIsAlreadyHasSession(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("Wallet already has session")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not nodeIsUnavailableRightNow userstate {}", userState, e);
        }
    }

    public void inactiveWallet(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text("Cannot start autodelegation for inactive wallet")
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not nodeIsUnavailableRightNow userstate {}", userState, e);
        }
    }


    public void unableToStartInitProblem(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .text(autoDelegateLabels.unableToStartSessionDueToInitProblem())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not nodeIsUnavailableRightNow userstate {}", userState, e);
        }
    }

    public void nodeIsUnavailableRightNow(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .replyMarkup(commonViews.getToMainMenuMarkup())
                .text(autoDelegateLabels.unableToStartSessionDueToSystemOffline())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not nodeIsUnavailableRightNow userstate {}", userState, e);
        }
    }

    public void autoDelegateSessionCreated(UserState userState, AutoDelegationSession session) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(session.getChatId())
                .messageId(session.getMessageToUpdate())
                .text(autoDelegateLabels.sessionStartMessage(
                        userState.getLocaleOrDefault(),
                        WalletTools.formatTronAddress(session.getAddress())
                ))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updateToTopupSessionStatus userstate {}", userState, e);
        }
    }

    public void autoDelegateSessionStoppedManually(UserState userState, AutoDelegationSession session) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(autoDelegateLabels.sessionStopManually(
                        userState.getLocaleOrDefault(),
                        WalletTools.formatTronAddress(session.getAddress())
                ))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updateToTopupSessionStatus userstate {}", userState, e);
        }
    }

    public void cannotStartAutoDelegateSessionLowBalance(UserState userState) {
        EditMessageText message = EditMessageText
                .builder()
                .messageId(userState.getMenuMessageId())
                .chatId(userState.getChatId())
                .text(autoDelegateLabels.cannotStartSessionLowBalance(userState.getLocaleOrDefault()))
                .replyMarkup(commonViews.getToMainMenuAndBackMarkup())
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not cannotStartAutoDelegateSessionLowBalance userstate {}", userState, e);
        }
    }


    public void autoDelegateSessionStoppedLowBalance(UserState userState, AutoDelegationSession session) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(autoDelegateLabels.sessionStopLowBalance(
                        userState.getLocaleOrDefault(),
                        WalletTools.formatTronAddress(session.getAddress())
                ))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updateToTopupSessionStatus userstate {}", userState, e);
        }
    }

    public void autoDelegateSessionStoppedInactivity(UserState userState, AutoDelegationSession session) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(autoDelegateLabels.sessionStopInactivity(
                        userState.getLocaleOrDefault(),
                        WalletTools.formatTronAddress(session.getAddress())
                ))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updateToTopupSessionStatus userstate {}", userState, e);
        }
    }

    public void sendAutoDelegationTransactionNotification(UserState userState, Order order) {
        String label = autoDelegateLabels.transactionSuccess(
                userState.getLocaleOrDefault(),
                FormattingTools.formatBalance(order.getSunAmount()),
                WalletTools.formatTronAddress(order.getReceiveAddress()));

        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(label)
                // .replyMarkup(getToMainMenuNotificationMarkup())
                // .parseMode("MARKDOWN")
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Failed to sendTransactionSuccessNotification user: {}", userState, e);
        }
        return;
    }

    public void updateSessionStatus(UserState userState, AutoDelegationSession session) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(session.getChatId())
                .messageId(session.getMessageToUpdate())
                .text(autoDelegateLabels.sessionStatusMessage(
                        userState.getLocaleOrDefault(),
                        mapStatusToEmoji(session.getStatus()),
                        getStatusText(userState, session.getStatus()),
                        WalletTools.formatTronAddress(session.getAddress())
                ))
                .build();
        try {
            tgClient.execute(message);
        } catch (Exception e) {
            logger.error("Could not updateToTopupSessionStatus userstate {}", userState, e);
        }
    }

    private String mapStatusToEmoji(AutoDelegationSessionStatus status) {
        return switch (status){
            case AutoDelegationSessionStatus.ACTIVE -> commonLabels.greenCircle();
            case AutoDelegationSessionStatus.STOPPED_BY_USER,
                 AutoDelegationSessionStatus.STOPPED_ENERGY_UNUSED,
                 AutoDelegationSessionStatus.STOPPED_INACTIVE_WALLET,
                 AutoDelegationSessionStatus.STOPPED_SYSTEM_RESTART,
                 AutoDelegationSessionStatus.STOPPED_NODE_DISCONNECTED,
                 AutoDelegationSessionStatus.STOPPED_INSUFFICIENT_BALANCE -> commonLabels.redCircle();
            default -> "💀";
        };
    }

    private String getEventString(UserState userState, AutoDelegationEvent event) {
        String eventStr = switch (event.getType()) {
            case SESSION_STOPPED -> autoDelegateLabels.eventSessionStopped(userState.getLocaleOrDefault());
            case SC_INVOCATION -> autoDelegateLabels.eventScInvocation(userState.getLocaleOrDefault(), FormattingTools.formatBalance(event.getOrder().getSunAmount()));
            case REDELEGATE_PARTIALLY -> autoDelegateLabels.eventRedelegate(userState.getLocaleOrDefault(), FormattingTools.formatBalance(event.getOrder().getSunAmount()));
            case INIT_DELEGATION -> autoDelegateLabels.eventInitDelegate(userState.getLocaleOrDefault(), FormattingTools.formatBalance(event.getOrder().getSunAmount()));
            default -> "Unexpected event type";
        };
        String orderStatus = "";
        if (event.getOrder() != null) {
            orderStatus = switch (event.getOrder().getOrderStatus()) {
                case PENDING -> autoDelegateLabels.pending(userState.getLocaleOrDefault());
                case COMPLETED -> autoDelegateLabels.completed(userState.getLocaleOrDefault());
                case REFUNDED -> autoDelegateLabels.refunded(userState.getLocaleOrDefault());
                default -> "Unexpected order status";
            };
        }

        return "🔹 %s %s".formatted(eventStr, orderStatus);
    }

    private String getStatusText(UserState userState, AutoDelegationSessionStatus status) {
        return switch (status){
            case AutoDelegationSessionStatus.ACTIVE -> autoDelegateLabels.statusActive(userState.getLocaleOrDefault()) ;
            case AutoDelegationSessionStatus.STOPPED_BY_USER -> autoDelegateLabels.statusStoppedByUser(userState.getLocaleOrDefault()) ;
            case AutoDelegationSessionStatus.STOPPED_ENERGY_UNUSED -> autoDelegateLabels.statusEnergyUnused(userState.getLocaleOrDefault());
            case AutoDelegationSessionStatus.STOPPED_NODE_DISCONNECTED -> autoDelegateLabels.statusSystemOffline(userState.getLocaleOrDefault());
            case AutoDelegationSessionStatus.STOPPED_INSUFFICIENT_BALANCE -> autoDelegateLabels.statusInsufficientFunds(userState.getLocaleOrDefault()) ;
            case AutoDelegationSessionStatus.STOPPED_INACTIVE_WALLET -> autoDelegateLabels.statusInactiveWallet(userState.getLocaleOrDefault());
            case AutoDelegationSessionStatus.STOPPED_SYSTEM_RESTART -> autoDelegateLabels.statusSystemRestart(userState.getLocaleOrDefault());
            case AutoDelegationSessionStatus.STOPPED_ERROR -> autoDelegateLabels.statusError(userState.getLocaleOrDefault());
            default -> "UNKNOW STATUS!";
        };
    }

    @SneakyThrows
    public void autoDelegationMenu(UserState userState, List<WalletWithAutoTopupSession> walletsWithSessions) {
        EditMessageText message = EditMessageText
                .builder()
                .chatId(userState.getChatId())
                .messageId(userState.getMenuMessageId())
                .text(autoDelegateLabels.description(userState.getLocaleOrDefault()))
                .parseMode("MARKDOWN")
                .replyMarkup(getWalletsMenuMarkup(walletsWithSessions))
                .build();
        tgClient.execute(message);
    }

    @SneakyThrows
    public Message autoDelegationMenuMsg(UserState userState, List<WalletWithAutoTopupSession> walletsWithSessions) {
        SendMessage message = SendMessage
                .builder()
                .chatId(userState.getChatId())
                .text(autoDelegateLabels.description(userState.getLocaleOrDefault()))
                .parseMode("MARKDOWN")
                .replyMarkup(getWalletsMenuMarkup(walletsWithSessions))
                .build();
        return tgClient.execute(message);
    }


    private InlineKeyboardMarkup getWalletsMenuMarkup(List<WalletWithAutoTopupSession> wallets) {
        List<InlineKeyboardRow> walletRows = wallets.stream().map(walletSession -> {
            InlineKeyboardRow row = new InlineKeyboardRow(
                    InlineKeyboardButton
                            .builder()
                            .text(getWalletStatusText(walletSession))
                            .callbackData(InlineMenuCallbacks.createToggleAutoTopupCallback(walletSession.getWalletAddress(), walletSession.getActiveSessionId()))
                            .build());
            return row;
        }).toList();

        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder<?, ?> builder = InlineKeyboardMarkup.builder();
        walletRows.forEach(builder::keyboardRow);

        return builder.keyboardRow(
                        new InlineKeyboardRow(
                                InlineKeyboardButton
                                        .builder()
                                        .text(commonLabels.toMainMenu())
                                        .callbackData(InlineMenuCallbacks.TO_MAIN_MENU)
                                        .build())

                )
                .build();
    }

    private String getWalletStatusText(WalletWithAutoTopupSession walletSession) {
        String secondPart = WalletTools.formatTronAddressAndLabel(walletSession.getWalletAddress(), walletSession.getWalletLabel());
        return (walletSession.getActiveSessionId() == null
                ? commonLabels.redCircle()
                : commonLabels.greenCircle()) + " " + secondPart;
    }

}
