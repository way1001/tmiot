package com.aiforest.tmiot.common.data.entity.bo;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DevicePointJsonbBattVBO {
    /** 设备号 */
    private String deviceId;

    /** 创建时间 */
    private LocalDateTime createTime;


    /** 电压投影值（listVoltageCurveBatch 用） */
    private Integer voltage;
}
