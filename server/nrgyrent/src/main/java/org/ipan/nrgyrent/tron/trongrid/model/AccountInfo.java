package org.ipan.nrgyrent.tron.trongrid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class AccountInfo {
  private String address;
  private Long balance;
  private Long create_time;
}

