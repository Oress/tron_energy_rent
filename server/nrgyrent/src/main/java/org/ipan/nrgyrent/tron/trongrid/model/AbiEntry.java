package org.ipan.nrgyrent.tron.trongrid.model;

import lombok.Data;

import java.util.List;

@Data
public class AbiEntry {
    private Boolean constant;
    private String name;
    private String stateMutability;
    private String type;
    private List<AbiParam> inputs;
    private List<AbiParam> outputs;
}

