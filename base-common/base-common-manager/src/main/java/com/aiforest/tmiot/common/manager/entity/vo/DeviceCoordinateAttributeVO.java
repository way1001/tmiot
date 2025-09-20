/*
 * Copyright 2026-present the TM IoT original author or authors.
 */
package com.aiforest.tmiot.common.manager.entity.vo;

import com.aiforest.tmiot.common.constant.common.TimeConstant;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.aiforest.tmiot.common.entity.base.BaseVO;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DeviceCoordinateAttributeVO implements Serializable {

    @NotBlank(groups = {Add.class})
    private String attributeName;

    @NotBlank(groups = {Add.class})
    private String attributeCode;

    @NotNull(groups = {Add.class, Update.class})
    private Long deviceId;

    private EnableFlagEnum enableFlag;

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空",
            groups = {Update.class})
    private Long id;

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
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
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
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime operateTime;
}