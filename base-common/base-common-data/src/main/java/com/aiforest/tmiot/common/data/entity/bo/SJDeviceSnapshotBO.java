package com.aiforest.tmiot.common.data.entity.bo;

import lombok.Data;

@Data
public class SJDeviceSnapshotBO {
    private String id;
    private String deviceCode;
    private String deviceName;
    private String status;
    private PointNodeBO SWS1;
    private PointNodeBO SRS1;
    private PointNodeBO SWS2;
    private PointNodeBO SRS2;
}
