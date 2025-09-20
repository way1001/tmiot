/*
 * Copyright 2026-present the TM IoT original author or authors.
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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateAttributeBO;
import com.aiforest.tmiot.common.manager.entity.query.DeviceCoordinateAttributeQuery;

import java.util.List;

/**
 * 设备坐标属性业务接口
 *
 * @author way
 * @version 2026.7.0
 * @since 2022.1.0
 */
public interface DeviceCoordinateAttributeService {

    /**
     * 新增
     */
    void save(DeviceCoordinateAttributeBO bo);

    /**
     * 根据 ID 删除
     */
    void remove(Long id);

    /**
     * 更新
     */
    void update(DeviceCoordinateAttributeBO bo);

    /**
     * 根据主键查询
     */
    DeviceCoordinateAttributeBO selectById(Long id);

    /**
     * 根据设备ID查询其下全部坐标属性
     */
    List<DeviceCoordinateAttributeBO> selectByDeviceId(Long deviceId);

    List<DeviceCoordinateAttributeBO> selectByDeviceIds(List<Long> deviceIds);


    /**
     * 分页查询
     */
    Page<DeviceCoordinateAttributeBO> selectByPage(DeviceCoordinateAttributeQuery query);
}