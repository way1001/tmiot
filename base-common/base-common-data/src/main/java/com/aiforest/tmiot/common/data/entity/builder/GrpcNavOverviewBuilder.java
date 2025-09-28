package com.aiforest.tmiot.common.data.entity.builder;

import com.aiforest.tmiot.api.center.manager.*;
import com.aiforest.tmiot.common.data.entity.bo.*;
import com.aiforest.tmiot.common.data.entity.query.CusNavOverviewQuery;
import com.aiforest.tmiot.common.utils.GrpcBuilderUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = GrpcBuilderUtil.class)
public interface GrpcNavOverviewBuilder {

    /* ---------- 请求：BO -> gRPC ---------- */
    @Mapping(target = "overviewId", source = "overviewId")
    @Mapping(target = "tenantId", source = "tenantId")
    GrpcCusNavOverviewQuery toSingleRequest(CusNavOverviewQuery q);

    @Mapping(target = "page", source = "page")
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "navName", source = "navName")
    @Mapping(target = "enableFlag", source = "enableFlag")
    GrpcPageCusNavOverviewQuery toPageRequest(CusNavOverviewQuery q);

    /* ---------- 响应：gRPC -> BO ---------- */
    // 入口：单个
    @Mapping(target = "id", source = "data.id")
    @Mapping(target = "name", source = "data.navName")   // 注意 proto 叫 navName
    @Mapping(target = "widthPx", source = "data.widthPx")
    @Mapping(target = "heightPx", source = "data.heightPx")
    @Mapping(target = "originXPx", source = "data.originXPx")
    @Mapping(target = "originYPx", source = "data.originYPx")
    @Mapping(target = "navNodes", source = "data.navNodesList", qualifiedByName = "mapNodes")
    CusNavOverviewBO toBO(GrpcRCusNavOverviewDTO dto);

    // 入口：分页
//    @Mapping(target = "list", source = "data.dataList", qualifiedByName = "mapOverviewList")
//    @Mapping(target = "page", source = "data.page")
//    CusNavOverviewPageBO toPageBO(GrpcRPageCusNavOverviewDTO dto);

    /* ---------- 子对象转换 ---------- */
    @Named("mapOverviewList")
    List<CusNavOverviewBO> mapOverviewList(List<GrpcCusNavOverviewDTO> list);

    @Named("mapNodes")
    List<NavNodeBO> mapNodes(List<GrpcNavNodesDTO> list);

    // 节点
    @Mapping(target = "coordinateBind", source = "coordinateBind")
    NavNodeBO toNodeBO(GrpcNavNodesDTO dto);

    // 绑定
    @Mapping(target = "attribute", source = "attribute")
    DeviceCoordinateBindBO toBindBO(GrpcDeviceCoordinateBindDTO dto);

    // 属性
    DeviceCoordinateAttributeBO toAttrBO(GrpcDeviceCoordinateAttributeDTO dto);
}