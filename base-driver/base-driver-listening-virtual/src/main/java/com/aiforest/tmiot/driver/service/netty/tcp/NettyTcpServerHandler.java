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

package com.aiforest.tmiot.driver.service.netty.tcp;

import com.aiforest.tmiot.common.driver.service.DriverSenderService;
import com.aiforest.tmiot.common.enums.DeviceStatusEnum;
import com.aiforest.tmiot.driver.service.netty.NettyServerHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 报文处理, 需要视具体情况开发
 * 本驱动中使用报文(设备名称[22]+关键字[1]+海拔[4]+速度[8]+液位[8]+方向[4]+锁定[1]+经纬[21])进行测试使用
 * 4C 69 73 74 65 6E 69 6E 67 56 69 72 74 75 61 6C 44 65 76 69 63 65
 * 62
 * 44 C3 E7 5C
 * 40 46 D5 C2 8F 5C 28 F6
 * 00 00 00 00 00 00 00 0C
 * 00 00 00 2D
 * 01
 * 31 33 31 2E 32 33 31 34 35 36 2C 30 32 31 2E 35 36 38 32 31 31
 * <p>
 * 使用 sokit 发送以下报文
 * lg:[4C 69 73 74 65 6E 69 6E 67 56 69 72 74 75 61 6C 44 65 76 69 63 65 62 44 C3 E7 5C 40 46 D5 C2 8F 5C 28 F6 00 00 00 00 00 00 00 0C 00 00 00 2D 01 31 33 31 2E 32 33 31 34 35 36 2C 30 32 31 2E 35 36 38 32 31 31]
 *
 * @author way
 * @version 2025.7.27
 * @since 2022.1.0
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class NettyTcpServerHandler extends ChannelInboundHandlerAdapter {
//    private static NettyTcpServerHandler nettyTcpServerHandler;
//    @Resource
//    private NettyServerHandler nettyServerHandler;
//    // 注入DriverSenderService，用于发送离线状态（原有驱动已注入，直接复用）
//    @Resource
//    private DriverSenderService driverSenderService;

    // 2. 声明成员变量，使用final修饰（构造方法注入后不可修改，保证线程安全）
    private final NettyServerHandler nettyServerHandler;
    private final DriverSenderService driverSenderService;
    // 3. 构造方法注入依赖（@Resource 注解标注构造方法，Spring自动装配，确保依赖非null）
    @Autowired
    public NettyTcpServerHandler(NettyServerHandler nettyServerHandler, DriverSenderService driverSenderService) {
        this.nettyServerHandler = nettyServerHandler;
        this.driverSenderService = driverSenderService;
    }
//    @PostConstruct
//    public void init() {
//        nettyTcpServerHandler = this;
//    }

    @Override
    @SneakyThrows
    public void channelRead(ChannelHandlerContext context, Object msg) {
//        nettyTcpServerHandler.nettyServerHandler.read(context, (ByteBuf) msg);
        // 直接使用成员变量nettyServerHandler，无需静态引用
        this.nettyServerHandler.read(context, (ByteBuf) msg);
    }

    // 新增：处理空闲事件（核心方法）
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断是否为IdleStateEvent（空闲事件）
        if (evt instanceof IdleStateEvent idleStateEvent) {
            // 只处理读空闲事件（长时间未收到数据）
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                log.warn("通道读空闲超时，设备即将标记为离线：{}", ctx.channel().remoteAddress());
                // 1. 根据通道查找关联的设备ID
                Long deviceId = getDeviceIdByChannel(ctx.channel());
                if (deviceId != null) {
                    // 2. 移除设备与通道的映射关系
                    NettyTcpServer.deviceChannelMap.remove(deviceId);
                    // 3. 发送设备离线状态给SDK
                    driverSenderService.deviceStatusSender(deviceId, DeviceStatusEnum.OFFLINE);
                    log.info("设备{}已标记为离线（读空闲超时）", deviceId);
                }
                // 4. 关闭通道（可选，根据业务需求决定是否关闭）
                ctx.close();
            }
        } else {
            // 非空闲事件，执行父类逻辑
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    @SneakyThrows
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) {
//        log.debug(throwable.getMessage());
//        context.disconnect();
        log.error("通道异常：{}", throwable.getMessage(), throwable);
        // 异常时：清理设备-通道映射，标记设备离线
        Long deviceId = getDeviceIdByChannel(context.channel());
        if (deviceId != null) {
            NettyTcpServer.deviceChannelMap.remove(deviceId);
            driverSenderService.deviceStatusSender(deviceId, DeviceStatusEnum.OFFLINE);
            log.info("设备{}已标记为离线（通道异常）", deviceId);
        }
        context.close();
    }

    // 新增：根据通道反向查找设备ID（关键辅助方法）
    private Long getDeviceIdByChannel(Channel channel) {
        // 遍历deviceChannelMap，根据通道查找对应的设备ID
        for (Map.Entry<Long, Channel> entry : NettyTcpServer.deviceChannelMap.entrySet()) {
            if (entry.getValue().equals(channel)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // 新增：通道断开时的处理（可选，增强健壮性）
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("通道断开：{}", ctx.channel().remoteAddress());
        // 通道断开时：清理映射，标记设备离线
        Long deviceId = getDeviceIdByChannel(ctx.channel());
        if (deviceId != null) {
            NettyTcpServer.deviceChannelMap.remove(deviceId);
            driverSenderService.deviceStatusSender(deviceId, DeviceStatusEnum.OFFLINE);
            log.info("设备{}已标记为离线（通道断开）", deviceId);
        }
        super.channelInactive(ctx);
    }


}