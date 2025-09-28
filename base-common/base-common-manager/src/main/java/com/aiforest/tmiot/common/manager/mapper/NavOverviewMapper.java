package com.aiforest.tmiot.common.manager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.manager.entity.model.CusNavOverviewDO;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateAttributeDO;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateBindDO;
import com.aiforest.tmiot.common.manager.entity.model.NavNodesDO;
import com.aiforest.tmiot.common.manager.entity.query.CusNavOverviewQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 自定导航相关Mapper接口
 *
 * @author way
 * @since 2025.09.02
 */
public interface NavOverviewMapper extends BaseMapper<CusNavOverviewDO> {

    /**
     * 分页查询自定导航总览
     */
    Page<CusNavOverviewDO> selectOverviewPage(Page<CusNavOverviewDO> page,
                                               @Param("query") CusNavOverviewQuery query);

    /**
     * 根据总览ID查询导航节点
     */
    List<NavNodesDO> selectNodesByOverviewId(@Param("overviewId") Long overviewId);

    /**
     * 根据节点ID查询坐标绑定
     */
    List<DeviceCoordinateBindDO> selectBindsByNodeId(@Param("nodeId") Long nodeId);

    /**
     * 根据属性ID查询坐标属性
     */
    DeviceCoordinateAttributeDO selectAttributeById(@Param("attributeId") Long attributeId);
}