package com.aiforest.tmiot.common.data.entity.bo;

import lombok.Data;

@Data
public class DeviceSnapshotBO {
    private String id;
    private String deviceCode;
    private String deviceName;
    private String status;
    private PointNodeBO ctrl_mode;
    private PointNodeBO run_state;
    private PointNodeBO nav_mode;
    private PointNodeBO battery_voltage;
    private PointNodeBO blind_dist;
    private PointNodeBO current_tag;
    private PointNodeBO nav_route;
//    private PointNodeBO lspeed;
//    private PointNodeBO rspeed;
//    private PointNodeBO work_status;
//    private PointNodeBO operating_mode;
//    private PointNodeBO cleaning;
//    private PointNodeBO full;
//    private PointNodeBO ejection;
//    private PointNodeBO charging;
//    private PointNodeBO exit_direction;
//    private PointNodeBO EWP;
//    private PointNodeBO trun_count;
//    private PointNodeBO CAF;
}
