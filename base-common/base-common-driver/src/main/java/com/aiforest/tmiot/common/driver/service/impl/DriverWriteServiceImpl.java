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

package com.aiforest.tmiot.common.driver.service.impl;

import com.aiforest.tmiot.common.driver.entity.bean.WValue;
import com.aiforest.tmiot.common.driver.entity.bo.AttributeBO;
import com.aiforest.tmiot.common.driver.entity.bo.DeviceBO;
import com.aiforest.tmiot.common.driver.entity.bo.PointBO;
import com.aiforest.tmiot.common.driver.metadata.DeviceMetadata;
import com.aiforest.tmiot.common.driver.metadata.DriverMetadata;
import com.aiforest.tmiot.common.driver.metadata.PointMetadata;
import com.aiforest.tmiot.common.driver.service.DriverCustomService;
import com.aiforest.tmiot.common.driver.service.DriverWriteService;
import com.aiforest.tmiot.common.entity.dto.DeviceCommandDTO;
import com.aiforest.tmiot.common.exception.ReadPointException;
import com.aiforest.tmiot.common.exception.ServiceException;
import com.aiforest.tmiot.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverWriteServiceImpl implements DriverWriteService {

    @Resource
    private DriverMetadata driverMetadata;
    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private PointMetadata pointMetadata;
    @Resource
    private DriverCustomService driverCustomService;

    @Override
    public void write(Long deviceId, Long pointId, String value) {
        try {
            // Get device from metadata cache
            DeviceBO device = deviceMetadata.getCache(deviceId);
            if (Objects.isNull(device)) {
                throw new ReadPointException("Failed to write point value, device[{}] is null", deviceId);
            }

            // Check if device contains the specified point
            if (!device.getPointIds().contains(pointId)) {
                throw new ReadPointException("Failed to write point value, device[{}] not contained point[{}]", deviceId, pointId);
            }

            // Get driver and point configurations
            Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
            Map<String, AttributeBO> pointConfig = deviceMetadata.getPointConfig(deviceId, pointId);

            // Get point from metadata cache
            PointBO point = pointMetadata.getCache(pointId);
            if (Objects.isNull(point)) {
                throw new ReadPointException("Failed to write point value, point[{}] is null" + deviceId);
            }

            // Write value to device through custom driver service
            driverCustomService.write(driverConfig, pointConfig, device, point, new WValue(value, point.getPointTypeFlag(), point.getBitwise(), point.getPointCode()));
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @Override
    public void write(DeviceCommandDTO commandDTO) {
        // Parse device write command from DTO content
        DeviceCommandDTO.DeviceWrite deviceWrite = JsonUtil.parseObject(commandDTO.getContent(), DeviceCommandDTO.DeviceWrite.class);
        if (Objects.isNull(deviceWrite)) {
            return;
        }

        // Log command execution start
        log.info("Start command of write: {}", JsonUtil.toJsonString(commandDTO));
        // Execute write operation
        write(deviceWrite.getDeviceId(), deviceWrite.getPointId(), deviceWrite.getValue());
        // Log command execution end
        log.info("End command of write: write");
    }

}
