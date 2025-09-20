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

package com.aiforest.tmiot.common.constant.driver;

import com.aiforest.tmiot.common.constant.common.ExceptionConstant;

/**
 * 消息 相关常量
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
public class RabbitConstant {

    // Arguments
    public static final String MESSAGE_TTL = "x-message-ttl";
    public static final String AUTO_DELETE = "x-auto-delete";
    public static final String ROUTING_REGISTER_UP_PREFIX = "tmiot.r.register.up.";
    public static final String ROUTING_REGISTER_DOWN_PREFIX = "tmiot.r.register.down.";
    public static final String ROUTING_DRIVER_EVENT_PREFIX = "tmiot.r.event.driver.";
    public static final String ROUTING_DEVICE_EVENT_PREFIX = "tmiot.r.event.device.";
    public static final String ROUTING_DRIVER_METADATA_PREFIX = "tmiot.r.metadata.driver.";
    public static final String ROUTING_DRIVER_COMMAND_PREFIX = "tmiot.r.command.driver.";
    public static final String ROUTING_DEVICE_COMMAND_PREFIX = "tmiot.r.command.device.";
    public static final String ROUTING_POINT_VALUE_PREFIX = "tmiot.r.value.point.";
    public static final String ROUTING_MQTT_PREFIX = "tmiot.r.mqtt.";
    // Register
    public static String TOPIC_EXCHANGE_REGISTER = "tmiot.e.register";
    public static String QUEUE_REGISTER_UP = "tmiot.q.register.up";
    public static String QUEUE_REGISTER_DOWN_PREFIX = "tmiot.q.register.down.";
    // Event
    public static String TOPIC_EXCHANGE_EVENT = "tmiot.e.event";
    public static String QUEUE_DRIVER_EVENT = "tmiot.q.event.driver";
    public static String QUEUE_DEVICE_EVENT = "tmiot.q.event.device";
    // Metadata
    public static String TOPIC_EXCHANGE_METADATA = "tmiot.e.metadata";
    public static String QUEUE_DRIVER_METADATA_PREFIX = "tmiot.q.metadata.driver.";
    // Command
    public static String TOPIC_EXCHANGE_COMMAND = "tmiot.e.command";
    public static String QUEUE_DRIVER_COMMAND_PREFIX = "tmiot.q.command.driver.";
    public static String QUEUE_DEVICE_COMMAND_PREFIX = "tmiot.q.command.device.";
    // Value
    public static String TOPIC_EXCHANGE_VALUE = "tmiot.e.value";
    public static String QUEUE_POINT_VALUE = "tmiot.q.value.point";

    // Mqtt
    public static String TOPIC_EXCHANGE_MQTT = "tmiot.e.mqtt";
    public static String QUEUE_MQTT = "tmiot.q.mqtt";

    private RabbitConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

}
