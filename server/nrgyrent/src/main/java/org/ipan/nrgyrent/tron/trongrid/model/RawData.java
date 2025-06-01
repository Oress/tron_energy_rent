package org.ipan.nrgyrent.tron.trongrid.model;

import java.util.ArrayList;
import java.util.List;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class RawData {
  private String ref_block_bytes;
  private String ref_block_hash;
  private Long expiration;
  private String data;
  private Long timestamp;
  private Long fee_limit;
  private List<Contract> contract = new ArrayList<>();
}

