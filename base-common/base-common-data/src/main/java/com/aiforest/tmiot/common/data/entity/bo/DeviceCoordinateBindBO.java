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
public class DeviceCoordinateBindBO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long navNodeId;
    private Long deviceCoordinateAttributeId;
    private String coordinateValue;

    private DeviceCoordinateAttributeBO attribute;

}