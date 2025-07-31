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

package com.aiforest.tmiot.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.aiforest.tmiot.common.entity.base.BaseVO;
import com.aiforest.tmiot.common.entity.ext.ApiExt;
import com.aiforest.tmiot.common.enums.ApiTypeFlagEnum;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.valid.Add;
import com.aiforest.tmiot.common.valid.Auth;
import com.aiforest.tmiot.common.valid.Update;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * Api VO
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
public class ApiVO extends BaseVO {

    /**
     * Api接口类型标识
     */
    private ApiTypeFlagEnum apiTypeFlag;

    /**
     * Api接口名称
     */
    @NotBlank(message = "Api接口名称不能为空",
            groups = {Add.class, Auth.class})
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$",
            message = "无效接口名称",
            groups = {Add.class, Update.class})
    private String apiName;

    /**
     * Api接口编号, 一般为URL的MD5编码
     */
    private String apiCode;

    /**
     * Api接口拓展信息
     */
    private ApiExt apiExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
