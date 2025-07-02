package org.ipan.nrgyrent.tron.trongrid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
@Setter
public class TransactionsTrc20Response {
  private List<TransactionTrc20> data = new ArrayList<>();
  private Boolean success;
  // private V1AccountsAddressGet200ResponseMeta meta;
}

