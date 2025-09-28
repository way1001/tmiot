package com.aiforest.tmiot.common.manager.grpc.server.manager;

import com.aiforest.tmiot.api.center.manager.NavigationPathGrpcGrpc;
import com.aiforest.tmiot.api.center.manager.PlanPathReply;
import com.aiforest.tmiot.api.center.manager.PlanPathRequest;
import com.aiforest.tmiot.common.manager.entity.vo.NavPathPoint;
import com.aiforest.tmiot.common.manager.grpc.builder.NavigationPathGrpcBuilder;
import com.aiforest.tmiot.common.manager.service.NavigationPathService;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@Slf4j
@GrpcService
public class NavigationPathGrpc  extends NavigationPathGrpcGrpc.NavigationPathGrpcImplBase{

    @Resource
    private NavigationPathService pathService; // 复用核心逻辑
    @Resource
    private NavigationPathGrpcBuilder builder;   // <-- 注入

    @Override
    public void planPath(PlanPathRequest request,
                         StreamObserver<PlanPathReply> responseObserver) {
        List<NavPathPoint> path = pathService.planPath(
                request.getOverviewId(),
                request.getFromNodeId(),
                request.getToNodeId());

        PlanPathReply reply = PlanPathReply.newBuilder()
                .addAllPath(path.stream()
                        .map(builder::voToGrpc)   // <-- 改用 builder
                        .toList())
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
