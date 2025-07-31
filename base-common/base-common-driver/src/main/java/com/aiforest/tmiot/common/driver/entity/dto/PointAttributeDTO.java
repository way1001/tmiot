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

package com.aiforest.tmiot.common.driver.entity.dto;

import com.aiforest.tmiot.common.entity.base.BaseDTO;
import com.aiforest.tmiot.common.entity.ext.PointAttributeExt;
import com.aiforest.tmiot.common.enums.AttributeTypeFlagEnum;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 位号属性BO
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointAttributeDTO extends BaseDTO {

    /**
     * 属性名称
     */
    private String attributeName;

    /**
     * 属性编号
     */
    private String attributeCode;

    /**
     * 属性类型标识
     */
    private AttributeTypeFlagEnum attributeTypeFlag;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 驱动ID
     */
    private Long driverId;

    /**
     * 位号属性拓展信息
     */
    private PointAttributeExt attributeExt;

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
