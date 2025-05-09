package org.ipan.nrgyrent.itrx.dto;

import lombok.Getter;

@Getter
public class OrderCallbackRequest {
    public String active_hash; // If activated, activated txid
    public String bandwidth_hash; // If there is bandwidth, the txid of the bandwidth
    public Integer energy_amount; // Order energy
    public String out_trade_no; // External order number
    public Double pay_amount; // Actual commission energy
    public String serial; // Internal order number
    public String txid; // txid of energy
    public Integer status; // Status 40 is successful, 41 is failed
    public String type; // type
    public String receive_address; // receive address
    public String source; // order source
}
