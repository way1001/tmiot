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

package com.aiforest.tmiot.common.dal.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.aiforest.tmiot.common.entity.base.BaseVO;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.enums.GroupTypeFlagEnum;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Group VO
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
public class GroupVO extends BaseVO {

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
    @NotBlank(message = "分组名称不能为空",
            groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "分组名称格式无效",
            groups = {Add.class, Update.class})
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
    private Integer groupIndex;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
