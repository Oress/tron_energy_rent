package org.ipan.nrgyrent.telegram.handlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.ipan.nrgyrent.bybit.BybitRestClient;
import org.ipan.nrgyrent.bybit.dto.WithdrawResponse;
import org.ipan.nrgyrent.domain.model.CollectionWallet;
import org.ipan.nrgyrent.domain.model.UserRole;
import org.ipan.nrgyrent.domain.model.repository.CollectionWalletRepo;
import org.ipan.nrgyrent.telegram.InlineMenuCallbacks;
import org.ipan.nrgyrent.telegram.States;
import org.ipan.nrgyrent.telegram.mapdb.TelegramStateInMem;
import org.ipan.nrgyrent.telegram.mapdb.UserStateInMem;
import org.ipan.nrgyrent.telegram.state.BybitWithdrawParams;
import org.ipan.nrgyrent.telegram.state.UserState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchState;
import org.ipan.nrgyrent.telegram.statetransitions.MatchStates;
import org.ipan.nrgyrent.telegram.statetransitions.StateHandlerRegistry;
import org.ipan.nrgyrent.telegram.statetransitions.UpdateType;
import org.ipan.nrgyrent.telegram.views.AdminViews;
import org.ipan.nrgyrent.telegram.views.BybitWithdrawViews;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

class BybitWithdrawalHandlerTest {
    private static final long USER_ID = 42L;
    private static final String ADDRESS = "TEUbZqhE3LkzsaubZzVLGDvYuYmvgCGXUk";
    private final CollectionWalletRepo wallets = mock(CollectionWalletRepo.class);
    private final BybitRestClient bybit = mock(BybitRestClient.class);
    private final BybitWithdrawViews views = mock(BybitWithdrawViews.class);
    private final AdminViews adminViews = mock(AdminViews.class);
    private DB db;
    private TelegramStateInMem state;
    private BybitWithdrawalHandler handler;
    private CollectionWallet wallet;

