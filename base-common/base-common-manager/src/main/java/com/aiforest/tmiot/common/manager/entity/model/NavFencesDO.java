package com.aiforest.tmiot.common.manager.entity.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * 导航围栏表：存储导航场景中绘制的水平/垂直围栏直线信息
 * </p>
 *
 * @author way
 * @since 2025.09.19
 */
@Getter
@Setter
@ToString
@TableName("tm_nav_fences")
public class NavFencesDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 围栏主键ID（自增）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联导航总览ID（对应 tm_cus_nav_overview.id）
     */
    @TableField("overview_id")
    private Long overviewId;

    /**
     * 围栏排序序号：用于前端按顺序展示围栏
     */
    @TableField("fence_order")
    private Integer fenceOrder;

    /**
     * 围栏起点X坐标（网格单位，对应前端 TFence.x1）
     */
    @TableField("grid_x1")
    private Integer gridX1;

    /**
     * 围栏终点X坐标（网格单位，对应前端 TFence.x2）
     */
    @TableField("grid_x2")
    private Integer gridX2;

    /**
     * 围栏起点Y坐标（网格单位，对应前端 TFence.y1）
     */
    @TableField("grid_y1")
    private Integer gridY1;

    /**
     * 围栏终点Y坐标（网格单位，对应前端 TFence.y2）
     */
    @TableField("grid_y2")
    private Integer gridY2;

    /**
     * 逻辑删除标识：0=未删除，1=已删除
     */
    @TableLogic
    @TableField("deleted")
    private Byte deleted;
}
