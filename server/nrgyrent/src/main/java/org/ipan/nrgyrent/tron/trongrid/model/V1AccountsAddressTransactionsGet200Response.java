package org.ipan.nrgyrent.tron.trongrid.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class V1AccountsAddressTransactionsGet200Response {
  private List<Transaction> data = new ArrayList<>();
  private Boolean success;
  // private V1AccountsAddressGet200ResponseMeta meta;
}

