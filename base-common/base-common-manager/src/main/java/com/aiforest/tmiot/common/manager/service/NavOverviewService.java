package com.aiforest.tmiot.common.manager.service;

import com.aiforest.tmiot.common.manager.entity.bo.GrpcCusNavOverviewBO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.manager.entity.query.CusNavOverviewQuery;

/**
 * 自定导航总览服务接口
 *
 * @author way
 * @since 2025.09.02
 */
public interface NavOverviewService {

    /**
     * 按条件分页查询自定导航总览列表（含完整关联数据）
     */
    Page<GrpcCusNavOverviewBO> selectByPage(CusNavOverviewQuery query);

    /**
     * 按ID查询单个自定导航总览（含完整关联数据）
     */
    GrpcCusNavOverviewBO selectById(CusNavOverviewQuery query);
}