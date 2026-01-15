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

package com.aiforest.tmiot.driver.service.netty;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.CharsetUtil;
import com.aiforest.tmiot.common.driver.entity.bean.PointValue;
import com.aiforest.tmiot.common.driver.entity.bean.RValue;
import com.aiforest.tmiot.common.driver.entity.bo.AttributeBO;
import com.aiforest.tmiot.common.driver.entity.bo.DeviceBO;
import com.aiforest.tmiot.common.driver.entity.bo.PointBO;
import com.aiforest.tmiot.common.driver.metadata.DeviceMetadata;
import com.aiforest.tmiot.common.driver.metadata.PointMetadata;
import com.aiforest.tmiot.common.driver.service.DriverSenderService;
import com.aiforest.tmiot.driver.service.netty.tcp.NettyTcpServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author way
 * @version 2025.7.27
 * @since 2022.1.0
 */
@Slf4j
@Service
public class NettyServerHandler {

    @Resource
    private DeviceMetadata deviceMetadata;
    @Resource
    private PointMetadata pointMetadata;
    @Resource
    private DriverSenderService driverSenderService;

    /**
     * 例子, 仅供参考, 请结合自己的实际数据格式进行解析
     */
    public void read(ChannelHandlerContext context, ByteBuf byteBuf) {
        log.info("{}->{}", context.channel().remoteAddress(), ByteBufUtil.hexDump(byteBuf));
        String deviceName = byteBuf.toString(0, 19, CharsetUtil.CHARSET_ISO_8859_1);
        long deviceId = Long.parseLong(deviceName);
        DeviceBO device = deviceMetadata.getCache(deviceId);

        String hexKey = ByteBufUtil.hexDump(byteBuf, 19, 1);
        NettyTcpServer.deviceChannelMap.put(deviceId, context.channel());

        Map<Long, Map<String, AttributeBO>> pointConfigMap = deviceMetadata.getPointConfig(deviceId);

        List<PointValue> pointValues = new ArrayList<>(16);
        for (Map.Entry<Long, Map<String, AttributeBO>> entry : pointConfigMap.entrySet()) {
            PointBO point = pointMetadata.getCache(entry.getKey());
            Map<String, AttributeBO> infoMap = pointConfigMap.get(entry.getKey());
            int start = infoMap.get("start").getValue(Integer.class);
            int end = infoMap.get("end").getValue(Integer.class);

            if (infoMap.get("key").getValue().equals(hexKey)) {
                String value = switch (point.getPointCode()) {
                    case "ctrl_mode", "SWS1", "SRS1", "SRS2", "SWS2", "ef_status", "charging" -> String.valueOf(byteBuf.getByte(start));
                    case "run_state", "nav_mode", "battery_voltage", "blind_dist" -> String.valueOf(byteBuf.getShort(start));
                    case "current_tag" -> String.valueOf(byteBuf.getInt(start));
//                    case "nav_route" -> String.valueOf(byteBuf.getByte(start));
                    case "nav_route" -> byteBuf.toString(start, byteBuf.readableBytes() - start, CharsetUtil.CHARSET_ISO_8859_1).trim();
                    default -> CharSequenceUtil.EMPTY;
                };
//                String value = switch (point.getPointName()) {
//                    case "左轮速度" -> String.valueOf(byteBuf.getShort(start));
//                    case "右轮速度" -> String.valueOf(byteBuf.getShort(start));
////                    case "方向" -> String.valueOf(byteBuf.getInt(start));
//                    case "控制模式" -> String.valueOf(byteBuf.getByte(start));
//                    case "工作状态" -> String.valueOf(byteBuf.getShort(start));
//                    case "基础状态" -> String.valueOf(byteBuf.getByte(start));
//                    case "条筒电量" -> String.valueOf(byteBuf.getShort(start));
//                    case "充电到位" -> String.valueOf(byteBuf.getByte(start));
//                    case "补筒信号" -> String.valueOf(byteBuf.getByte(start));
//                    case "推筒完成" -> String.valueOf(byteBuf.getByte(start));
//                    case "待机标志" -> String.valueOf(byteBuf.getByte(start));
//                    case "满筒信号" -> String.valueOf(byteBuf.getByte(start));
//                    case "自清洁标志" -> String.valueOf(byteBuf.getByte(start));
//                    case "左出筒方向" -> String.valueOf(byteBuf.getByte(start));
//                    case "充电完成" -> String.valueOf(byteBuf.getByte(start));
//                    case "旋转计数" -> String.valueOf(byteBuf.getShort(start));
//                    case "当前坐标" -> String.valueOf(byteBuf.getInt(start));
//                    case "导航路径" -> String.valueOf(start);
//                    case "经纬" -> byteBuf.toString(start, end, CharsetUtil.CHARSET_ISO_8859_1).trim();
//                    default -> CharSequenceUtil.EMPTY;
//                };

                if (CharSequenceUtil.isNotEmpty(value) && !point.getPointTypeFlag().getCode().equals("string")) {
                    pointValues.add(new PointValue(new RValue(device, point, value)));
                } else {
                    pointValues.add(new PointValue(new RValue(device, point, value)));
                }
            }
        }

        driverSenderService.pointValueSender(pointValues);
    }


}
