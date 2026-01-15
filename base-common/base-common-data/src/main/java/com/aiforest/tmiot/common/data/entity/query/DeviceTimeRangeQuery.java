package com.aiforest.tmiot.common.data.entity.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceTimeRangeQuery {
    private List<String> deviceIds;
    private LocalDateTime start;
    private LocalDateTime end;
}