package org.ipan.nrgyrent.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class Balance {
    private BigDecimal amountTrx;
}
