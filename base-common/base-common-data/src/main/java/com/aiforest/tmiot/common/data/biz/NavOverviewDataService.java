package com.aiforest.tmiot.common.data.biz;

import com.aiforest.tmiot.common.data.entity.bo.CusNavOverviewBO;
import com.aiforest.tmiot.common.data.entity.query.CusNavOverviewQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * 自定义导航总览数据服务接口
 * 提供导航总览数据的查询能力，适配缓存与底层数据查询
 *
 * @author way
 * @since 2025.09.02
 */
public interface NavOverviewDataService {

    /**
     * 分页查询自定义导航总览数据
     * @return 分页数据列表，包含总览BO对象
     */
    Page<CusNavOverviewBO> listNavOverviews(CusNavOverviewQuery query);
    /**
     * 根据ID查询单条自定义导航总览数据
     * @param overviewId 总览ID
     * @param tenantId 租户ID（数据隔离维度）
     * @return 单条总览BO对象，无数据时返回null
     */
    CusNavOverviewBO getNavOverviewById(Long overviewId, Long tenantId);
}