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
import com.aiforest.tmiot.common.entity.ext.ProfileExt;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.enums.ProfileShareFlagEnum;
import com.aiforest.tmiot.common.enums.ProfileTypeFlagEnum;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Profile VO
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
public class ProfileVO extends BaseVO {

    /**
     * 模版名称
     */
    @NotBlank(message = "模版名称不能为空",
            groups = {Add.class})
    @Pattern(regexp = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_#@/.|]{1,31}$",
            message = "模版名称格式无效",
            groups = {Add.class, Update.class})
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
     * 模版拓展信息
     */
    private ProfileExt profileExt;

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
