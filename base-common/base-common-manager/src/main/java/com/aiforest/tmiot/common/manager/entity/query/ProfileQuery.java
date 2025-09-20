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

package com.aiforest.tmiot.common.manager.entity.query;

import com.aiforest.tmiot.common.entity.common.Pages;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.enums.ProfileShareFlagEnum;
import com.aiforest.tmiot.common.enums.ProfileTypeFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Profile Query
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProfileQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * 租户ID
     */
    private Long tenantId;

    // 查询字段

    /**
     * 模版名称
     */
    private String profileName;

    /**
     * 模版编号
     */
    private String profileCode;

    /**
     * 模版共享类型标识
     */
    private ProfileShareFlagEnum profileShareFlag;

    /**
     * 模版类型标识
     */
    private ProfileTypeFlagEnum profileTypeFlag;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 版本
     */
    private Integer version;

    // 附加字段

    /**
     * 设备ID
     */
    private Long deviceId;
}