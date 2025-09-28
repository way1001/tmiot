package com.aiforest.tmiot.common.data.biz.impl;

import com.aiforest.tmiot.api.center.manager.GrpcCusNavOverviewQuery;
import com.aiforest.tmiot.api.center.manager.GrpcPageCusNavOverviewQuery;
import com.aiforest.tmiot.api.center.manager.GrpcRCusNavOverviewDTO;
import com.aiforest.tmiot.api.center.manager.GrpcRPageCusNavOverviewDTO;
import com.aiforest.tmiot.api.center.manager.NavOverviewApiGrpc;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.data.biz.NavOverviewDataService;
import com.aiforest.tmiot.common.data.entity.bo.CusNavOverviewBO;
import com.aiforest.tmiot.common.data.entity.builder.GrpcNavOverviewBuilder;
import com.aiforest.tmiot.common.data.entity.query.CusNavOverviewQuery;
import com.aiforest.tmiot.common.exception.ServiceException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 自定义导航总览数据服务实现类
 * 核心逻辑：通过GRPC调用Manager服务获取数据，处理分页与单条查询的参数适配
 *
 * @author way
 * @since 2025.09.02
 */
@Slf4j
@Service
public class NavOverviewDataServiceImpl implements NavOverviewDataService {

    /**
     * 注入Manager服务的GRPC阻塞式存根（参考DeviceApiGrpc的调用方式）
     */
    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private NavOverviewApiGrpc.NavOverviewApiBlockingStub navOverviewApiBlockingStub;

    /**
     * 注入GRPC DTO与BO/Query的转换构建器（与ManagerNavOverviewServer共用）
     */
    @Resource
    private GrpcNavOverviewBuilder navOverviewBuilder;

    /**
     * 分页查询导航总览数据
     * 适配逻辑：构建GRPC分页请求，调用Manager服务，处理分页结果（支持多页数据拼接）
     */
    @Override
    public Page<CusNavOverviewBO> listNavOverviews(CusNavOverviewQuery query) {
        // 1. 校验入参，设置默认值（避免空指针）
//        long pageNum = Objects.isNull(current) ? 1 : current;
//        long pageSize = Objects.isNull(size) ? 10 : size;
//
//        // 2. 首次查询，获取第一页数据与总页数
//        GrpcRPageCusNavOverviewDTO firstPageResponse = getGrpcPageResponse(tenantId, pageNum, pageSize);
//        com.aiforest.tmiot.api.center.manager.GrpcPageCusNavOverviewDTO pageData = firstPageResponse.getData();
//        if (Objects.isNull(pageData)) {
//            log.warn("listNavOverviews: tenantId={} first page data is null", tenantId);
//            return Collections.emptyList();
//        }
//
//        // 3. 转换第一页GRPC DTO为BO列表
//        List<GrpcCusNavOverviewBO> allOverviewBOs = convertGrpcDtoToBo(pageData.getDataList());
//        long totalPages = pageData.getPage().getPages();
//
//        // 4. 分页查询剩余数据（若总页数>当前页）
//        while (pageNum < totalPages) {
//            pageNum++;
//            GrpcRPageCusNavOverviewDTO nextPageResponse = getGrpcPageResponse(tenantId, pageNum, pageSize);
//            com.aiforest.tmiot.api.center.manager.GrpcPageCusNavOverviewDTO nextPageData = nextPageResponse.getData();
//            if (Objects.nonNull(nextPageData)) {
//                allOverviewBOs.addAll(convertGrpcDtoToBo(nextPageData.getDataList()));
//                // 更新总页数（防止分页过程中数据变化导致总页数变更）
//                totalPages = nextPageData.getPage().getPages();
//            }
//        }
//
//        return allOverviewBOs;
        return null;
    }

    /**
     * 根据ID查询单条导航总览数据
     * 适配逻辑：构建GRPC单条查询请求，调用Manager服务，处理单条结果
     */
    @Override
    public CusNavOverviewBO getNavOverviewById(Long overviewId, Long tenantId) {
        // 1. 校验入参
        if (Objects.isNull(overviewId) || Objects.isNull(tenantId)) {
            log.error("getNavOverviewById: overviewId={} or tenantId={} is null", overviewId, tenantId);
            throw new ServiceException("查询参数（总览ID/租户ID）不能为空");
        }

        // 2. 构建GRPC查询请求（通过builder转换Query为GRPC请求）
        CusNavOverviewQuery query = new CusNavOverviewQuery();
        query.setOverviewId(overviewId);
        query.setTenantId(tenantId);
        GrpcCusNavOverviewQuery grpcQuery = navOverviewBuilder.toSingleRequest(query);

        // 3. 调用GRPC服务查询单条数据
        GrpcRCusNavOverviewDTO grpcResponse = navOverviewApiBlockingStub.selectByOverviewId(grpcQuery);
        if (!grpcResponse.getResult().getOk()) {
            log.error("getNavOverviewById: GRPC call failed, overviewId={}, msg={}",
                    overviewId, grpcResponse.getResult().getMessage());
            throw new ServiceException("获取导航总览数据失败：" + grpcResponse.getResult().getMessage());
        }

        // 4. 转换GRPC DTO为BO并返回
//        com.aiforest.tmiot.api.center.manager.GrpcCusNavOverviewDTO grpcDto = grpcResponse.getData();
        return navOverviewBuilder.toBO(grpcResponse);
    }

    // ------------------------------ 私有工具方法 ------------------------------

    /**
     * 构建GRPC分页请求并调用服务，返回分页响应
     * 统一处理GRPC调用异常与结果校验
     */
//    private GrpcRPageCusNavOverviewDTO getGrpcPageResponse(Long tenantId, long current, long size) {
//        // 1. 构建分页查询条件（租户ID+分页参数）
//        CusNavOverviewQuery query = new CusNavOverviewQuery();
//        query.setTenantId(tenantId);
//        query.setCurrent(current);
//        query.setSize(size);
//
//        // 2. 转换Query为GRPC分页请求
//        GrpcPageCusNavOverviewQuery grpcPageQuery = navOverviewBuilder.buildGrpcQueryByPageQuery(query);
//
//        // 3. 调用GRPC服务并校验结果
//        GrpcRPageCusNavOverviewDTO grpcResponse = navOverviewApiBlockingStub.selectByPage(grpcPageQuery);
//        if (!grpcResponse.getResult().getOk()) {
//            log.error("getGrpcPageResponse: GRPC call failed, tenantId={}, current={}, msg={}",
//                    tenantId, current, grpcResponse.getResult().getMessage());
//            throw new ServiceException("获取导航总览分页数据失败：" + grpcResponse.getResult().getMessage());
//        }
//
//        return grpcResponse;
//    }
//
//    /**
//     * 将GRPC DTO列表转换为BO列表
//     * 依赖GrpcNavOverviewBuilder的转换能力，与Manager服务端保持一致
//     */
//    private List<GrpcCusNavOverviewBO> convertGrpcDtoToBo(
//            List<com.aiforest.tmiot.api.center.manager.GrpcCusNavOverviewDTO> grpcDtoList) {
//        if (grpcDtoList.isEmpty()) {
//            return Collections.emptyList();
//        }
//        return grpcDtoList.stream()
//                .map(navOverviewBuilder::buildBOByGrpcDTO)
//                .filter(Objects::nonNull) // 过滤空对象，避免后续NPE
//                .collect(Collectors.toList());
//    }
}