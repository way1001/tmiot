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

package com.aiforest.tmiot.common.manager.service;

import com.aiforest.tmiot.common.base.service.BaseService;
import com.aiforest.tmiot.common.enums.ProfileTypeFlagEnum;
import com.aiforest.tmiot.common.manager.entity.bo.ProfileBO;
import com.aiforest.tmiot.common.manager.entity.query.ProfileQuery;

import java.util.List;
import java.util.Set;

/**
 * Profile Interface
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
public interface ProfileService extends BaseService<ProfileBO, ProfileQuery> {

    /**
     * 根据 模板名称和模版类型 查询模版
     *
     * @param tenantId 租户ID
     * @param name     模板名称
     * @param type     模板类型 {@link ProfileTypeFlagEnum}
     * @return ProfileBO
     */
    ProfileBO selectByNameAndType(Long tenantId, String name, ProfileTypeFlagEnum type);

    /**
     * 根据 设备ID 查询模版
     *
     * @param deviceId 设备ID
     * @return ProfileBO 集合
     */
    List<ProfileBO> selectByDeviceId(Long deviceId);

    /**
     * 根据 模版ID集 查询模版
     *
     * @param ids 模版ID集
     * @return ProfileBO 集合
     */
    List<ProfileBO> selectByIds(Set<Long> ids);

}
