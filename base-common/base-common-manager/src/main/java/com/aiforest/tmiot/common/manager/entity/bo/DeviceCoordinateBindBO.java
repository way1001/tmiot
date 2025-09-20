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
public class DeviceCoordinateBindBO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long navNodeId;
    private Long deviceCoordinateAttributeId;
    private Long deviceId;
    private String coordinateValue;
    private EnableFlagEnum enableFlag;
    private Long tenantId;

    /* audit */
    private Long creatorId;
    private String creatorName;
    private LocalDateTime createTime;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operateTime;
}