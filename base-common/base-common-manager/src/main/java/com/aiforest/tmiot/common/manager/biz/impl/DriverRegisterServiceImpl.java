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

package com.aiforest.tmiot.common.manager.biz.impl;

import com.aiforest.tmiot.api.center.auth.GrpcCodeQuery;
import com.aiforest.tmiot.api.center.auth.GrpcRTenantDTO;
import com.aiforest.tmiot.api.center.auth.TenantApiGrpc;
import com.aiforest.tmiot.api.common.GrpcDriverAttributeDTO;
import com.aiforest.tmiot.api.common.GrpcPointAttributeDTO;
import com.aiforest.tmiot.api.common.driver.GrpcDriverRegisterDTO;
import com.aiforest.tmiot.common.constant.service.AuthConstant;
import com.aiforest.tmiot.common.exception.ServiceException;
import com.aiforest.tmiot.common.manager.biz.DriverRegisterService;
import com.aiforest.tmiot.common.manager.entity.bo.DriverAttributeBO;
import com.aiforest.tmiot.common.manager.entity.bo.DriverBO;
import com.aiforest.tmiot.common.manager.entity.bo.PointAttributeBO;
import com.aiforest.tmiot.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import com.aiforest.tmiot.common.manager.grpc.builder.GrpcDriverBuilder;
import com.aiforest.tmiot.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import com.aiforest.tmiot.common.manager.service.DriverAttributeService;
import com.aiforest.tmiot.common.manager.service.DriverService;
import com.aiforest.tmiot.common.manager.service.PointAttributeService;
import com.aiforest.tmiot.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 驱动同步相关接口实现
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverRegisterServiceImpl implements DriverRegisterService {

    private final GrpcDriverBuilder grpcDriverBuilder;
    private final GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;
    private final GrpcPointAttributeBuilder grpcPointAttributeBuilder;
    private final DriverService driverService;
    private final DriverAttributeService driverAttributeService;
    private final PointAttributeService pointAttributeService;
    @GrpcClient(AuthConstant.SERVICE_NAME)
    private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;

    public DriverRegisterServiceImpl(GrpcDriverBuilder grpcDriverBuilder, GrpcDriverAttributeBuilder grpcDriverAttributeBuilder, GrpcPointAttributeBuilder grpcPointAttributeBuilder, DriverService driverService, DriverAttributeService driverAttributeService, PointAttributeService pointAttributeService) {
        this.grpcDriverBuilder = grpcDriverBuilder;
        this.grpcDriverAttributeBuilder = grpcDriverAttributeBuilder;
        this.grpcPointAttributeBuilder = grpcPointAttributeBuilder;
        this.driverService = driverService;
        this.driverAttributeService = driverAttributeService;
        this.pointAttributeService = pointAttributeService;
    }

    @Override
    public DriverBO registerDriver(GrpcDriverRegisterDTO entityGrpc) {
        GrpcRTenantDTO rTenantDTO = tenantApiBlockingStub.selectByCode(GrpcCodeQuery.newBuilder().setCode(entityGrpc.getTenant()).build());
        if (!rTenantDTO.getResult().getOk()) {
            throw new ServiceException("Tenant[{}] information is invalid: {}", entityGrpc.getTenant(), rTenantDTO.getResult().getMessage());
        }

        DriverBO driverBO = grpcDriverBuilder.buildBOByGrpcDTO(entityGrpc.getDriver());
        Objects.requireNonNull(driverBO).setTenantId(rTenantDTO.getData().getBase().getId());
        DriverBO entityBO = driverService.selectByServiceName(driverBO.getServiceName(), driverBO.getTenantId());
        if (Objects.nonNull(entityBO)) {
            log.info("The driver has been registered, perform update: {}", JsonUtil.toJsonString(driverBO));
            driverBO.setId(entityBO.getId());
            driverService.update(driverBO);
        } else {
            log.info("The driver is not registered, perform new addition: {}", JsonUtil.toJsonString(driverBO));
            driverService.save(driverBO);
        }

        return driverService.selectByServiceName(driverBO.getServiceName(), driverBO.getTenantId());
    }

    @Override
    public List<DriverAttributeBO> registerDriverAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO) {
        Map<String, DriverAttributeBO> newDriverAttributeMap = entityGrpc.getDriverAttributesList().stream()
                .collect(Collectors.toMap(GrpcDriverAttributeDTO::getAttributeCode, grpcDriverAttributeBuilder::buildBOByGrpcDTO));

        Map<String, DriverAttributeBO> oldDriverAttributeMap = driverAttributeService.selectByDriverId(entityBO.getId()).stream()
                .collect(Collectors.toMap(DriverAttributeBO::getAttributeCode, Function.identity()));

        for (Map.Entry<String, DriverAttributeBO> entry : newDriverAttributeMap.entrySet()) {
            String name = entry.getKey();
            DriverAttributeBO attribute = newDriverAttributeMap.get(name);
            attribute.setDriverId(entityBO.getId());
            if (oldDriverAttributeMap.containsKey(name)) {
                log.debug("The driver attributes have been registered, update is performed: {}", JsonUtil.toJsonString(attribute));
                attribute.setId(oldDriverAttributeMap.get(name).getId());
                driverAttributeService.update(attribute);
            } else {
                log.debug("The driver attributes are not registered, perform new addition: {}", JsonUtil.toJsonString(attribute));
                driverAttributeService.save(attribute);
            }
        }

        for (Map.Entry<String, DriverAttributeBO> entry : oldDriverAttributeMap.entrySet()) {
            String name = entry.getKey();
            if (!newDriverAttributeMap.containsKey(name)) {
                log.debug("Driver attribute is redundant, deleting: {}", oldDriverAttributeMap.get(name));
                driverAttributeService.remove(oldDriverAttributeMap.get(name).getId());
            }
        }

        return driverAttributeService.selectByDriverId(entityBO.getId());
    }

    @Override
    public List<PointAttributeBO> registerPointAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO) {
        Map<String, PointAttributeBO> newPointAttributeMap = entityGrpc.getPointAttributesList().stream()
                .collect(Collectors.toMap(GrpcPointAttributeDTO::getAttributeCode, grpcPointAttributeBuilder::buildBOByGrpcDTO));

        Map<String, PointAttributeBO> oldPointAttributeMap = pointAttributeService.selectByDriverId(entityBO.getId()).stream()
                .collect(Collectors.toMap(PointAttributeBO::getAttributeCode, Function.identity()));

        for (Map.Entry<String, PointAttributeBO> entry : newPointAttributeMap.entrySet()) {
            String name = entry.getKey();
            PointAttributeBO attribute = newPointAttributeMap.get(name);
            attribute.setDriverId(entityBO.getId());
            if (oldPointAttributeMap.containsKey(name)) {
                log.debug("The point attribute has been registered, update is performed: {}", JsonUtil.toJsonString(attribute));
                attribute.setId(oldPointAttributeMap.get(name).getId());
                pointAttributeService.update(attribute);
            } else {
                log.debug("The point attribute is not registered, perform update: {}", JsonUtil.toJsonString(attribute));
                pointAttributeService.save(attribute);
            }
        }

        for (Map.Entry<String, PointAttributeBO> entry : oldPointAttributeMap.entrySet()) {
            String name = entry.getKey();
            if (!newPointAttributeMap.containsKey(name)) {
                log.debug("Point attribute is redundant, deleting: {}", oldPointAttributeMap.get(name));
                pointAttributeService.remove(oldPointAttributeMap.get(name).getId());
            }
        }

        return pointAttributeService.selectByDriverId(entityBO.getId());
    }

}
