package com.aiforest.tmiot.common.manager.entity.query;

import com.aiforest.tmiot.common.entity.common.Pages;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import lombok.Data;

/**
 * 自定导航总览查询条件
 *
 * @author way
 * @since 2025.09.02
 */
@Data
public class CusNavOverviewQuery {
    // 分页参数
    private Pages page;

    // 总览ID（单个查询时必传）
    private Long overviewId;

    // 租户ID（必传，权限过滤）
    private Long tenantId;

    // 导航名称（模糊查询）
    private String navName;

    // 使能标识（0:启用，1:禁用，-1:全部）
    private EnableFlagEnum enableFlag;
}