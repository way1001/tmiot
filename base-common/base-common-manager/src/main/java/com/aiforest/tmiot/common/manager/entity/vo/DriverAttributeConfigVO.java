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

package com.aiforest.tmiot.common.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.aiforest.tmiot.common.entity.base.BaseVO;
import com.aiforest.tmiot.common.entity.ext.JsonExt;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DriverAttributeConfig VO
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
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DriverAttributeConfigVO extends BaseVO {

    /**
     * 驱动属性ID
     */
    @NotNull(message = "驱动属性ID不能为空",
            groups = {Add.class, Update.class})
    private Long attributeId;

    /**
     * 驱动属性配置值
     */
    @NotNull(message = "驱动属性配置值不能为空")
    private String configValue;

    /**
     * 设备ID
     */
    @NotNull(message = "设备ID不能为空",
            groups = {Add.class, Update.class})
    private Long deviceId;

    /**
     * 驱动配置信息
     */
    private JsonExt configExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 签名
     */
    private String signature;

    /**
     * 版本
     */
    private Integer version;
}
