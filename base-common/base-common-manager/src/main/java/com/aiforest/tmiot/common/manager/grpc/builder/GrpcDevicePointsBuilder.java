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

package com.aiforest.tmiot.common.manager.grpc.builder;

import com.aiforest.tmiot.api.common.GrpcDevicePointsDTO;
import com.aiforest.tmiot.common.manager.entity.bo.DevicePointLatestBO;
import com.aiforest.tmiot.common.manager.entity.bo.PointBO;
import com.aiforest.tmiot.common.utils.JsonUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Optional;

/**
 * GrpcDevicePoint Builder
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {GrpcPointBuilder.class})
public interface GrpcDevicePointsBuilder {

    /**
     * DevicePointLatestBO -> GrpcDevicePointsDTO
     *
     * @param bo DevicePointLatestBO
     * @return GrpcDevicePointsDTO
     */
    @Mapping(target = "deviceExt", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "pointsList", ignore = true)
    @Mapping(target = "deviceNameBytes", ignore = true)
    @Mapping(target = "deviceCodeBytes", ignore = true)
    @Mapping(target = "deviceExtBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcDevicePointsDTO buildGrpcDPDTOByBO(DevicePointLatestBO bo);

    @AfterMapping
    default void afterProcess(DevicePointLatestBO bo, @MappingTarget GrpcDevicePointsDTO.Builder builder) {

        // 2. 设备扩展信息
        Optional.ofNullable(bo.getDeviceExt())
                .ifPresent(ext -> builder.setDeviceExt(JsonUtil.toJsonString(ext)));

        // 3. enableFlag 枚举 -> int
        Optional.ofNullable(bo.getEnableFlag())
                .ifPresent(flag -> builder.setEnableFlag(flag.getIndex()));

        // 4. 点位列表：PointBO -> GrpcPointDTO
        List<PointBO> pointBOList = bo.getPoints();
        if (pointBOList != null && !pointBOList.isEmpty()) {
            // 借助已有的 GrpcPointBuilder 完成单个 PointBO -> GrpcPointDTO 的转换
            GrpcPointBuilder pointBuilder = Mappers.getMapper(GrpcPointBuilder.class);
            pointBOList.stream()
                    .map(pointBuilder::buildGrpcDTOByBO)
                    .forEach(builder::addPoints);
        }
    }
}