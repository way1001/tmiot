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

package com.aiforest.tmiot.common.constant.service;

import com.aiforest.tmiot.common.constant.common.ExceptionConstant;

/**
 * 权限服务 相关常量
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
public class AuthConstant {

    /**
     * 服务名
     */
    public static final String SERVICE_NAME = "base-center-auth";
    public static final String USER_URL_PREFIX = "/user";
    public static final String TENANT_URL_PREFIX = "/tenant";
    public static final String TOKEN_URL_PREFIX = "/token";
    public static final String DICTIONARY_URL_PREFIX = "/dictionary";

    private AuthConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
