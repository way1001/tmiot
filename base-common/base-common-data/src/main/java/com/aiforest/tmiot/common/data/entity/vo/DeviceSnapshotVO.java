package com.aiforest.tmiot.common.data.entity.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceSnapshotVO {
    private Long id;
    private String deviceId;
    private JsonNode snapshot;
    private LocalDateTime createTime;
}
