package com.aiforest.tmiot.common.data.entity.builder;

import com.aiforest.tmiot.common.data.entity.vo.NavPathPoint;
import com.aiforest.tmiot.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

/**
 * 路径规划专用 gRPC 客户端转换器
 *
 * @author way
 * @since 2025.09.02
 */
@Mapper(componentModel = "spring", uses = MapStructUtil.class)
public interface GrpcNavigationPathBuilder {

    /**
     * GRPC 消息 -> 内部 VO
     */
    NavPathPoint grpcToVo(com.aiforest.tmiot.api.center.manager.NavPathPoint grpc);
}