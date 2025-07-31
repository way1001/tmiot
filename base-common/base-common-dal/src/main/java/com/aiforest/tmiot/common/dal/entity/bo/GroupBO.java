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

package com.aiforest.tmiot.common.dal.entity.bo;

import com.aiforest.tmiot.common.entity.base.BaseBO;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.enums.GroupTypeFlagEnum;
import lombok.*;

/**
 * Group BO
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
public class GroupBO extends BaseBO {

    /**
     * 父分组ID
     */
    private String parentGroupId;

    /**
     * 分组标识
     */
    private GroupTypeFlagEnum groupTypeFlag;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组编号
     */
    private String groupCode;

    /**
     * 分组层级
     */
    private Byte groupLevel;

    /**
     * 分组顺序
     */
    private Byte groupIndex;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;
}
