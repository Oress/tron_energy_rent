package org.ipan.nrgyrent.catfee.dto;

import lombok.Data;

@Data
public class CfPlaceOrderResponse {
    String id; // "667878c2-dfaa-48c9-9bfe-8a7fc61b746d"
    String resource_type; // "ENERGY"
    String billing_type;
    String source_type;
    Long pay_timestamp;
    String receiver;
    Long pay_amount_sun;
    Integer quantity; // 65000
    String duration;
    String status; // "PAYMENT_SUCCESS"
    String activate_status; // "ALREADY_ACTIVATED"
    String confirm_status;
    Long balance;
}