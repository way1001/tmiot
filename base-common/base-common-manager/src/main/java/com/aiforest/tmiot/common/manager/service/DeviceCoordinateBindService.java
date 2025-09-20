package com.aiforest.tmiot.common.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceCoordinateBindBO;
import com.aiforest.tmiot.common.manager.entity.query.DeviceCoordinateBindQuery;

import java.util.List;

public interface DeviceCoordinateBindService {

    void save(DeviceCoordinateBindBO bo);

    void remove(Long id);

    void update(DeviceCoordinateBindBO bo);

    DeviceCoordinateBindBO selectById(Long id);

    List<DeviceCoordinateBindBO> selectByNavNodeId(Long navNodeId);

    List<DeviceCoordinateBindBO> selectByDeviceId(Long deviceId);

    List<DeviceCoordinateBindBO> selectByAttributeId(Long attributeId);

}