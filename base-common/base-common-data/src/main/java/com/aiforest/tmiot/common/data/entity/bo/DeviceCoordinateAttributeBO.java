/*
 * Copyright 2026-present the TM IoT original author or authors.
 */
package com.aiforest.tmiot.common.data.entity.bo;

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
}