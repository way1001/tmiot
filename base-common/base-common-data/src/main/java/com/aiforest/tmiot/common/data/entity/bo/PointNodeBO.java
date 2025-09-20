package com.aiforest.tmiot.common.data.entity.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PointNodeBO {
    private String pointId;
    private String pointName;
    private String pointCode;
    private String unit;
    private String rawValue;
    private String calValue;
}