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

import com.aiforest.tmiot.common.driver.entity.bean.PointValue;
import com.aiforest.tmiot.common.driver.entity.bean.RValue;
import com.aiforest.tmiot.common.driver.entity.bo.AttributeBO;
import com.aiforest.tmiot.common.driver.entity.bo.DeviceBO;
import com.aiforest.tmiot.common.driver.entity.bo.PointBO;
import com.aiforest.tmiot.common.driver.metadata.DeviceMetadata;
import com.aiforest.tmiot.common.driver.metadata.PointMetadata;
import com.aiforest.tmiot.common.driver.service.DriverCustomService;
import com.aiforest.tmiot.common.driver.service.DriverReadService;
import com.aiforest.tmiot.common.driver.service.DriverSenderService;
import com.aiforest.tmiot.common.entity.dto.DeviceCommandDTO;
import com.aiforest.tmiot.common.exception.ReadPointException;
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
public class DriverReadServiceImpl implements DriverReadService {

    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private PointMetadata pointMetadata;
    @Resource
    private DriverSenderService driverSenderService;
    @Resource
    private DriverCustomService driverCustomService;

    @Override
    public void read(Long deviceId, Long pointId) {
        // Get device from metadata cache
        DeviceBO device = deviceMetadata.getCache(deviceId);
        if (Objects.isNull(device)) {
            throw new ReadPointException("Failed to read point value, device[{}] is null", deviceId);
        }

        // Check if device contains the specified point
        if (!device.getPointIds().contains(pointId)) {
            throw new ReadPointException("Failed to read point value, device[{}] not contained point[{}]", deviceId, pointId);
        }

        // Get driver and point configurations
        Map<String, AttributeBO> driverConfig = deviceMetadata.getDriverConfig(deviceId);
        Map<String, AttributeBO> pointConfig = deviceMetadata.getPointConfig(deviceId, pointId);

        // Get point from metadata cache
//        pointMetadata.loadCache(pointId);
        PointBO point = pointMetadata.getCache(pointId);
        if (Objects.isNull(point)) {
            throw new ReadPointException("Failed to read point value, point[{}] is null" + deviceId);
        }

        // Read point value using custom driver service
        RValue rValue = driverCustomService.read(driverConfig, pointConfig, device, point);
        if (Objects.isNull(rValue)) {
            throw new ReadPointException("Failed to read point value, point value is null");
        }

        // Send point value to message queue
        driverSenderService.pointValueSender(new PointValue(rValue));
    }

    @Override
    public void read(DeviceCommandDTO commandDTO) {
        // Parse device read command from command DTO
        // Deserialize the command content into DeviceRead object
        DeviceCommandDTO.DeviceRead deviceRead = JsonUtil.parseObject(commandDTO.getContent(), DeviceCommandDTO.DeviceRead.class);
        if (Objects.isNull(deviceRead)) {
            return;
        }

        // Execute read command and log the process
        // Log the start of command execution with command details
        log.info("Start command of read: {}", JsonUtil.toJsonString(commandDTO));
        // Call the read method with device and point IDs
        read(deviceRead.getDeviceId(), deviceRead.getPointId());
        // Log the completion of command execution
        log.info("End command of read");
    }

}