    @BeforeEach
    void setUp() {
        db = DBMaker.memoryDB().make();
        state = new TelegramStateInMem(db, Jackson2ObjectMapperBuilder.json().build());
        state.updateUserState(USER_ID, UserStateInMem.builder().telegramId(USER_ID).chatId(USER_ID)
                .menuMessageId(10).role(UserRole.ADMIN).state(States.ADMIN_MENU).build());
        wallet = new CollectionWallet();
        wallet.setId(7L);
        wallet.setWalletAddress(ADDRESS);
        when(wallets.findFirstByIsActive(true)).thenReturn(wallet);
        when(wallets.findById(7L)).thenReturn(Optional.of(wallet));
        handler = new BybitWithdrawalHandler(state, wallets, bybit, views, adminViews);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void customDecimalAmountIsConfirmedAndSubmittedOnlyOnce() {
        BybitWithdrawParams params = prepare(" 123,456789 ");
        assertEquals(new BigDecimal("123.456789"), params.amount());
        assertEquals(ADDRESS, params.address());
        assertTrue(params.requestId().matches("[a-f0-9]{32}"));
        verify(views).confirm(any(), eq(params));
        verifyNoInteractions(bybit);
        WithdrawResponse response = accepted();
        when(bybit.withdrawTrx(ADDRESS, params.amount(), params.requestId())).thenAnswer(invocation -> {
            assertNull(state.getBybitWithdrawParams(USER_ID));
            assertEquals(States.ADMIN_BYBIT_WITHDRAW_RESULT, user().getState());
            return response;
        });

        Update confirm = callback(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId());
        handler.confirm(user(), confirm);
        handler.confirm(user(), confirm);

        verify(bybit, times(1)).withdrawTrx(ADDRESS, params.amount(), params.requestId());
        verify(views).submitted(any(), eq(params), eq("withdrawal-123"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.000000", "-1", "1.0000001", "1e3", "NaN", "hello", "", "1,2,3", "99999999999999999999999999999999999999999.1"})
    void invalidAmountsStayAtAmountPrompt(String amount) {
        handler.promptAmount(user(), new Update());
        handler.handleAmount(user(), message(amount));

        assertEquals(States.ADMIN_BYBIT_WITHDRAW_AMOUNT, user().getState());
        assertNull(state.getBybitWithdrawParams(USER_ID).amount());
        verify(views).promptAmount(any(), eq(ADDRESS), eq(true));
        verifyNoInteractions(bybit);
    }

    @Test
    void nonTextInputDoesNotSubmit() {
        handler.promptAmount(user(), new Update());
        handler.handleAmount(user(), message(null));
        assertEquals(States.ADMIN_BYBIT_WITHDRAW_AMOUNT, user().getState());
        verifyNoInteractions(bybit);
    }

    @Test
    void missingWalletStopsFlow() {
        when(wallets.findFirstByIsActive(true)).thenReturn(null);
        handler.promptAmount(user(), new Update());
        assertNull(state.getBybitWithdrawParams(USER_ID));
        verify(views).noWallet(any());
        verifyNoInteractions(bybit);
    }

    @Test
    void changedOrInactiveWalletCannotReceiveWithdrawal() {
        BybitWithdrawParams params = prepare("10");
        wallet.setIsActive(false);
        handler.confirm(user(), callback(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId()));
        verify(views).noWallet(any());
        verifyNoInteractions(bybit);

        wallet.setIsActive(true);
        params = prepare("10");
        wallet.setWalletAddress("TAnotherAddress");
        handler.confirm(user(), callback(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId()));
        verify(views, times(2)).noWallet(any());
        verifyNoInteractions(bybit);
    }

    @Test
    void cancelClearsRequestAndOldButtonCannotConfirmNewAmount() {
        BybitWithdrawParams old = prepare("10");
        handler.back(user(), callback(InlineMenuCallbacks.CONFIRM_NO));
        assertNull(state.getBybitWithdrawParams(USER_ID));
        assertEquals(States.ADMIN_MENU, user().getState());
        BybitWithdrawParams current = prepare("25");
        handler.confirm(user(), callback(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + old.requestId()));
        assertEquals(current, state.getBybitWithdrawParams(USER_ID));
        verifyNoInteractions(bybit);
    }

    @Test
    void mainMenuClearsPendingRequest() {
        prepare("10");
        handler.clear(user(), callback(InlineMenuCallbacks.TO_MAIN_MENU));
        assertNull(state.getBybitWithdrawParams(USER_ID));
        verifyNoInteractions(bybit);
    }

    @Test
    void rejectionIsShownWithoutReportingSuccess() {
        BybitWithdrawParams params = prepare("10");
        WithdrawResponse rejected = new WithdrawResponse();
        rejected.setRetCode(131001);
        rejected.setRetMsg("Insufficient balance");
        when(bybit.withdrawTrx(anyString(), any(), anyString())).thenReturn(rejected);
        handler.confirm(user(), callback(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId()));
        verify(views).rejected(any(), eq(131001), eq("Insufficient balance"));
        verify(views, never()).submitted(any(), any(), anyString());
    }

    @Test
    void lostResponseIsReportedAsUnknownAndCannotBeResubmitted() {
        BybitWithdrawParams params = prepare("10");
        when(bybit.withdrawTrx(anyString(), any(), anyString())).thenThrow(new RuntimeException("timeout"));
        Update confirm = callback(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId());
        handler.confirm(user(), confirm);
        handler.confirm(user(), confirm);
        verify(views).unknown(any(), eq(params.requestId()));
        verify(bybit, times(1)).withdrawTrx(anyString(), any(), anyString());
        verify(views, never()).submitted(any(), any(), anyString());
    }

    @Test
    void malformedResponseIsNotSuccess() {
        BybitWithdrawParams params = prepare("10");
        WithdrawResponse response = new WithdrawResponse();
        response.setRetCode(0);
        when(bybit.withdrawTrx(anyString(), any(), anyString())).thenReturn(response);
        handler.confirm(user(), callback(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId()));
        verify(views).unknown(any(), eq(params.requestId()));
        verify(views, never()).submitted(any(), any(), anyString());
    }

    @Test
    void telegramFailureAfterAcceptanceDoesNotRetryOrReportApiFailure() {
        BybitWithdrawParams params = prepare("10");
        when(bybit.withdrawTrx(anyString(), any(), anyString())).thenReturn(accepted());
        doThrow(new RuntimeException("Telegram unavailable")).when(views).submitted(any(), any(), anyString());
        Update confirm = callback(InlineMenuCallbacks.BYBIT_WITHDRAW_CONFIRM + params.requestId());
        assertThrows(RuntimeException.class, () -> handler.confirm(user(), confirm));
        handler.confirm(user(), confirm);
        verify(bybit, times(1)).withdrawTrx(anyString(), any(), anyString());
        verify(views, never()).unknown(any(), anyString());
    }

    @Test
    void allRegisteredTransitionsRequireAdminRole() {
        StateHandlerRegistry registry = new StateHandlerRegistry();
        for (var method : BybitWithdrawalHandler.class.getDeclaredMethods()) {
            MatchState single = method.getAnnotation(MatchState.class);
            MatchStates multiple = method.getAnnotation(MatchStates.class);
            var annotations = single != null ? new MatchState[]{single}
                    : multiple != null ? multiple.value() : new MatchState[0];
            for (MatchState annotation : annotations) {
                assertTrue(annotation.forAdmin(), method.getName());
                registry.register(annotation, method, handler);
            }
        }
        UserState regular = user().withRole(UserRole.USER);
        for (States step : new States[]{States.ADMIN_MENU, States.ADMIN_BYBIT_WITHDRAW_AMOUNT,
                States.ADMIN_BYBIT_WITHDRAW_CONFIRM, States.ADMIN_BYBIT_WITHDRAW_RESULT}) {
            for (var matcher : registry.getHandlers(step, UpdateType.CALLBACK_QUERY)) {
                assertFalse(matcher.matches(regular, callback(InlineMenuCallbacks.MANAGE_BYBIT_WITHDRAW)));
            }
            for (var matcher : registry.getHandlers(step, UpdateType.MESSAGE)) {
                assertFalse(matcher.matches(regular, message("10")));
            }
        }
    }

    private BybitWithdrawParams prepare(String amount) {
        handler.promptAmount(user(), new Update());
        handler.handleAmount(user(), message(amount));
        return state.getBybitWithdrawParams(USER_ID);
    }

    private UserState user() {
        return state.getOrCreateUserState(USER_ID);
    }

    private Update message(String text) {
        Message message = new Message();
        message.setText(text);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update callback(String data) {
        CallbackQuery query = new CallbackQuery();
        query.setData(data);
        Update update = new Update();
        update.setCallbackQuery(query);
        return update;
    }

    private WithdrawResponse accepted() {
        WithdrawResponse response = new WithdrawResponse();
        response.setRetCode(0);
        WithdrawResponse.Result result = new WithdrawResponse.Result();
        result.setId("withdrawal-123");
        response.setResult(result);
        return response;
    }
}
