/*
 * Copyright 2026-present the TM IoT original author or authors.
 */
package com.aiforest.tmiot.common.manager.entity.bo;

import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class DeviceCoordinateAttributeBO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String attributeName;
    private String attributeCode;
    private Long deviceId;
    private EnableFlagEnum enableFlag;
    private Long tenantId;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建者名称
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 操作者ID
     */
    private Long operatorId;

    /**
     * 操作者名称
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
}