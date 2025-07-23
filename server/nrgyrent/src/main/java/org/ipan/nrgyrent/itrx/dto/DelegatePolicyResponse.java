package org.ipan.nrgyrent.itrx.dto;

import lombok.Data;

@Data
public class DelegatePolicyResponse {
    private Long id;
    private String receive_address;
    private Integer status;
    private Integer last_step;
    private Boolean main_delegated;

    private String expired_time;
    private String create_time;
    private String update_time;

    private String last_step_display;
    private String status_display;
    private Integer auto_type;
    private String auto_type_display;
    private Integer unused_times;
    private Integer max_energy;
    private Integer period;
    private Integer count_limit;
    private Integer count_limit_dynamic;
    private Boolean is_always;
    private Integer unused_times_threshold;
    private Boolean count_bandwidth_limit;
    private Boolean pause;
}