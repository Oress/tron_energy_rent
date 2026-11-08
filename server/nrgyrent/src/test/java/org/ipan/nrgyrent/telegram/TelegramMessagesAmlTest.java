package org.ipan.nrgyrent.telegram;

import org.ipan.nrgyrent.domain.model.AmlProvider;
import org.ipan.nrgyrent.domain.model.AmlVerification;
import org.ipan.nrgyrent.domain.model.AmlVerificationStatus;
import org.ipan.nrgyrent.telegram.i18n.CommonLabels;
import org.ipan.nrgyrent.telegram.mapdb.UserStateInMem;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.utils.FormattingTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramMessagesAmlTest {
    private static final String REPORT = "AML report";
    private static final String NOT_MODIFIED = "Bad Request: message is not modified: specified new message content "
            + "and reply markup are exactly the same as a current content and reply markup of the message";

    @Mock
    private TelegramClient tgClient;
    @Mock
    private FormattingTools formattingTools;
    @Mock
    private CommonLabels commonLabels;

    private TelegramMessages messages;
    private AmlVerification verification;
    private final UserState userState = UserStateInMem.builder()
            .telegramId(99L).chatId(999L).languageCode("en").build();

    @BeforeEach
    void setUp() {
        messages = new TelegramMessages(tgClient, null, null, commonLabels, null, formattingTools, null);
        verification = new AmlVerification();
        verification.setId(42L);
        verification.setChatId(123L);
        verification.setMessageToUpdate(456);
        verification.setProvider(AmlProvider.BITOK);
        verification.setWalletAddress("wallet-address");
    }

    @ParameterizedTest
    @EnumSource(value = AmlVerificationStatus.class, names = {"COMPLETED", "FAILED"})
    void editsTheOriginalReportMessage(AmlVerificationStatus status) throws TelegramApiException {
        givenReport(status);

        sendReport(status);

        ArgumentCaptor<EditMessageText> edit = ArgumentCaptor.forClass(EditMessageText.class);
        verify(tgClient).execute(edit.capture());
        assertEquals("123", edit.getValue().getChatId());
        assertEquals(456, edit.getValue().getMessageId());
        assertEquals(REPORT, edit.getValue().getText());
        assertEquals("MARKDOWN", edit.getValue().getParseMode());
        verifyNoMoreInteractions(tgClient);
    }

    @ParameterizedTest
    @EnumSource(value = AmlVerificationStatus.class, names = {"COMPLETED", "FAILED"})
    void repeatedIdenticalReportIsSuccessfulWithoutSendingAnotherMessage(AmlVerificationStatus status)
            throws TelegramApiException {
        givenReport(status);
        when(tgClient.execute(any(EditMessageText.class)))
                .thenReturn(new Message())
                .thenThrow(apiError(400, NOT_MODIFIED));

        assertDoesNotThrow(() -> sendReport(status));
        assertDoesNotThrow(() -> sendReport(status));

        verify(tgClient, times(2)).execute(any(EditMessageText.class));
        verifyNoMoreInteractions(tgClient);
    }

    @ParameterizedTest
    @MethodSource("otherApiErrors")
    void propagatesOtherApiErrors(AmlVerificationStatus status, Integer code, String description)
            throws TelegramApiException {
        givenReport(status);
        TelegramApiRequestException error = apiError(code, description);
        when(tgClient.execute(any(EditMessageText.class))).thenThrow(error);

        assertSame(error, assertThrows(TelegramApiRequestException.class, () -> sendReport(status)));

        verify(tgClient).execute(any(EditMessageText.class));
        verifyNoMoreInteractions(tgClient);
    }

    @ParameterizedTest
    @EnumSource(value = AmlVerificationStatus.class, names = {"COMPLETED", "FAILED"})
    void propagatesTransportErrors(AmlVerificationStatus status) throws TelegramApiException {
        givenReport(status);
        TelegramApiException error = new TelegramApiException("Connection timed out");
        when(tgClient.execute(any(EditMessageText.class))).thenThrow(error);

        assertSame(error, assertThrows(TelegramApiException.class, () -> sendReport(status)));

        verify(tgClient).execute(any(EditMessageText.class));
        verifyNoMoreInteractions(tgClient);
    }

    @ParameterizedTest
    @EnumSource(value = AmlVerificationStatus.class, names = {"COMPLETED", "FAILED"})
    void sendsNewMessageWhenThereIsNoMessageToEdit(AmlVerificationStatus status) throws TelegramApiException {
        givenReport(status);
        verification.setMessageToUpdate(null);

        sendReport(status);

        ArgumentCaptor<SendMessage> sent = ArgumentCaptor.forClass(SendMessage.class);
        verify(tgClient).execute(sent.capture());
        assertEquals("999", sent.getValue().getChatId());
        assertEquals(REPORT, sent.getValue().getText());
        assertEquals("MARKDOWN", sent.getValue().getParseMode());
        verifyNoMoreInteractions(tgClient);
    }

    private void givenReport(AmlVerificationStatus status) {
        if (status == AmlVerificationStatus.COMPLETED) {
            when(formattingTools.formatAmlReport(verification, Locale.ENGLISH)).thenReturn(REPORT);
        } else {
            when(commonLabels.amlReportFailed(Locale.ENGLISH, verification.getWalletAddress(),
                    verification.getProvider().getDisplayName())).thenReturn(REPORT);
        }
    }

    private void sendReport(AmlVerificationStatus status) {
        if (status == AmlVerificationStatus.COMPLETED) {
            messages.sendAmlReportCompleted(userState, verification);
        } else {
            messages.sendAmlReportFailed(userState, verification);
        }
    }

    private static Stream<Arguments> otherApiErrors() {
        return Stream.of(AmlVerificationStatus.COMPLETED, AmlVerificationStatus.FAILED)
                .flatMap(status -> Stream.of(
                        Arguments.of(status, 400, "Bad Request: message to edit not found"),
                        Arguments.of(status, 400, "Bad Request: can't parse entities"),
                        Arguments.of(status, 403, "Forbidden: bot was blocked by the user"),
                        Arguments.of(status, 500, NOT_MODIFIED),
                        Arguments.of(status, 400, null),
                        Arguments.of(status, null, NOT_MODIFIED)));
    }

    private static TelegramApiRequestException apiError(Integer code, String description) {
        return new TelegramApiRequestException("Error executing EditMessageText query", ApiResponse.builder()
                .ok(false).errorCode(code).errorDescription(description).build());
    }
}
