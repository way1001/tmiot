package com.aiforest.tmiot.common.manager.grpc.server.manager;

import com.aiforest.tmiot.api.center.manager.GrpcCusNavOverviewQuery;
import com.aiforest.tmiot.api.center.manager.GrpcPageCusNavOverviewQuery;
import com.aiforest.tmiot.api.center.manager.GrpcRCusNavOverviewDTO;
import com.aiforest.tmiot.api.center.manager.GrpcRPageCusNavOverviewDTO;
import com.aiforest.tmiot.api.center.manager.NavOverviewApiGrpc;
import com.aiforest.tmiot.api.common.GrpcPage;
import com.aiforest.tmiot.api.common.GrpcR;
import com.aiforest.tmiot.common.enums.ResponseEnum;
import com.aiforest.tmiot.common.manager.entity.bo.GrpcCusNavOverviewBO;
import com.aiforest.tmiot.common.manager.entity.query.CusNavOverviewQuery;
import com.aiforest.tmiot.common.manager.grpc.builder.GrpcNavOverviewBuilder;
import com.aiforest.tmiot.common.manager.service.NavOverviewService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.Objects;

/**
 * 自定导航总览GRPC服务实现
 *
 * @author way
 * @since 2025.09.02
 */
@Slf4j
@GrpcService
public class ManagerNavOverviewServer extends NavOverviewApiGrpc.NavOverviewApiImplBase {

    @Resource
    private NavOverviewService navOverviewService;

    @Resource
    private GrpcNavOverviewBuilder navOverviewBuilder;

    @Override
    public void selectByOverviewId(GrpcCusNavOverviewQuery request,
                                   StreamObserver<GrpcRCusNavOverviewDTO> responseObserver) {
        GrpcRCusNavOverviewDTO.Builder builder = GrpcRCusNavOverviewDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        try {
            // 1. 转换请求参数为查询条件
            CusNavOverviewQuery query = navOverviewBuilder.buildQueryBySingleRequest(request);

            // 2. 调用服务查询数据
            GrpcCusNavOverviewBO overviewBO = navOverviewService.selectById(query);

            // 3. 处理查询结果
            if (Objects.isNull(overviewBO)) {
                rBuilder.setOk(false);
                rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
                rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
            } else {
                rBuilder.setOk(true);
                rBuilder.setCode(ResponseEnum.OK.getCode());
                rBuilder.setMessage(ResponseEnum.OK.getText());

                // 转换BO为GRPC DTO
                builder.setData(navOverviewBuilder.buildOverviewDTO(overviewBO));
            }
        } catch (Exception e) {
            log.error("selectByOverviewId error", e);
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.FAILURE.getCode());
            rBuilder.setMessage(ResponseEnum.FAILURE.getText());
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void selectByPage(GrpcPageCusNavOverviewQuery request,
                             StreamObserver<GrpcRPageCusNavOverviewDTO> responseObserver) {
        GrpcRPageCusNavOverviewDTO.Builder builder = GrpcRPageCusNavOverviewDTO.newBuilder();
        GrpcR.Builder rBuilder = GrpcR.newBuilder();

        try {
            // 1. 转换请求参数为查询条件
            CusNavOverviewQuery query = navOverviewBuilder.buildQueryByPageRequest(request);

            // 2. 调用服务查询分页数据
            Page<GrpcCusNavOverviewBO> overviewPage = navOverviewService.selectByPage(query);

            // 3. 处理查询结果
            if (Objects.isNull(overviewPage)) {
                rBuilder.setOk(false);
                rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
                rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getText());
            } else {
                rBuilder.setOk(true);
                rBuilder.setCode(ResponseEnum.OK.getCode());
                rBuilder.setMessage(ResponseEnum.OK.getText());

                // 构建分页响应
                com.aiforest.tmiot.api.center.manager.GrpcPageCusNavOverviewDTO.Builder pageDataBuilder =
                        com.aiforest.tmiot.api.center.manager.GrpcPageCusNavOverviewDTO.newBuilder();

                // 设置分页信息
                GrpcPage.Builder pageBuilder = GrpcPage.newBuilder();
                pageBuilder.setCurrent(overviewPage.getCurrent());
                pageBuilder.setSize(overviewPage.getSize());
                pageBuilder.setPages(overviewPage.getPages());
                pageBuilder.setTotal(overviewPage.getTotal());
                pageDataBuilder.setPage(pageBuilder);

                // 转换BO列表为GRPC DTO列表
                List<com.aiforest.tmiot.api.center.manager.GrpcCusNavOverviewDTO> dtoList =
                        overviewPage.getRecords().stream()
                                .map(navOverviewBuilder::buildOverviewDTO)
                                .collect(java.util.stream.Collectors.toList());
                pageDataBuilder.addAllData(dtoList);

                builder.setData(pageDataBuilder);
            }
        } catch (Exception e) {
            log.error("selectByPage error", e);
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.FAILURE.getCode());
            rBuilder.setMessage(ResponseEnum.FAILURE.getText());
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}