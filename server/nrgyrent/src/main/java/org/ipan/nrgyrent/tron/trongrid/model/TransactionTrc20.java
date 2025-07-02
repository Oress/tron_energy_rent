package org.ipan.nrgyrent.tron.trongrid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@ToString
@Getter
@Setter
public class TransactionTrc20 {
  private String transaction_id;
  private Trc20TokenInfo token_info;
  private Long block_timestamp;
  private String from;
  private String to;
  private String type;
  private String value;
}


