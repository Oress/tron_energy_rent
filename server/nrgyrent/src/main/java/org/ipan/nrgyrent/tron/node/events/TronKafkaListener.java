package org.ipan.nrgyrent.tron.node.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.application.service.AutoAmlService;
import org.ipan.nrgyrent.application.service.DepositService;
import org.ipan.nrgyrent.telegram.state.TelegramState;
import org.ipan.nrgyrent.tron.node.events.dto.AddressTransactionEvent;
import org.ipan.nrgyrent.tron.node.events.dto.SmartContractEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Slf4j
@AllArgsConstructor
public class TronKafkaListener {
    private final TelegramState telegramState;
    private final AddressesWatchlist addressesWatchlist;
    private final AutoAmlWatchlist autoAmlWatchlist;
    private final ObjectMapper objectMapper;
    private final DepositService depositService;
    private final AutoAmlService autoAmlService;

    @KafkaListener(id = "tg_bot_app_transaction", topics = "transaction")
    public void listenForTransactions(String in) {
        try {
            AddressTransactionEvent value = objectMapper.readValue(in, AddressTransactionEvent.class);
            if (value != null && shouldEnqueueTxMessage(value)) {
                logger.info("Processing TRX transaction {}", value);
                depositService.processTxEventAsync(value);
            }
        } catch (JsonProcessingException e) {
            logger.error("Error during processing transaction from Kafka {}", in);
        }
    }

    @KafkaListener(id = "tg_bot_app_contractevent", topics = "contractevent")
    public void listenForContractevent(String in) {
        try {
            SmartContractEvent value = objectMapper.readValue(in, SmartContractEvent.class);
            if (value != null) {
                if (shouldEnqueueSmartContractEvent(value)) {
                    logger.info("Processing USDT transaction {}", value);
                    depositService.processUsdtEventAsync(value);
                }
                if (isUsdtTransferForAmlEvent(value)) {
                    checkAutoAmlTrigger(value);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Error during processing transaction from Kafka {}", in);
        }
    }

    private boolean isUsdtTransferForAmlEvent(SmartContractEvent value) {
        if ("Transfer".equals(value.getEventName()) && "Transfer(address,address,uint256)".equals(value.getEventSignature())) {
            String toAddress = value.getToAddress();
            if (autoAmlWatchlist.contains(toAddress)) {
                return true;
            }
        }
        return false;
    }

    private void checkAutoAmlTrigger(SmartContractEvent event) {
        String toAddress = event.getToAddress();
        Collection<AutoAmlWatchlist.SessionInfo> sessionInfos = autoAmlWatchlist.getSessions(toAddress);
        if (sessionInfos.isEmpty()) {
            return;
        }

        Long amount = event.getAssetAmount();
        if (amount == null) {
            return;
        }

        String fromAddress = event.getFromAddress();
        if (fromAddress == null) {
            return;
        }

        for (AutoAmlWatchlist.SessionInfo session : sessionInfos) {
            if (amount < session.getThresholdSun()) {
                continue;
            }
            logger.info("Auto-AML trigger: toAddress={} fromAddress={} amount={} threshold={} user={}",
                    toAddress, fromAddress, amount, session.getThresholdSun(), session.getUserId());
            autoAmlService.triggerAutoAmlCheck(session.getSessionId(), fromAddress, amount);
        }
    }

    private boolean shouldEnqueueSmartContractEvent(SmartContractEvent value) {
        if ("Transfer".equals(value.getEventName()) && "Transfer(address,address,uint256)".equals(value.getEventSignature())) {
//            String fromAddress = value.getFromAddress();
            String toAddress = value.getToAddress();
            if (addressesWatchlist.contains(toAddress)) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldEnqueueTxMessage(AddressTransactionEvent value) {
        if (ContractTypes.TRANSFER.equals(value.getContractType())) {
//            String fromAddress = value.getFromAddress();
            String toAddress = value.getToAddress();
            if (addressesWatchlist.contains(toAddress)) {
                return true;
            }
        }

        return false;
    }

}