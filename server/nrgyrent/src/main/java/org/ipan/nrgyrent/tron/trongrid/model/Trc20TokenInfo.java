package org.ipan.nrgyrent.tron.trongrid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class Trc20TokenInfo {
  private String symbol;
  private String address;
  private Integer decimals;
  private String name;
}
