package com.aiforest.tmiot.common.manager.entity.bo;

import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateAttributeDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备坐标属性BO（关联DO）
 *
 * @author way
 * @since 2025.09.02
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GrpcDeviceCoordinateAttributeBO extends DeviceCoordinateAttributeDO {
    // 直接继承DO的所有字段，如需扩展业务字段可在此添加
}