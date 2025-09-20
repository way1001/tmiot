package com.aiforest.tmiot.common.data.entity.model;


import com.aiforest.tmiot.common.data.entity.bo.DeviceSnapshotBO;
import com.aiforest.tmiot.common.data.handler.JacksonNodeTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "t_device_points_jsonb", autoResultMap = true)
public class DevicePointsJsonbDO {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("device_id")
    private String deviceId;

    /** 映射 JSONB 列，直接到 BO */
//    @TableField(typeHandler = JacksonTypeHandler.class)
//    private DeviceSnapshotBO snapshot;

    @TableField(typeHandler = JacksonNodeTypeHandler.class)
    private JsonNode snapshot;

    @TableField("create_time")
    private LocalDateTime createTime;
}
