package org.ipan.nrgyrent.catfee.dto;

import lombok.Data;

@Data
public class CfResponse<T> {
    private Integer code;
    private T data;
    private String msg;
}
