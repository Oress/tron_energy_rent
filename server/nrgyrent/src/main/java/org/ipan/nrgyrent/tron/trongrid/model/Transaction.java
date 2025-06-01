package org.ipan.nrgyrent.tron.trongrid.model;

import java.util.ArrayList;
import java.util.List;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@ToString
@Getter
@Setter
public class Transaction {
  private String txID;
  private Integer blockNumber;
  private Long block_timestamp;
  private List<String> signature = new ArrayList<>();
  private String raw_data_hex;
  private RawData raw_data;
  private Long energy_fee;
  private Long energy_usage;
  private Long energy_usage_total;
  private Long net_fee;
  private Long net_usage;
  private List<String> internal_transactions = new ArrayList<>();
  private Boolean visible;
}

