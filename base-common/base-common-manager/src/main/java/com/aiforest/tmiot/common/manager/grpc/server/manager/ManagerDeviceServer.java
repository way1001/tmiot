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

package com.aiforest.tmiot.common.manager.grpc.server.manager;


import cn.hutool.core.collection.CollUtil;
import com.aiforest.tmiot.api.common.GrpcDevicePointsDTO;
import com.aiforest.tmiot.common.manager.entity.bo.DevicePointLatestBO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aiforest.tmiot.api.center.manager.*;
import com.aiforest.tmiot.api.common.GrpcDeviceDTO;
import com.aiforest.tmiot.api.common.GrpcPage;
import com.aiforest.tmiot.api.common.GrpcR;
import com.aiforest.tmiot.common.enums.ResponseEnum;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceBO;
import com.aiforest.tmiot.common.manager.entity.query.DeviceQuery;
import com.aiforest.tmiot.common.manager.grpc.builder.GrpcDeviceBuilder;
import  com.aiforest.tmiot.common.manager.grpc.builder.GrpcDevicePointsBuilder;
import com.aiforest.tmiot.common.manager.service.DeviceService;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Objects;

/**
 * 设备 Api
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class ManagerDeviceServer extends DeviceApiGrpc.DeviceApiImplBase {

    @Resource
    private GrpcDeviceBuilder grpcDeviceBuilder;

    @Resource
    private GrpcDevicePointsBuilder grpcDevicePointsBuilder;

    @Resource
    private DeviceService deviceService;

    @Override
    public void selectByPage(GrpcPageDeviceQuery request, StreamObserver<GrpcRPageDeviceDTO> responseObserver) {
        GrpcRPageDeviceDTO.Builder builder = GrpcRPageDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceQuery query = grpcDeviceBuilder.buildQueryByGrpcQuery(request);

        Page<DeviceBO> entityPage = deviceService.selectByPage(query);
        if (Objects.isNull(entityPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPageDeviceDTO.Builder pageBuilder = GrpcPageDeviceDTO.newBuilder();
            GrpcPage.Builder page = GrpcPage.newBuilder();
            page.setCurrent(entityPage.getCurrent());
            page.setSize(entityPage.getSize());
            page.setPages(entityPage.getPages());
            page.setTotal(entityPage.getTotal());
            pageBuilder.setPage(page);

            List<GrpcDeviceDTO> entityGrpcDTOList = entityPage.getRecords().stream().map(grpcDeviceBuilder::buildGrpcDTOByBO).toList();
            pageBuilder.addAllData(entityGrpcDTOList);

            builder.setData(pageBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByLatestPage(GrpcPageDeviceQuery request, StreamObserver<GrpcRPageDevicePointsDTO> responseObserver) {
        GrpcRPageDevicePointsDTO.Builder builder = GrpcRPageDevicePointsDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceQuery query = grpcDeviceBuilder.buildQueryByGrpcQuery(request);

        Page<DevicePointLatestBO> entityPage = deviceService.selectByLatestPage(query);
        if (Objects.isNull(entityPage)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            GrpcPageDevicePointsDTO.Builder pageBuilder = GrpcPageDevicePointsDTO.newBuilder();
            GrpcPage.Builder page = GrpcPage.newBuilder();
            page.setCurrent(entityPage.getCurrent());
            page.setSize(entityPage.getSize());
            page.setPages(entityPage.getPages());
            page.setTotal(entityPage.getTotal());
            pageBuilder.setPage(page);

            List<GrpcDevicePointsDTO> entityGrpcDTOList = entityPage.getRecords().stream().map(grpcDevicePointsBuilder::buildGrpcDPDTOByBO).toList();
            pageBuilder.addAllData(entityGrpcDTOList);

            builder.setData(pageBuilder);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByDriverId(GrpcDriverQuery driver, StreamObserver<GrpcRDeviceListDTO> responseObserver) {
        GrpcRDeviceListDTO.Builder builder = GrpcRDeviceListDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        List<DeviceBO> entityBOList = deviceService.selectByDriverId(driver.getDriverId());
        if (CollUtil.isEmpty(entityBOList)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            List<GrpcDeviceDTO> entityGrpcDTOList = entityBOList.stream().map(grpcDeviceBuilder::buildGrpcDTOByBO).toList();

            builder.addAllData(entityGrpcDTOList);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByProfileId(GrpcProfileQuery request, StreamObserver<GrpcRDeviceListDTO> responseObserver) {
        GrpcRDeviceListDTO.Builder builder = GrpcRDeviceListDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        List<DeviceBO> entityBOList = deviceService.selectByProfileId(request.getProfileId());
        if (CollUtil.isEmpty(entityBOList)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            List<GrpcDeviceDTO> entityGrpcDTOList = entityBOList.stream().map(grpcDeviceBuilder::buildGrpcDTOByBO).toList();

            builder.addAllData(entityGrpcDTOList);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByDeviceId(GrpcDeviceQuery request, StreamObserver<GrpcRDeviceDTO> responseObserver) {
        GrpcRDeviceDTO.Builder builder = GrpcRDeviceDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        DeviceBO entityBO = deviceService.selectById(request.getDeviceId());
        if (Objects.isNull(entityBO)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getText());

            builder.setData(grpcDeviceBuilder.buildGrpcDTOByBO(entityBO));
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
