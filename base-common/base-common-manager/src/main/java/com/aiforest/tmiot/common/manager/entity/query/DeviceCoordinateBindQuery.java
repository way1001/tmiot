package com.aiforest.tmiot.common.manager.entity.query;

import com.aiforest.tmiot.common.entity.common.Pages;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceCoordinateBindQuery extends Pages {

    private Long navNodeId;
    private Long deviceCoordinateAttributeId;
    private Long deviceId;
    private Long tenantId;
    private String coordinateValue;
}