package com.aiforest.tmiot.common.manager.entity.bo;

import com.aiforest.tmiot.common.manager.entity.model.CusNavOverviewDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 自定导航总览BO（关联节点BO列表）
 *
 * @author way
 * @since 2025.08.30
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GrpcCusNavOverviewBO extends CusNavOverviewDO {
    // 关联的导航节点BO列表（1对多）
    private List<GrpcNavNodesBO> grpcNavNodesBOList;
}