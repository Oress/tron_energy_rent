package org.ipan.nrgyrent.bybit.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetOrderResponseInner {
    private String nextPageCursor;
    private String category;
    private List<GetOrderData> list;
}
