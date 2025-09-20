package com.aiforest.tmiot.common.data.entity.vo;

import lombok.Data;

@Data
public class HistoryPageVO {
    private java.util.List<DeviceSnapshotVO> records;
    private Long total;
    private Long size;
    private Long current;
}
