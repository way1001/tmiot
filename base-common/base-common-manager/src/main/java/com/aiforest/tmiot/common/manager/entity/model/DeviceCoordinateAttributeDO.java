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
 * 设备坐标属性配置表
 * </p>
 *
 * @author way
 * @since 2025.09.02
 */
@Getter
@Setter
@ToString
@TableName("tm_device_coordinate_attribute")
public class DeviceCoordinateAttributeDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 坐标属性名称
     */
    @TableField("attribute_name")
    private String attributeName;

    /**
     * 坐标属性编码
     */
    @TableField("attribute_code")
    private String attributeCode;

    /**
     * 设备ID
     */
    @TableField("device_id")
    private Long deviceId;

    /**
     * 使能标识
     */
    @TableField("enable_flag")
    private Byte enableFlag;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("creator_name")
    private String creatorName;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operate_time")
    private LocalDateTime operateTime;

    @TableLogic
    @TableField("deleted")
    private Byte deleted;
}
