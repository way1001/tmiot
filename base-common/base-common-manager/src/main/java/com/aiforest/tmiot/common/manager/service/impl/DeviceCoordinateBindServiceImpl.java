package com.aiforest.tmiot.common.manager.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateAttributeDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.aiforest.tmiot.common.constant.common.QueryWrapperConstant;
import com.aiforest.tmiot.common.exception.*;
import com.aiforest.tmiot.common.manager.dal.DeviceCoordinateBindManager;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateBindBO;
import com.aiforest.tmiot.common.manager.entity.builder.DeviceCoordinateBindBuilder;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateBindDO;
import com.aiforest.tmiot.common.manager.entity.query.DeviceCoordinateBindQuery;
import com.aiforest.tmiot.common.manager.service.DeviceCoordinateBindService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DeviceCoordinateBindServiceImpl implements DeviceCoordinateBindService {

    @Resource
    private DeviceCoordinateBindBuilder builder;
    @Resource
    private DeviceCoordinateBindManager manager;

    @Override
    public void save(DeviceCoordinateBindBO bo) {
        if (checkDuplicate(bo, false)) {
            throw new DuplicateException("Device coordinate bind already exists");
        }
        DeviceCoordinateBindDO entity = builder.buildDOByBO(bo);
        if (!manager.save(entity)) {
            throw new AddException("Failed to create device coordinate bind");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);
        if (!manager.removeById(id)) {
            throw new DeleteException("Failed to delete device coordinate bind");
        }
    }

    @Override
    public void update(DeviceCoordinateBindBO bo) {
        getDOById(bo.getId(), true);
        if (checkDuplicate(bo, true)) {
            throw new DuplicateException("Device coordinate bind duplicated");
        }
        DeviceCoordinateBindDO entity = builder.buildDOByBO(bo);
        entity.setOperateTime(null);
        if (!manager.updateById(entity)) {
            throw new UpdateException("Failed to update device coordinate bind");
        }
    }

    @Override
    public DeviceCoordinateBindBO selectById(Long id) {
        return builder.buildBOByDO(getDOById(id, true));
    }

    @Override
    public List<DeviceCoordinateBindBO> selectByNavNodeId(Long navNodeId) {
        List<DeviceCoordinateBindDO> list = manager.lambdaQuery()
                .eq(DeviceCoordinateBindDO::getNavNodeId, navNodeId)
                .eq(DeviceCoordinateBindDO::getDeleted, 0)
                .list();
        return builder.buildBOListByDOList(list);
    }

    @Override
    public List<DeviceCoordinateBindBO> selectByDeviceId(Long deviceId) {
        List<DeviceCoordinateBindDO> list = manager.lambdaQuery()
                .eq(DeviceCoordinateBindDO::getDeviceId, deviceId)
                .eq(DeviceCoordinateBindDO::getDeleted, 0)
                .list();
        return builder.buildBOListByDOList(list);
    }

    @Override
    public List<DeviceCoordinateBindBO> selectByAttributeId(Long attributeId) {
        List<DeviceCoordinateBindDO> list = manager.lambdaQuery()
                .eq(DeviceCoordinateBindDO::getDeviceCoordinateAttributeId, attributeId)
                .eq(DeviceCoordinateBindDO::getDeleted, 0)
                .list();
        return builder.buildBOListByDOList(list);
    }

    /* -------------------- private -------------------- */

    private LambdaQueryWrapper<DeviceCoordinateBindDO> fuzzyQuery(DeviceCoordinateBindQuery query) {
        LambdaQueryWrapper<DeviceCoordinateBindDO> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Objects.nonNull(query.getNavNodeId()), DeviceCoordinateBindDO::getNavNodeId, query.getNavNodeId());
        wrapper.eq(Objects.nonNull(query.getDeviceId()), DeviceCoordinateBindDO::getDeviceId, query.getDeviceId());
        wrapper.eq(Objects.nonNull(query.getDeviceCoordinateAttributeId()),
                DeviceCoordinateBindDO::getDeviceCoordinateAttributeId, query.getDeviceCoordinateAttributeId());
        wrapper.like(CharSequenceUtil.isNotBlank(query.getCoordinateValue()),
                DeviceCoordinateBindDO::getCoordinateValue, query.getCoordinateValue());
        wrapper.eq(DeviceCoordinateBindDO::getTenantId, query.getTenantId());
        wrapper.eq(DeviceCoordinateBindDO::getDeleted, 0);
        return wrapper;
    }

    private boolean checkDuplicate(DeviceCoordinateBindBO bo, boolean isUpdate) {
        LambdaQueryWrapper<DeviceCoordinateBindDO> wrapper =  Wrappers.<DeviceCoordinateBindDO>query().lambda()
                .eq(DeviceCoordinateBindDO::getNavNodeId, bo.getNavNodeId())
                .eq(DeviceCoordinateBindDO::getDeviceCoordinateAttributeId, bo.getDeviceCoordinateAttributeId())
                .eq(DeviceCoordinateBindDO::getDeviceId, bo.getDeviceId())
                .last(QueryWrapperConstant.LIMIT_ONE);
        DeviceCoordinateBindDO one = manager.getOne(wrapper);
        return one != null && (!isUpdate || !one.getId().equals(bo.getId()));
    }

    private DeviceCoordinateBindDO getDOById(Long id, boolean throwEx) {
        DeviceCoordinateBindDO entity = manager.getById(id);
        if (throwEx && Objects.isNull(entity)) {
            throw new NotFoundException("Device coordinate bind does not exist");
        }
        return entity;
    }
}