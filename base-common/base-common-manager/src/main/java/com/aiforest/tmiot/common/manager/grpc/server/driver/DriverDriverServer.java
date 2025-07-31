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

package com.aiforest.tmiot.common.manager.grpc.server.driver;

import com.aiforest.tmiot.api.common.GrpcDriverAttributeDTO;
import com.aiforest.tmiot.api.common.GrpcDriverDTO;
import com.aiforest.tmiot.api.common.GrpcPointAttributeDTO;
import com.aiforest.tmiot.api.common.GrpcR;
import com.aiforest.tmiot.api.common.driver.DriverApiGrpc;
import com.aiforest.tmiot.api.common.driver.GrpcDriverRegisterDTO;
import com.aiforest.tmiot.api.common.driver.GrpcRDriverRegisterDTO;
import com.aiforest.tmiot.common.enums.ResponseEnum;
import com.aiforest.tmiot.common.manager.biz.DriverRegisterService;
import com.aiforest.tmiot.common.manager.entity.bo.DriverAttributeBO;
import com.aiforest.tmiot.common.manager.entity.bo.DriverBO;
import com.aiforest.tmiot.common.manager.entity.bo.PointAttributeBO;
import com.aiforest.tmiot.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import com.aiforest.tmiot.common.manager.grpc.builder.GrpcDriverBuilder;
import com.aiforest.tmiot.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import com.aiforest.tmiot.common.manager.service.DeviceService;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * 设备 Api
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class DriverDriverServer extends DriverApiGrpc.DriverApiImplBase {

    @Resource
    private GrpcDriverBuilder grpcDriverBuilder;
    @Resource
    private GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;
    @Resource
    private GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    @Resource
    private DriverRegisterService driverRegisterService;
    @Resource
    private DeviceService deviceService;

    @Override
    public void driverRegister(GrpcDriverRegisterDTO request, StreamObserver<GrpcRDriverRegisterDTO> responseObserver) {
        GrpcRDriverRegisterDTO.Builder builder = GrpcRDriverRegisterDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        try {
            // 注册驱动
            DriverBO entityBO = driverRegisterService.registerDriver(request);
            GrpcDriverDTO entityGrpcDTO = grpcDriverBuilder.buildGrpcDTOByBO(entityBO);
            builder.setDriver(entityGrpcDTO);

            // 注册驱动属性
            List<DriverAttributeBO> driverAttributeBOList = driverRegisterService.registerDriverAttribute(request, entityBO);
            List<GrpcDriverAttributeDTO> grpcDriverAttributeDTOList = driverAttributeBOList.stream().map(grpcDriverAttributeBuilder::buildGrpcDTOByBO).toList();
            builder.addAllDriverAttributes(grpcDriverAttributeDTOList);

            // 注册位号属性
            List<PointAttributeBO> pointAttributeBOList = driverRegisterService.registerPointAttribute(request, entityBO);
            List<GrpcPointAttributeDTO> grpcPointAttributeDTOList = pointAttributeBOList.stream().map(grpcPointAttributeBuilder::buildGrpcDTOByBO).toList();
            builder.addAllPointAttributes(grpcPointAttributeDTOList);

            // 查询驱动下设备
            List<Long> idList = deviceService.selectIdsByDriverId(entityBO.getId());
            builder.addAllDeviceIds(idList);

            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());
        } catch (Exception e) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.FAILURE.getCode());
            rBuilder.setMessage(e.getMessage());

            log.error(e.getMessage(), e);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
