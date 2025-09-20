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
import java.time.LocalDateTime;

/**
 * <p>
 * 自定导航总览表
 * </p>
 *
 * @author way
 * @since 2025.08.30
 */
@Getter
@Setter
@ToString
@TableName("tm_cus_nav_overview")
public class CusNavOverviewDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 自定导航名称
     */
    @TableField("nav_name")
    private String navName;

    /**
     * 画布宽度
     */
    @TableField("width_px")
    private Integer widthPx;

    /**
     * 画布高度
     */
    @TableField("height_px")
    private Integer heightPx;

    /**
     * 原点 left
     */
    @TableField("origin_x_px")
    private Integer originXPx;

    /**
     * 原点 top
     */
    @TableField("origin_y_px")
    private Integer originYPx;

    /**
     * 使能标识, 0:启用, 1:禁用
     */
    @TableField("enable_flag")
    private Byte enableFlag;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 描述
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建者ID
     */
    @TableField("creator_id")
    private Long creatorId;

    /**
     * 创建者名称
     */
    @TableField("creator_name")
    private String creatorName;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 操作者ID
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作者名称
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 操作时间
     */
    @TableField("operate_time")
    private LocalDateTime operateTime;

    /**
     * 逻辑删除标识, 0:未删除, 1:已删除
     */
    @TableLogic
    @TableField("deleted")
    private Byte deleted;
}
