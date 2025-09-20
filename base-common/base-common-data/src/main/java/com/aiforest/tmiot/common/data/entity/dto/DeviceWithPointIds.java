package com.aiforest.tmiot.common.data.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DeviceWithPointIds {
    private final Long deviceId;
    private final List<Long> pointIds;
}