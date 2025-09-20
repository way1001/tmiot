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
package com.aiforest.tmiot.common.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.constant.common.QueryWrapperConstant;
import com.aiforest.tmiot.common.entity.common.Pages;
import com.aiforest.tmiot.common.exception.*;
import com.aiforest.tmiot.common.manager.dal.DeviceCoordinateAttributeManager;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateAttributeBO;
import com.aiforest.tmiot.common.manager.entity.builder.DeviceCoordinateAttributeBuilder;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateAttributeDO;
import com.aiforest.tmiot.common.manager.entity.query.DeviceCoordinateAttributeQuery;
import com.aiforest.tmiot.common.manager.service.DeviceCoordinateAttributeService;
import com.aiforest.tmiot.common.utils.FieldUtil;
import com.aiforest.tmiot.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * DeviceCoordinateAttributeServiceImpl
 *
 * @author way
 * @version 2026.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DeviceCoordinateAttributeServiceImpl implements DeviceCoordinateAttributeService {

    @Resource
    private DeviceCoordinateAttributeBuilder builder;

    @Resource
    private DeviceCoordinateAttributeManager manager;

    /* ---------- 增 ---------- */

    @Override
    public void save(DeviceCoordinateAttributeBO bo) {
        if (checkDuplicate(bo, false)) {
            throw new DuplicateException("Device coordinate attribute already exists");
        }
        DeviceCoordinateAttributeDO entity = builder.buildDOByBO(bo);
        if (!manager.save(entity)) {
            throw new AddException("Failed to create device coordinate attribute");
        }
    }

    /* ---------- 删 ---------- */

    @Override
    public void remove(Long id) {
        getDOById(id, true);
        if (!manager.removeById(id)) {
            throw new DeleteException("Failed to delete device coordinate attribute");
        }
    }

    /* ---------- 改 ---------- */

    @Override
    public void update(DeviceCoordinateAttributeBO bo) {
        getDOById(bo.getId(), true);
        if (checkDuplicate(bo, true)) {
            throw new DuplicateException("Device coordinate attribute duplicated");
        }
        DeviceCoordinateAttributeDO entity = builder.buildDOByBO(bo);
        entity.setOperateTime(null);
        if (!manager.updateById(entity)) {
            throw new UpdateException("Failed to update device coordinate attribute");
        }
    }

    /* ---------- 查 ---------- */

    @Override
    public DeviceCoordinateAttributeBO selectById(Long id) {
        DeviceCoordinateAttributeDO entity = getDOById(id, true);
        return builder.buildBOByDO(entity);
    }

    /**
     * 根据设备ID查询其下全部坐标属性
     */
    @Override
    public List<DeviceCoordinateAttributeBO> selectByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<DeviceCoordinateAttributeDO> wrapper =
                manager.lambdaQuery()
                        .eq(DeviceCoordinateAttributeDO::getDeviceId, deviceId)
                        .eq(DeviceCoordinateAttributeDO::getDeleted, 0);
        List<DeviceCoordinateAttributeDO> list = wrapper.list();
        return builder.buildBOListByDOList(list);
    }

    @Override
    public List<DeviceCoordinateAttributeBO> selectByDeviceIds(List<Long> deviceIds) {
        if (org.springframework.util.CollectionUtils.isEmpty(deviceIds)) {
            return java.util.Collections.emptyList();
        }
        LambdaQueryWrapper<DeviceCoordinateAttributeDO> wrapper =
                Wrappers.<DeviceCoordinateAttributeDO>lambdaQuery()
                        .in(DeviceCoordinateAttributeDO::getDeviceId, deviceIds)
                        .eq(DeviceCoordinateAttributeDO::getDeleted, 0);
        List<DeviceCoordinateAttributeDO> list = manager.list(wrapper);
        return builder.buildBOListByDOList(list);
    }

    @Override
    public Page<DeviceCoordinateAttributeBO> selectByPage(DeviceCoordinateAttributeQuery query) {
        if (Objects.isNull(query.getPage())) {
            query.setPage(new Pages());
        }
        Page<DeviceCoordinateAttributeDO> pageDO =
                manager.page(PageUtil.page(query.getPage()), fuzzyQuery(query));
        return builder.buildBOPageByDOPage(pageDO);
    }

    /* ---------- 私有工具 ---------- */

    private LambdaQueryWrapper<DeviceCoordinateAttributeDO> fuzzyQuery(DeviceCoordinateAttributeQuery query) {
        LambdaQueryWrapper<DeviceCoordinateAttributeDO> wrapper =
                Wrappers.<DeviceCoordinateAttributeDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(query.getAttributeName()),
                DeviceCoordinateAttributeDO::getAttributeName, query.getAttributeName());
        wrapper.like(CharSequenceUtil.isNotEmpty(query.getAttributeCode()),
                DeviceCoordinateAttributeDO::getAttributeCode, query.getAttributeCode());
        wrapper.eq(FieldUtil.isValidIdField(query.getDeviceId()),
                DeviceCoordinateAttributeDO::getDeviceId, query.getDeviceId());
        wrapper.eq(DeviceCoordinateAttributeDO::getTenantId, query.getTenantId());
        wrapper.eq(DeviceCoordinateAttributeDO::getDeleted, 0);
        return wrapper;
    }

    private boolean checkDuplicate(DeviceCoordinateAttributeBO bo, boolean isUpdate) {
        LambdaQueryWrapper<DeviceCoordinateAttributeDO> wrapper =
                Wrappers.<DeviceCoordinateAttributeDO>query().lambda()
                        .eq(DeviceCoordinateAttributeDO::getAttributeCode, bo.getAttributeCode())
                        .eq(DeviceCoordinateAttributeDO::getDeviceId, bo.getDeviceId())
                        .eq(DeviceCoordinateAttributeDO::getTenantId, bo.getTenantId())
                        .last(QueryWrapperConstant.LIMIT_ONE);
        DeviceCoordinateAttributeDO one = manager.getOne(wrapper);
        return one != null && (!isUpdate || !one.getId().equals(bo.getId()));
    }

    private DeviceCoordinateAttributeDO getDOById(Long id, boolean throwEx) {
        DeviceCoordinateAttributeDO entity = manager.getById(id);
        if (throwEx && Objects.isNull(entity)) {
            throw new NotFoundException("Device coordinate attribute does not exist");
        }
        return entity;
    }
}