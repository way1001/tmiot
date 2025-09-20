package com.aiforest.tmiot.common.manager.entity.vo;

import com.aiforest.tmiot.common.constant.common.TimeConstant;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Update;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class DeviceCoordinateBindVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(groups = {Update.class})
    private Long id;

    @NotNull(groups = {Add.class})
    private Long navNodeId;

    @NotNull(groups = {Add.class})
    private Long deviceCoordinateAttributeId;

    @NotNull(groups = {Add.class})
    private Long deviceId;

    private String coordinateValue;

    private EnableFlagEnum enableFlag;

    /* audit */
    private Long creatorId;
    private String creatorName;
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime createTime;
    private Long operatorId;
    private String operatorName;
    @JsonFormat(pattern = TimeConstant.COMPLETE_DATE_FORMAT, timezone = TimeConstant.DEFAULT_TIMEZONE)
    private LocalDateTime operateTime;
}