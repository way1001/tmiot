package com.aiforest.tmiot.common.manager.entity.bo;

import com.aiforest.tmiot.common.manager.entity.model.NavNodesDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 导航节点BO（关联坐标绑定BO列表）
 *
 * @author way
 * @since 2025.08.30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GrpcNavNodesBO extends NavNodesDO {
    // 关联的坐标绑定BO列表（1对多）
    private GrpcDeviceCoordinateBindBO grpcDeviceCoordinateBindBO;
}