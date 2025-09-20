package com.aiforest.tmiot.common.data.entity.builder;

import com.aiforest.tmiot.api.common.GrpcDeviceDTO;
import com.aiforest.tmiot.api.common.GrpcDevicePointsDTO;
import com.aiforest.tmiot.api.common.GrpcPointDTO;
import com.aiforest.tmiot.common.data.entity.bo.DevicePointLatestBO;
import com.aiforest.tmiot.common.data.entity.bo.PointBO;
import com.aiforest.tmiot.common.entity.ext.DeviceExt;
import com.aiforest.tmiot.common.optional.CollectionOptional;
import com.aiforest.tmiot.common.optional.EnableOptional;
import com.aiforest.tmiot.common.optional.JsonOptional;
import com.aiforest.tmiot.common.utils.GrpcBuilderUtil;
import com.aiforest.tmiot.common.utils.JsonUtil;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring", uses = {GrpcPointBuilder.class})
public interface GrpcDevicePointsBuilder {

    @Mapping(target = "deviceExt", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "points", ignore = true)
    DevicePointLatestBO buildDTOByGrpcDTO(GrpcDevicePointsDTO entityGrpc);

    @AfterMapping
    default void afterProcess(GrpcDevicePointsDTO entityGrpc,
                              @MappingTarget DevicePointLatestBO entityBO) {

        JsonOptional.ofNullable(entityGrpc.getDeviceExt()).ifPresent(value -> entityBO.setDeviceExt(JsonUtil.parseObject(value, DeviceExt.class)));
        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityBO::setEnableFlag);
        GrpcPointBuilder pointBuilder = Mappers.getMapper(GrpcPointBuilder.class);
        List<PointBO> points = Optional.of(entityGrpc.getPointsList())
                .orElse(List.of())
                .stream()
                .map(pointBuilder::buildDTOByGrpcDTO)
                .toList();
        entityBO.setPoints(points);

    }
}
