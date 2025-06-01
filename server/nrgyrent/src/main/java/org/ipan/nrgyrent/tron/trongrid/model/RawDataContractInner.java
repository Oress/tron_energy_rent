package org.ipan.nrgyrent.tron.trongrid.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class RawDataContractInner {
  private String type;
  private RawDataContractInnerParameter parameter;
}
