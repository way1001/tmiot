package com.aiforest.tmiot.common.manager.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateBindBO;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateBindDO;
import com.aiforest.tmiot.common.manager.entity.vo.DeviceCoordinateBindVO;
import com.aiforest.tmiot.common.utils.MapStructUtil;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = MapStructUtil.class)
public interface DeviceCoordinateBindBuilder {

    /* VO → BO */
    @Mapping(target = "tenantId", ignore = true)
    DeviceCoordinateBindBO buildBOByVO(DeviceCoordinateBindVO vo);
    List<DeviceCoordinateBindBO> buildBOListByVOList(List<DeviceCoordinateBindVO> vos);

    /* BO → DO */
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    DeviceCoordinateBindDO buildDOByBO(DeviceCoordinateBindBO bo);
    List<DeviceCoordinateBindDO> buildDOListByBOList(List<DeviceCoordinateBindBO> bos);

    @AfterMapping
    default void afterBO2DO(DeviceCoordinateBindBO bo, @MappingTarget DeviceCoordinateBindDO entity) {
        if (bo.getEnableFlag() != null) {
            entity.setEnableFlag(bo.getEnableFlag().getIndex());
        }
    }

    /* DO → BO */
    @Mapping(target = "enableFlag", ignore = true)
    DeviceCoordinateBindBO buildBOByDO(DeviceCoordinateBindDO entity);
    List<DeviceCoordinateBindBO> buildBOListByDOList(List<DeviceCoordinateBindDO> entities);

    @AfterMapping
    default void afterDO2BO(DeviceCoordinateBindDO entity, @MappingTarget DeviceCoordinateBindBO bo) {
        bo.setEnableFlag(EnableFlagEnum.ofIndex(entity.getEnableFlag()));
    }

    /* BO → VO */
    DeviceCoordinateBindVO buildVOByBO(DeviceCoordinateBindBO bo);
    List<DeviceCoordinateBindVO> buildVOListByBOList(List<DeviceCoordinateBindBO> bos);

    /* 分页 */
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<DeviceCoordinateBindBO> buildBOPageByDOPage(Page<DeviceCoordinateBindDO> pageDO);

    Page<DeviceCoordinateBindVO> buildVOPageByBOPage(Page<DeviceCoordinateBindBO> pageBO);
}