package org.ipan.nrgyrent.domain.model.projections;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TopupHistoryDto {
    private String type; // TRX or USDT
    private Long amount; // TRX amount in sun
    private Long originalAmount; // USDT amount in usdt minor units if type=USDT
    private Timestamp createdAt; // UTC
}
