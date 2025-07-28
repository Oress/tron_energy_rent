package org.ipan.nrgyrent.tron.node.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.FullnodeConfig;
import org.ipan.nrgyrent.application.service.DepositService;
import org.ipan.nrgyrent.application.service.EnergyService;
import org.ipan.nrgyrent.domain.events.autotopup.AutoDelegationSessionEventPublisher;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.telegram.state.WalletMonitoringState;
import org.ipan.nrgyrent.tron.node.events.dto.AddressTransactionEvent;
import org.springframework.stereotype.Component;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import zmq.Msg;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


//@Component
// ZeroMq plugin does not some types of events.
/*
@Slf4j
@AllArgsConstructor
public class TronZeroMqListener {
    public static final AtomicBoolean isConnected = new AtomicBoolean(false);

    private final BlockingQueue<AddressTransactionEvent> queue = new LinkedBlockingQueue<>(10000);
    private final TelegramState telegramState;
    private final AddressesWatchlist addressesWatchlist;
    private final ObjectMapper objectMapper;
    private final EnergyService energyService;
    private final DepositService depositService;
    private final FullnodeConfig fullnodeConfig;
    private final AutoDelegationSessionEventPublisher autoDelegationSessionEventPublisher;
    private final ExecutorService pollingTp = Executors.newFixedThreadPool(1);
    private final ExecutorService messageProcessorTp = Executors.newFixedThreadPool(1);

    @PostConstruct
    public void afterPropsSet() {
        startPolling();
        startProcessing();
    }

    private void startPolling() {
        pollingTp.submit(() -> {
            try {
                ZMQ.Context context = ZMQ.context(1);
                ZMQ.Socket subscriber = context.socket(SocketType.SUB);
                subscriber.subscribe("transaction");
                subscriber.setHeartbeatTimeout(10 * 1000);
                subscriber.setReceiveTimeOut(60 * 1000);
                subscriber.connect(fullnodeConfig.getZeroMqUrl());

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        byte[] payload = subscriber.recv(0);

                        sendEventIfRequired(payload);
                        isConnected.set(payload != null);
                        if (payload == null) {
                            continue;
                        }

                        // Check for transactionTrigger
                        logger.info("msg {}", new String(payload));
                        if (checkForTransactionTrigger(payload)) continue;

                        AddressTransactionEvent value = objectMapper.readValue(payload, AddressTransactionEvent.class);
                        if (shouldEnqueueMessage(value)) {
                            queue.offer(value);
                        }
                    } catch (Exception e) {
                        isConnected.set(false);
                        logger.error("AUTODELEGATION. Error when polling for new messages", e);
                    }
                }
                subscriber.close();
                context.term();
            } catch (Exception e) {
                isConnected.set(false);
                e.printStackTrace();
            }
        });
    }

    private void sendEventIfRequired(byte[] payload) {
        // was connected && disc or vice versa
        if (isConnected.get() && payload == null) {
            autoDelegationSessionEventPublisher.publishNodeDisconnectedEvent();
        } else if (!isConnected.get() && payload != null) {
            autoDelegationSessionEventPublisher.publishNodeReconnectEvent();
        }
    }

    private void startProcessing() {
        messageProcessorTp.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    AddressTransactionEvent event = queue.poll(10, TimeUnit.SECONDS);
                    if (event != null) {
                        depositService.processTxEventAsync(event);
                    }
                } catch (InterruptedException e) {
                    logger.error("Exception when processing message", e);
                }
            }
        });
    }

    private boolean shouldEnqueueMessage(AddressTransactionEvent value) {
        if (ContractTypes.TRIGGER_SC.equals(value.getContractType())) {
            String fromAddress = value.getFromAddress();
            if (fromAddress != null && !fromAddress.isBlank()) {
                 WalletMonitoringState walletMonitoringState = telegramState.getWalletMonitoringState(fromAddress);
                 return walletMonitoringState != null;
            }
        }

        if (ContractTypes.TRANSFER.equals(value.getContractType())) {
            String fromAddress = value.getFromAddress();
            String toAddress = value.getToAddress();
            if (addressesWatchlist.contains(fromAddress) || addressesWatchlist.contains(toAddress)) {
                return true;
            }
        }

*/
/*
        if (ContractTypes.UNDELEGATE_RESOURCE.equals(value.getContractType())) {
            String toAddress = value.getToAddress();
            if (toAddress != null && !toAddress.isBlank()) {
                 WalletMonitoringState walletMonitoringState = telegramState.getWalletMonitoringState(toAddress);
                 return walletMonitoringState != null;
            }
        }
*//*

        return false;
    }

    private static final byte[] transactionTrigger = {116, 114, 97, 110, 115, 97, 99, 116, 105, 111, 110, 84, 114, 105, 103, 103, 101, 114};

    private boolean checkForTransactionTrigger(byte[] bytes) {
        return Arrays.compare(transactionTrigger, bytes) == 0;
    }
}*/
