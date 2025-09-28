package com.aiforest.tmiot.common.manager.grpc.builder;

import com.aiforest.tmiot.api.center.manager.*;
import com.aiforest.tmiot.common.manager.entity.bo.*;
import com.aiforest.tmiot.common.manager.entity.query.CusNavOverviewQuery;
import com.aiforest.tmiot.common.optional.EnableOptional;
import com.aiforest.tmiot.common.utils.GrpcBuilderUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * 自定导航总览BO与GRPC DTO转换器
 *
 * @author way
 * @since 2025.09.02
 */
@Mapper(componentModel = "spring", uses = {GrpcBuilderUtil.class})
public interface GrpcNavOverviewBuilder {

    // ------------------------------ GRPC请求转Query ------------------------------
    /**
     * 单个总览查询请求 -> 查询条件
     */
    @Mapping(target = "page", ignore = true)
    @Mapping(target = "navName", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    CusNavOverviewQuery buildQueryBySingleRequest(GrpcCusNavOverviewQuery request);

    /**
     * 分页总览查询请求 -> 查询条件
     */
    @Mapping(target = "overviewId", ignore = true)
    CusNavOverviewQuery buildQueryByPageRequest(GrpcPageCusNavOverviewQuery request);

    @AfterMapping
    default void afterProcess(GrpcPageCusNavOverviewQuery request, @MappingTarget CusNavOverviewQuery query) {
        // 处理分页参数
        query.setPage(GrpcBuilderUtil.buildPagesByGrpcPage(request.getPage()));
        // 处理可选参数（使能标识，默认-1）
        EnableOptional.ofNullable(request.getEnableFlag()).ifPresent(query::setEnableFlag);
    }

    // ------------------------------ BO转GRPC DTO ------------------------------
    /**
     * 坐标属性BO -> GRPC DTO
     */
    GrpcDeviceCoordinateAttributeDTO buildAttributeDTO(GrpcDeviceCoordinateAttributeBO bo);

    /**
     * 坐标绑定BO -> GRPC DTO（含关联属性）
     */
    @Mapping(target = "attribute", ignore = true) // 手动处理关联属性
    GrpcDeviceCoordinateBindDTO buildBindDTO(GrpcDeviceCoordinateBindBO bo);

    @AfterMapping
    default void afterProcess(GrpcDeviceCoordinateBindBO bo, @MappingTarget GrpcDeviceCoordinateBindDTO.Builder dtoBuilder) {
        // 转换关联的坐标属性BO -> DTO
        Optional.ofNullable(bo.getAttributeBO())
                .ifPresent(attributeBO -> dtoBuilder.setAttribute(buildAttributeDTO(attributeBO)));
    }

    /**
     * 导航节点BO -> GRPC DTO（含关联绑定列表）
     */
    @Mapping(target = "coordinateBind", ignore = true) // 手动处理关联绑定列表
    GrpcNavNodesDTO buildNodeDTO(GrpcNavNodesBO bo);

    @AfterMapping
    default void afterProcess(GrpcNavNodesBO bo, @MappingTarget GrpcNavNodesDTO.Builder dtoBuilder) {
        // 转换关联的坐标绑定列表BO -> DTO
        Optional.ofNullable(bo.getGrpcDeviceCoordinateBindBO())
                .ifPresent(boBindBO -> {dtoBuilder.setCoordinateBind(buildBindDTO(boBindBO));});
    }

    /**
     * 自定导航总览BO -> GRPC DTO（含关联节点列表）
     */
    @Mapping(target = "navNodesList", ignore = true) // 手动处理关联节点列表
    GrpcCusNavOverviewDTO buildOverviewDTO(GrpcCusNavOverviewBO bo);

    @AfterMapping
    default void afterProcess(GrpcCusNavOverviewBO bo, @MappingTarget GrpcCusNavOverviewDTO.Builder dtoBuilder) {
        // 转换关联的导航节点列表BO -> DTO
        Optional.ofNullable(bo.getGrpcNavNodesBOList())
                .ifPresent(coordinateBindBOList -> {
                    List<GrpcNavNodesDTO> grpcNavNodesDTOList = coordinateBindBOList.stream()
                            .map(this::buildNodeDTO)
                            .toList();
                    dtoBuilder.addAllNavNodes(grpcNavNodesDTOList);
                });
    }
}