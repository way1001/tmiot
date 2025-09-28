package com.aiforest.tmiot.common.manager.grpc.builder;

import com.aiforest.tmiot.common.manager.entity.vo.NavPathPoint;
import com.aiforest.tmiot.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

/**
 * 路径规划专用 GRPC 转换器
 *
 * @author way
 * @since 2025.09.02
 */
@Mapper(componentModel = "spring",uses = {MapStructUtil .class})
public interface NavigationPathGrpcBuilder {

    /**
     * VO -> GRPC 消息
     */
    com.aiforest.tmiot.api.center.manager.NavPathPoint voToGrpc(NavPathPoint vo);
}