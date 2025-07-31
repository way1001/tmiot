/*
 * Copyright 2016-present the TM IoT original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aiforest.tmiot.common.manager.entity.bo;

import com.aiforest.tmiot.common.entity.ext.DeviceExt;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.manager.entity.model.PointDO;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DevicePointLatestBO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 驱动ID
     */
    private Long driverId;

    /**
     * 设备拓展信息
     */
    private DeviceExt deviceExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 签名
     */
    private String signature;

    /**
     * 版本
     */
    private Integer version;

    // 附加字段
    /**
     * 点位集合
     */
    private List<PointBO> points;
}
