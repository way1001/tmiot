package com.aiforest.tmiot.common.data.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsRespVO {

    /** 任务条数 */
    private long taskCount;

    /** 导航路径总段数 */
    private long navSegmentCount;

    /**
     * 简单积分的总耗电量
     * 单位：mV·min（可按需要再乘系数转 mWh）
     */
    private double totalConsumption;

    /** 检测到的充电次数 */
    private int chargeCount;
}
