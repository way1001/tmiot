package com.aiforest.tmiot.common.data.biz.impl;

import com.aiforest.tmiot.api.center.manager.NavigationPathGrpcGrpc;
import com.aiforest.tmiot.api.center.manager.PlanPathReply;
import com.aiforest.tmiot.api.center.manager.PlanPathRequest;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.data.biz.NavigationPathService;
import com.aiforest.tmiot.common.data.entity.vo.NavPathPoint;
import com.aiforest.tmiot.common.data.entity.builder.GrpcNavigationPathBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class NavigationPathServiceImpl implements NavigationPathService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private NavigationPathGrpcGrpc.NavigationPathGrpcBlockingStub navOverviewApiBlockingStub;

    @Resource
    GrpcNavigationPathBuilder builder;

    @Override
    public List<NavPathPoint> planPath(long overviewId,
                                       long fromNodeId,
                                       long toNodeId) {
        PlanPathRequest request = PlanPathRequest.newBuilder()
                .setOverviewId(overviewId)
                .setFromNodeId(fromNodeId)
                .setToNodeId(toNodeId)
                .build();

        PlanPathReply reply = navOverviewApiBlockingStub.planPath(request);

        return reply.getPathList()
                .stream()
                .map(builder::grpcToVo)
                .toList();
    }
}
