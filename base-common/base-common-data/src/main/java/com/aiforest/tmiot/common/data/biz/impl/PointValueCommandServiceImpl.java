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

package com.aiforest.tmiot.common.data.biz.impl;

import com.aiforest.tmiot.api.center.manager.DriverApiGrpc;
import com.aiforest.tmiot.api.center.manager.GrpcDeviceQuery;
import com.aiforest.tmiot.api.center.manager.GrpcRDriverDTO;
import com.aiforest.tmiot.api.center.manager.PointApiGrpc;
import com.aiforest.tmiot.common.constant.driver.RabbitConstant;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.data.biz.PointValueCommandService;
import com.aiforest.tmiot.common.data.entity.vo.PointValueReadVO;
import com.aiforest.tmiot.common.data.entity.vo.PointValueWriteVO;
import com.aiforest.tmiot.common.entity.dto.DeviceCommandDTO;
import com.aiforest.tmiot.common.enums.DeviceCommandTypeEnum;
import com.aiforest.tmiot.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointValueCommandServiceImpl implements PointValueCommandService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private PointApiGrpc.PointApiBlockingStub pointApiBlockingStub;
    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void read(PointValueReadVO entityVO) {
        GrpcDeviceQuery.Builder builder = GrpcDeviceQuery.newBuilder();
        builder.setDeviceId(entityVO.getDeviceId());
        GrpcRDriverDTO rDriverDTO = driverApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            return;
        }

        DeviceCommandDTO.DeviceRead deviceRead = new DeviceCommandDTO.DeviceRead(entityVO.getDeviceId(), entityVO.getPointId());
        DeviceCommandDTO deviceCommandDTO = new DeviceCommandDTO(DeviceCommandTypeEnum.READ, JsonUtil.toJsonString(deviceRead));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND, RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + rDriverDTO.getData().getServiceName(), deviceCommandDTO);
    }

    @Override
    public void write(PointValueWriteVO entityVO) {
        GrpcDeviceQuery.Builder builder = GrpcDeviceQuery.newBuilder();
        builder.setDeviceId(entityVO.getDeviceId());
        GrpcRDriverDTO rDriverDTO = driverApiBlockingStub.selectByDeviceId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            return;
        }

        DeviceCommandDTO.DeviceWrite deviceWrite = new DeviceCommandDTO.DeviceWrite(entityVO.getDeviceId(), entityVO.getPointId(), entityVO.getValue());
        DeviceCommandDTO deviceCommandDTO = new DeviceCommandDTO(DeviceCommandTypeEnum.WRITE, JsonUtil.toJsonString(deviceWrite));
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_COMMAND, RabbitConstant.ROUTING_DEVICE_COMMAND_PREFIX + rDriverDTO.getData().getServiceName(), deviceCommandDTO);
    }
}
