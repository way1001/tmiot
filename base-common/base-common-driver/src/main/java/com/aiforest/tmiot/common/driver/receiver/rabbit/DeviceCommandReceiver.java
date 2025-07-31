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

package com.aiforest.tmiot.common.driver.receiver.rabbit;

import cn.hutool.core.text.CharSequenceUtil;
import com.rabbitmq.client.Channel;
import com.aiforest.tmiot.common.driver.service.DriverReadService;
import com.aiforest.tmiot.common.driver.service.DriverWriteService;
import com.aiforest.tmiot.common.entity.dto.DeviceCommandDTO;
import com.aiforest.tmiot.common.utils.JsonUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 接收设备指令
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class DeviceCommandReceiver {

    @Resource
    private DriverReadService driverReadService;
    @Resource
    private DriverWriteService driverWriteService;

    /**
     * Receive and process device commands from RabbitMQ queue
     *
     * @param channel   RabbitMQ channel for message acknowledgment
     * @param message   Raw RabbitMQ message containing delivery information
     * @param entityDTO Device command data transfer object containing command details
     */
    @RabbitHandler
    @RabbitListener(queues = "#{deviceCommandQueue.name}")
    public void deviceCommandReceive(Channel channel, Message message, DeviceCommandDTO entityDTO) {
        try {
            // Acknowledge message receipt
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
            log.info("Receive device command: {}", JsonUtil.toJsonString(entityDTO));

            // Validate command data
            if (Objects.isNull(entityDTO)
                    || Objects.isNull(entityDTO.getType())
                    || CharSequenceUtil.isEmpty(entityDTO.getContent())) {
                log.error("Invalid device command: {}", entityDTO);
                return;
            }

            // Process command based on type
            switch (entityDTO.getType()) {
                case READ:
                    // Execute read operation
                    driverReadService.read(entityDTO);
                    break;
                case WRITE:
                    // Execute write operation
                    driverWriteService.write(entityDTO);
                    break;
                case CONFIG:
                    // to do something
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
