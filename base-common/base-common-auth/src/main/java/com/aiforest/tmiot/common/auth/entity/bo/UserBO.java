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

package com.aiforest.tmiot.common.auth.entity.bo;

import com.aiforest.tmiot.common.entity.base.BaseBO;
import com.aiforest.tmiot.common.entity.ext.UserIdentityExt;
import com.aiforest.tmiot.common.entity.ext.UserSocialExt;
import lombok.*;

/**
 * User BO
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
public class UserBO extends BaseBO {

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 社交相关拓展信息
     */
    private UserSocialExt socialExt;

    /**
     * 身份相关拓展信息
     */
    private UserIdentityExt identityExt;

    /**
     * 使能标识, 0:启用, 1:禁用
     */
    private Byte enableFlag;

}
