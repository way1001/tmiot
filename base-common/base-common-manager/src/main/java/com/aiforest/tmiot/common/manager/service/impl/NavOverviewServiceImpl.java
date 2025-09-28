package com.aiforest.tmiot.common.manager.service.impl;

import com.aiforest.tmiot.common.manager.entity.bo.GrpcCusNavOverviewBO;
import com.aiforest.tmiot.common.manager.entity.bo.GrpcDeviceCoordinateAttributeBO;
import com.aiforest.tmiot.common.manager.entity.bo.GrpcDeviceCoordinateBindBO;
import com.aiforest.tmiot.common.manager.entity.bo.GrpcNavNodesBO;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateAttributeDO;
import com.aiforest.tmiot.common.manager.entity.model.DeviceCoordinateBindDO;
import com.aiforest.tmiot.common.manager.entity.model.NavNodesDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aiforest.tmiot.common.manager.mapper.NavOverviewMapper;
import com.aiforest.tmiot.common.manager.mapper.DeviceCoordinateAttributeMapper;
import com.aiforest.tmiot.common.manager.mapper.DeviceCoordinateBindMapper;
import com.aiforest.tmiot.common.manager.entity.model.CusNavOverviewDO;
import com.aiforest.tmiot.common.manager.entity.query.CusNavOverviewQuery;
import com.aiforest.tmiot.common.manager.service.NavOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定导航总览服务实现类
 *
 * @author way
 * @since 2025.09.02
 */
@Service
@RequiredArgsConstructor
public class NavOverviewServiceImpl extends ServiceImpl<NavOverviewMapper, CusNavOverviewDO> implements NavOverviewService {

    private final NavOverviewMapper navOverviewMapper;
    private final DeviceCoordinateBindMapper deviceCoordinateBindMapper;
    private final DeviceCoordinateAttributeMapper deviceCoordinateAttributeMapper;

    @Override
    public Page<GrpcCusNavOverviewBO> selectByPage(CusNavOverviewQuery query) {
        // 1. 分页查询自定导航总览DO
        Page<CusNavOverviewDO> overviewPage = navOverviewMapper.selectOverviewPage(
                new Page<>(query.getPage().getCurrent(), query.getPage().getSize()),
                query
        );

        // 2. 转换为BO并关联数据
        Page<GrpcCusNavOverviewBO> resultPage = new Page<>(
                overviewPage.getCurrent(),
                overviewPage.getSize(),
                overviewPage.getTotal()
        );

        List<GrpcCusNavOverviewBO> overviewBOList = overviewPage.getRecords().stream()
                .map(this::convertToBO)
                .collect(Collectors.toList());

        resultPage.setRecords(overviewBOList);
        return resultPage;
    }

    @Override
    public GrpcCusNavOverviewBO selectById(CusNavOverviewQuery query) {
        // 1. 查询单个总览DO
        CusNavOverviewDO overviewDO = baseMapper.selectById(query.getOverviewId());
        if (overviewDO == null) {
            return null;
        }

        // 2. 转换为BO并关联数据
        return convertToBO(overviewDO);
    }

    /**
     * 将DO转换为BO并关联所有子数据
     */
    private GrpcCusNavOverviewBO convertToBO(CusNavOverviewDO overviewDO) {
        // 1. 转换总览基本信息
        GrpcCusNavOverviewBO overviewBO = new GrpcCusNavOverviewBO();
        BeanUtils.copyProperties(overviewDO, overviewBO);

        // 2. 查询并转换坐标绑定数据（跳过节点直接关联绑定）
        List<NavNodesDO> nodesDOList = navOverviewMapper.selectNodesByOverviewId(overviewDO.getId());

        if (nodesDOList != null && !nodesDOList.isEmpty()) {
            List<GrpcNavNodesBO> nodesBOList = nodesDOList.stream()
                    .map(nodeDO -> {
                        // 转换节点DO为BO
                        GrpcNavNodesBO nodeBO = new GrpcNavNodesBO();
                        BeanUtils.copyProperties(nodeDO, nodeBO);

                        // 3. 查询并转换坐标绑定（1对1）
                        // 由于是一对一关系，只取第一条绑定记录
                        List<DeviceCoordinateBindDO> bindDOList = navOverviewMapper.selectBindsByNodeId(nodeDO.getId());
                        if (bindDOList != null && !bindDOList.isEmpty()) {
                            DeviceCoordinateBindDO bindDO = bindDOList.get(0); // 只取第一个绑定
                            GrpcDeviceCoordinateBindBO bindBO = new GrpcDeviceCoordinateBindBO();
                            BeanUtils.copyProperties(bindDO, bindBO);

                            // 4. 关联坐标属性（1对1）
                            DeviceCoordinateAttributeDO attributeDO = navOverviewMapper.selectAttributeById(
                                    bindDO.getDeviceCoordinateAttributeId()
                            );
                            if (attributeDO != null) {
                                GrpcDeviceCoordinateAttributeBO attributeBO = new GrpcDeviceCoordinateAttributeBO();
                                BeanUtils.copyProperties(attributeDO, attributeBO);
                                bindBO.setAttributeBO(attributeBO);
                            }

                            nodeBO.setGrpcDeviceCoordinateBindBO(bindBO);
                        }

                        return nodeBO;
                    })
                    .collect(Collectors.toList());

            overviewBO.setGrpcNavNodesBOList(nodesBOList);
        }

        return overviewBO;
    }
}