package com.aiforest.tmiot.common.data.entity.bo;

import lombok.Data;

@Data
public class TMDeviceSnapshotBO {
    private String id;
    private String deviceCode;
    private String deviceName;
    private String status;
    private PointNodeBO lwpfull;
    private PointNodeBO rwpfull ;
    private PointNodeBO llifting ;
    private PointNodeBO rlifting ;
    private PointNodeBO lcleaning;
    private PointNodeBO rcleaning;
    private PointNodeBO lstart ;
    private PointNodeBO rstart ;
}