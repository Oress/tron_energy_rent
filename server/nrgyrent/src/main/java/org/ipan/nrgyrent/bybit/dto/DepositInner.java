package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

import java.util.List;

@Data
public class DepositInner {
    private List<DepositData> rows;
}
