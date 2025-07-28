package org.ipan.nrgyrent.tron.node.events.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Data
@Slf4j
public class SmartContractEvent {
    private Long timeStamp;
    private String triggerName;
    private String uniqueId;
    private String transactionId;
    private String contractAddress;
//    private String callerAddress;
    private String originAddress;
//    private String creatorAddress;
    private Long blockNumber;
    private String blockHash;
    private Boolean removed;
//    private Long latestSolidifiedBlockNumber;
//    private Object logInfo;
//    private RawDataDto rawData;
//    private Object abi;
    private String eventSignature;
//    private String eventSignatureFull;
    private String eventName;
    private Map<String, Object> topicMap;
    private Map<String, Object> dataMap;

    public String getToAddress() {
        if (topicMap != null && topicMap.containsKey("to")) {
            return topicMap.get("to").toString();
        }
        return null;
    }

    public Long getAssetAmount() {
        Long result = null;
        if (dataMap != null && dataMap.containsKey("value")) {
            try {
                result = Long.parseLong(dataMap.get("value").toString());
            } catch (NumberFormatException e) {
                logger.error("Cannot parse USDT amount from dataMap: {}", dataMap.get("value"), e);
            }
        }
        return result;
    }
}
