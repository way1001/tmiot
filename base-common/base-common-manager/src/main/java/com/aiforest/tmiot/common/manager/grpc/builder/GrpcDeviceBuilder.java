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

import com.aiforest.tmiot.api.center.manager.GrpcPageDeviceQuery;
import com.aiforest.tmiot.api.common.GrpcBase;
import com.aiforest.tmiot.api.common.GrpcDeviceDTO;
import com.aiforest.tmiot.api.common.GrpcDevicePointsDTO;
import com.aiforest.tmiot.api.common.GrpcPointDTO;
import com.aiforest.tmiot.common.constant.common.DefaultConstant;
import com.aiforest.tmiot.common.entity.common.Pages;
import com.aiforest.tmiot.common.manager.entity.bo.DeviceBO;
import com.aiforest.tmiot.common.manager.entity.bo.DevicePointLatestBO;
import com.aiforest.tmiot.common.manager.entity.query.DeviceQuery;
import com.aiforest.tmiot.common.optional.CollectionOptional;
import com.aiforest.tmiot.common.optional.EnableOptional;
import com.aiforest.tmiot.common.utils.GrpcBuilderUtil;
import com.aiforest.tmiot.common.utils.JsonUtil;
import com.aiforest.tmiot.common.utils.MapStructUtil;
import org.mapstruct.*;

import java.util.List;
import java.util.Optional;

/**
 * GrpcDevice Builder
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface GrpcDeviceBuilder {

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDeviceQuery
     * @return DeviceQuery
     */
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    DeviceQuery buildQueryByGrpcQuery(GrpcPageDeviceQuery entityQuery);

    @AfterMapping
    default void afterProcess(GrpcPageDeviceQuery entityGrpc, @MappingTarget DeviceQuery.DeviceQueryBuilder entityQuery) {
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        entityQuery.page(pages);

        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityQuery::enableFlag);
    }

    /**
     * Grpc Query to Query
     *
     * @param entityQuery GrpcPageDeviceQuery
     * @return DeviceQuery
     */
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "deviceName", ignore = true)
    @Mapping(target = "deviceCode", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "profileId", ignore = true)
    DeviceQuery buildQueryByGrpcQuery(com.aiforest.tmiot.api.common.driver.GrpcPageDeviceQuery entityQuery);

    @AfterMapping
    default void afterProcess(com.aiforest.tmiot.api.common.driver.GrpcPageDeviceQuery entityGrpc, @MappingTarget DeviceQuery.DeviceQueryBuilder entityQuery) {
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        entityQuery.page(pages);
    }

    /**
     * BO to Grpc DTO
     *
     * @param entityBO DeviceBO
     * @return GrpcDeviceDTO
     */
    @Mapping(target = "deviceExt", ignore = true)
    @Mapping(target = "profileIdsList", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deviceNameBytes", ignore = true)
    @Mapping(target = "deviceCodeBytes", ignore = true)
    @Mapping(target = "deviceExtBytes", ignore = true)
    @Mapping(target = "signatureBytes", ignore = true)
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "base", ignore = true)
    @Mapping(target = "mergeBase", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    GrpcDeviceDTO buildGrpcDTOByBO(DeviceBO entityBO);

    @AfterMapping
    default void afterProcess(DeviceBO entityBO, @MappingTarget GrpcDeviceDTO.Builder entityGrpc) {
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        entityGrpc.setBase(grpcBase);

        CollectionOptional.ofNullable(entityBO.getProfileIds()).ifPresent(entityGrpc::addAllProfileIds);
        Optional.ofNullable(entityBO.getDeviceExt()).ifPresent(value -> entityGrpc.setDeviceExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getEnableFlag()).ifPresentOrElse(value -> entityGrpc.setEnableFlag(value.getIndex()), () -> entityGrpc.setEnableFlag(DefaultConstant.DEFAULT_INT));
    }


////    @Mapping(target = "pointsList", ignore = true)   // 手写填充
////    @Mapping(target = "removePoints", ignore = true)
////    @Mapping(target = "pointsOrBuilderList", ignore = true)
////    @Mapping(target = "pointsBuilderList", ignore = true)
//    @Mapping(target = "points", ignore = true)
//    @Mapping(target = "deviceExt", ignore = true)
//    @Mapping(target = "enableFlag", ignore = true)
//    // 忽略所有 protobuf 生成的内部字段
//    @Mapping(target = "deviceNameBytes", ignore = true)
//    @Mapping(target = "deviceCodeBytes", ignore = true)
//    @Mapping(target = "deviceExtBytes", ignore = true)
//    @Mapping(target = "signatureBytes", ignore = true)
//    @Mapping(target = "mergeFrom", ignore = true)
//    @Mapping(target = "clearField", ignore = true)
//    @Mapping(target = "clearOneof", ignore = true)
//    @Mapping(target = "unknownFields", ignore = true)
//    @Mapping(target = "mergeUnknownFields", ignore = true)
//    @Mapping(target = "allFields", ignore = true)
//    GrpcDevicePointsDTO buildGrpcDPDTOByBO(DevicePointLatestBO bo);
//
//    @AfterMapping
//    default void afterProcess(DevicePointLatestBO bo,
//                              @MappingTarget GrpcDevicePointsDTO.Builder dtoBuilder,
//                              @Context GrpcPointBuilder pointBuilder) {
//
//        List<GrpcPointDTO> grpcPoints = Optional.ofNullable(bo.getPoints())
//                .orElse(List.of())
//                .stream()
//                .map(pointBuilder::buildGrpcDTOByBO)
//                .peek(dto -> System.out.println("Converted DTO: " + dto))
//                .toList();
//        dtoBuilder.addAllPoints(grpcPoints);
//
//        /* 1. 基本字段 */
////        dtoBuilder
////                .setId(bo.getId())
////                .setDeviceName(bo.getDeviceName())
////                .setDeviceCode(bo.getDeviceCode())
////                .setDriverId(bo.getDriverId())
////                .setTenantId(bo.getTenantId())
////                .setVersion(bo.getVersion())
////                .setSignature(bo.getSignature());
//        /* 2. 枚举 / JSON 扩展 */
////        Optional.ofNullable(bo.getDeviceExt())
////                .ifPresent(ext -> dtoBuilder.setDeviceExt(JsonUtil.toJsonString(ext)));
////        Optional.ofNullable(bo.getEnableFlag())
////                .ifPresent(e -> dtoBuilder.setEnableFlag(e.getIndex()));
////
////        List<GrpcPointDTO> grpcPoints = Optional.ofNullable(bo.getPoints())
////                .orElse(List.of())
////                .stream()
////                .map(pointBuilder::buildGrpcDTOByBO)
////                .peek(dto -> System.out.println("Converted DTO: " + dto))
////                .toList();
////        dtoBuilder.addAllPoints(grpcPoints);
//    }

}
