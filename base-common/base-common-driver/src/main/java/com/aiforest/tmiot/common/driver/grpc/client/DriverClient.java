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

package com.aiforest.tmiot.common.driver.grpc.client;

import com.aiforest.tmiot.api.common.GrpcDriverAttributeDTO;
import com.aiforest.tmiot.api.common.GrpcDriverDTO;
import com.aiforest.tmiot.api.common.GrpcPointAttributeDTO;
import com.aiforest.tmiot.api.common.driver.DriverApiGrpc;
import com.aiforest.tmiot.api.common.driver.GrpcDriverRegisterDTO;
import com.aiforest.tmiot.api.common.driver.GrpcRDriverRegisterDTO;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.driver.entity.bo.DriverBO;
import com.aiforest.tmiot.common.driver.entity.bo.RegisterBO;
import com.aiforest.tmiot.common.driver.entity.builder.DriverBuilder;
import com.aiforest.tmiot.common.driver.entity.builder.GrpcDriverAttributeBuilder;
import com.aiforest.tmiot.common.driver.entity.builder.GrpcPointAttributeBuilder;
import com.aiforest.tmiot.common.driver.entity.dto.DriverAttributeDTO;
import com.aiforest.tmiot.common.driver.entity.dto.PointAttributeDTO;
import com.aiforest.tmiot.common.driver.metadata.DriverMetadata;
import com.aiforest.tmiot.common.enums.DriverStatusEnum;
import com.aiforest.tmiot.common.exception.RegisterException;
import com.aiforest.tmiot.common.optional.CollectionOptional;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DriverClient {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private DriverMetadata driverMetadata;

    @Resource
    private DriverBuilder driverBuilder;
    @Resource
    private GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;
    @Resource
    private GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    /**
     * Register driver with metadata
     *
     * @param entityBO DriverRegisterBO containing driver registration information
     */
    public void driverRegister(RegisterBO entityBO) {
        // Build driver registration information
        GrpcDriverRegisterDTO.Builder builder = GrpcDriverRegisterDTO.newBuilder();
        GrpcDriverDTO grpcDriverDTO = driverBuilder.buildGrpcDTOByDTO(entityBO.getDriver());
        builder.setTenant(entityBO.getTenant())
                .setClient(entityBO.getClient())
                .setDriver(grpcDriverDTO);

        CollectionOptional.ofNullable(entityBO.getDriverAttributes()).ifPresent(value -> {
                    List<GrpcDriverAttributeDTO> grpcDriverAttributeDTOList = value.stream().map(grpcDriverAttributeBuilder::buildGrpcDTOByDTO).toList();
                    builder.addAllDriverAttributes(grpcDriverAttributeDTOList);
                }
        );
        CollectionOptional.ofNullable(entityBO.getPointAttributes()).ifPresent(value -> {
                    List<GrpcPointAttributeDTO> grpcPointAttributeDTOList = value.stream().map(grpcPointAttributeBuilder::buildGrpcDTOByDTO).toList();
                    builder.addAllPointAttributes(grpcPointAttributeDTOList);
                }
        );

        // Initiate driver registration
        GrpcRDriverRegisterDTO rDriverRegisterDTO = driverApiBlockingStub.driverRegister(builder.build());
        if (!rDriverRegisterDTO.getResult().getOk()) {
            throw new RegisterException(rDriverRegisterDTO.getResult().getMessage());
        }

        DriverBO driverBO = driverBuilder.buildDTOByGrpcDTO(rDriverRegisterDTO.getDriver());
        driverMetadata.setDriver(driverBO);

        driverMetadata.setDeviceIds(new HashSet<>(rDriverRegisterDTO.getDeviceIdsList()));

        List<GrpcDriverAttributeDTO> driverAttributesList = rDriverRegisterDTO.getDriverAttributesList();
        Map<Long, DriverAttributeDTO> driverAttributeIdMap = driverAttributesList.stream().collect(Collectors.toMap(entity -> entity.getBase().getId(), grpcDriverAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, DriverAttributeDTO> driverAttributeNameMap = driverAttributesList.stream().collect(Collectors.toMap(GrpcDriverAttributeDTO::getAttributeCode, grpcDriverAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setDriverAttributeIdMap(driverAttributeIdMap);
        driverMetadata.setDriverAttributeNameMap(driverAttributeNameMap);

        List<GrpcPointAttributeDTO> pointAttributesList = rDriverRegisterDTO.getPointAttributesList();
        Map<Long, PointAttributeDTO> pointAttributeIdMap = pointAttributesList.stream().collect(Collectors.toMap(entity -> entity.getBase().getId(), grpcPointAttributeBuilder::buildDTOByGrpcDTO));
        Map<String, PointAttributeDTO> pointAttributeNameMap = pointAttributesList.stream().collect(Collectors.toMap(GrpcPointAttributeDTO::getAttributeCode, grpcPointAttributeBuilder::buildDTOByGrpcDTO));
        driverMetadata.setPointAttributeIdMap(pointAttributeIdMap);
        driverMetadata.setPointAttributeNameMap(pointAttributeNameMap);

        driverMetadata.setDriverStatus(DriverStatusEnum.ONLINE);
    }
}
