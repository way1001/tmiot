package com.aiforest.tmiot.common.data.entity.bo;

import lombok.Data;

@Data
public class TMDeviceSnapshotBO {
    private String id;
    private String deviceCode;
    private String deviceName;
    private String status;
    private PointNodeBO full ;
    private PointNodeBO lifting ;
    private PointNodeBO cleaning;
    private PointNodeBO retainer;
}
