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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author way
 * @version 2025.7.27
 * @since 2022.1.0
 */
@Component
public class NettyTcpServer {
    /**
     * DeviceId:Channel
     * 用于存放设备的 Netty Context Channel
     * !!! 提示: 此处逻辑仅供参考, 请务必结合实际应用场景。!!!
     */
    public static final Map<Long, Channel> deviceChannelMap = new ConcurrentHashMap<>(16);

    // 注入Spring托管的NettyTcpServerHandler Bean
    @Resource
    private NettyTcpServerHandler nettyTcpServerHandler;

    @SneakyThrows
    public void start(int port) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new StringEncoder())
                                    .addLast(new ByteArrayEncoder())
                                    // 新增：添加空闲检测处理器
                                    // 参数说明：readerIdleTime(读空闲时间), writerIdleTime(写空闲时间), allIdleTime(读写空闲时间), 时间单位
                                    // 示例：30秒未收到数据则触发读空闲事件（可根据业务调整时间）
                                    .addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new WriteTimeoutHandler(30), nettyTcpServerHandler);
                        }
                    });
            ChannelFuture future = bootstrap.bind().sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}