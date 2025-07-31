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
import com.aiforest.tmiot.common.manager.entity.bo.ProfileBindBO;
import com.aiforest.tmiot.common.manager.entity.query.ProfileBindQuery;

import java.util.List;

/**
 * ProfileBind Interface
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
public interface ProfileBindService extends BaseService<ProfileBindBO, ProfileBindQuery> {

    /**
     * 根据 设备ID 删除关联的模版映射
     *
     * @param deviceId 设备ID
     * @return 是否删除
     */
    Boolean removeByDeviceId(Long deviceId);

    /**
     * 根据 设备ID 和 模版ID 删除关联的模版映射
     *
     * @param deviceId  设备ID
     * @param profileId 位号ID
     * @return 是否删除
     */
    Boolean removeByDeviceIdAndProfileId(Long deviceId, Long profileId);

    /**
     * 根据 设备ID 和 模版ID 查询关联的模版映射
     *
     * @param deviceId  设备ID
     * @param profileId 位号ID
     * @return ProfileBind
     */
    ProfileBindBO selectByDeviceIdAndProfileId(Long deviceId, Long profileId);

    /**
     * 根据 模版ID 查询关联的 设备ID 集合
     *
     * @param profileId 位号ID
     * @return 设备ID集
     */
    List<Long> selectDeviceIdsByProfileId(Long profileId);

    /**
     * 根据 设备ID 查询关联的 模版ID 集合
     *
     * @param deviceId 设备ID
     * @return 模版ID集
     */
    List<Long> selectProfileIdsByDeviceId(Long deviceId);

}
