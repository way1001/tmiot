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
 * 设备坐标属性与导航节点关联表
 * </p>
 *
 * @author way
 * @since 2025.09.02
 */
@Getter
@Setter
@ToString
@TableName("tm_device_coordinate_bind")
public class DeviceCoordinateBindDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 导航节点ID
     */
    @TableField("nav_node_id")
    private Long navNodeId;

    /**
     * 设备坐标属性ID
     */
    @TableField("device_coordinate_attribute_id")
    private Long deviceCoordinateAttributeId;

    /**
     * 设备ID
     */
    @TableField("device_id")
    private Long deviceId;

    /**
     * 坐标值
     */
    @TableField("coordinate_value")
    private String coordinateValue;

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
