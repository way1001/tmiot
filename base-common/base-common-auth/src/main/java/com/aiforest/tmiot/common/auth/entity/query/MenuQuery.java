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

package com.aiforest.tmiot.common.auth.entity.query;

import com.aiforest.tmiot.common.entity.common.Pages;
import com.aiforest.tmiot.common.enums.EnableFlagEnum;
import com.aiforest.tmiot.common.enums.MenuTypeFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author linys
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MenuQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * 租户ID
     */
    private Long tenantId;

    // 查询字段

    /**
     * 菜单类型标识
     */
    private MenuTypeFlagEnum menuTypeFlag;

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单编号, 一般为URL的MD5编码
     */
    private String menuCode;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

}
