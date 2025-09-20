/*
 * Copyright 2026-present the TM IoT original author or authors.
 */
package com.aiforest.tmiot.common.manager.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateAttributeBO;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateAttributeDO;
import com.aiforest.tmiot.common.manager.entity.vo.DeviceCoordinateAttributeVO;
import com.aiforest.tmiot.common.utils.MapStructUtil;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = MapStructUtil.class)
public interface DeviceCoordinateAttributeBuilder {

    /* VO → BO */
    @Mapping(target = "tenantId", ignore = true)
    DeviceCoordinateAttributeBO buildBOByVO(DeviceCoordinateAttributeVO vo);
    List<DeviceCoordinateAttributeBO> buildBOListByVOList(List<DeviceCoordinateAttributeVO> vos);

    /* BO → DO */
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    DeviceCoordinateAttributeDO buildDOByBO(DeviceCoordinateAttributeBO bo);
    List<DeviceCoordinateAttributeDO> buildDOListByBOList(List<DeviceCoordinateAttributeBO> bos);

    @AfterMapping
    default void afterBO2DO(DeviceCoordinateAttributeBO bo, @MappingTarget DeviceCoordinateAttributeDO entity) {
        if (bo.getEnableFlag() != null) {
            entity.setEnableFlag(bo.getEnableFlag().getIndex());
        }
    }

    /* DO → BO */
    @Mapping(target = "enableFlag", ignore = true)
    DeviceCoordinateAttributeBO buildBOByDO(DeviceCoordinateAttributeDO entity);
    List<DeviceCoordinateAttributeBO> buildBOListByDOList(List<DeviceCoordinateAttributeDO> entities);

    @AfterMapping
    default void afterDO2BO(DeviceCoordinateAttributeDO entity, @MappingTarget DeviceCoordinateAttributeBO bo) {
        bo.setEnableFlag(EnableFlagEnum.ofIndex(entity.getEnableFlag()));
    }

    /* BO → VO */
    DeviceCoordinateAttributeVO buildVOByBO(DeviceCoordinateAttributeBO bo);
    List<DeviceCoordinateAttributeVO> buildVOListByBOList(List<DeviceCoordinateAttributeBO> bos);

    /* 分页 */
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DeviceCoordinateAttributeBO> buildBOPageByDOPage(Page<DeviceCoordinateAttributeDO> pageDO);

    Page<DeviceCoordinateAttributeVO> buildVOPageByBOPage(Page<DeviceCoordinateAttributeBO> pageBO);
}