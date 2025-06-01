package org.ipan.nrgyrent.tron.trongrid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ContractParameterValue {
  private Long frozen_balance;
  private Long balance;
  private Long lock_period;
  private Boolean lock;
  private Long unfreeze_balance;
  private Long frozen_duration;
  private Long amount;
  private String owner_address;
  private String receiver_address;
  private String account_address;
  private String account_name;
  private String asset_name;
  private String account_id;
  private String to_address;
}

