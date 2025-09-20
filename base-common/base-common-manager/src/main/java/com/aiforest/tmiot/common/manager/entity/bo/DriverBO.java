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

import com.aiforest.tmiot.common.entity.base.BaseBO;
import com.aiforest.tmiot.common.entity.ext.DriverExt;
import com.aiforest.tmiot.common.enums.DriverTypeFlagEnum;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import lombok.*;

/**
 * Driver BO
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class DriverBO extends BaseBO {

    /**
     * 驱动名称
     */
    private String driverName;

    /**
     * 驱动编号
     */
    private String driverCode;

    /**
     * 驱动服务名称
     */
    private String serviceName;

    /**
     * 服务主机
     */
    private String serviceHost;

    /**
     * 驱动类型标识
     */
    private DriverTypeFlagEnum driverTypeFlag;

    /**
     * 驱动拓展信息
     */
    private DriverExt driverExt;

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
}
