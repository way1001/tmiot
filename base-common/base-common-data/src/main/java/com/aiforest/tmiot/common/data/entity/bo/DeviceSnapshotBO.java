package com.aiforest.tmiot.common.data.entity.bo;

import lombok.Data;

@Data
public class DeviceSnapshotBO {
    private String id;
    private String deviceCode;
    private String deviceName;
    private String status;
    private PointNodeBO lspeed;
    private PointNodeBO rspeed;
    private PointNodeBO work_status;
    private PointNodeBO operating_mode;
    private PointNodeBO cleaning;
    private PointNodeBO full;
    private PointNodeBO ejection;
    private PointNodeBO charging;
    private PointNodeBO exit_direction;
    private PointNodeBO EWP;
    private PointNodeBO trun_count;
    private PointNodeBO CAF;
}
