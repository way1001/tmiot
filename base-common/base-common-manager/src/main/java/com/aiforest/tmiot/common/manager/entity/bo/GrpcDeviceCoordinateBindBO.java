package com.aiforest.tmiot.common.manager.entity.bo;

import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateBindDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备坐标绑定BO（关联坐标属性BO）
 *
 * @author way
 * @since 2025.09.02
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GrpcDeviceCoordinateBindBO extends DeviceCoordinateBindDO {
    // 关联的坐标属性BO（1对1）
    private GrpcDeviceCoordinateAttributeBO attributeBO;
}