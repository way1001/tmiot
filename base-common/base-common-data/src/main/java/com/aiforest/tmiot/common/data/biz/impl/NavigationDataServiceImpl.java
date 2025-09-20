package com.aiforest.tmiot.common.data.biz.impl;

import com.aiforest.tmiot.api.center.manager.*;
import com.aiforest.tmiot.api.common.GrpcDevicePointsDTO;
import com.aiforest.tmiot.api.common.GrpcDriverAttributeConfigDTO;
import com.aiforest.tmiot.api.common.GrpcPage;
import com.aiforest.tmiot.api.common.GrpcPointAttributeConfigDTO;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.data.biz.NavigationDataService;
import com.aiforest.tmiot.common.data.entity.bo.DevicePointLatestBO;
import com.aiforest.tmiot.common.data.entity.builder.GrpcDevicePointsBuilder;
import com.aiforest.tmiot.common.data.entity.builder.GrpcPointBuilder;
import com.aiforest.tmiot.common.entity.bo.PointValueBO;
import com.aiforest.tmiot.common.exception.RepositoryException;
import com.aiforest.tmiot.common.exception.ServiceException;
import com.aiforest.tmiot.common.optional.CollectionOptional;
import com.aiforest.tmiot.common.redis.service.RedisRepositoryService;
import com.aiforest.tmiot.common.repository.RepositoryService;
import com.aiforest.tmiot.common.strategy.RepositoryStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NavigationDataServiceImpl implements NavigationDataService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Resource
    private RedisRepositoryService redisRepositoryService;

    @Resource
    private GrpcDevicePointsBuilder devicePointsBuilder;

    @Override
    public List<DevicePointLatestBO> listDevicePoints(Long tenantId) {
        long current = 1;
        GrpcRPageDevicePointsDTO rPageDeviceDTO = getGrpcRPageDevicePointsDTO(current, tenantId);
        GrpcPageDevicePointsDTO pageDTO = rPageDeviceDTO.getData();
        List<GrpcDevicePointsDTO> dataList = pageDTO.getDataList();
        List<DevicePointLatestBO> deviceBOList = dataList.stream().map(devicePointsBuilder::buildDTOByGrpcDTO).toList();
        List<DevicePointLatestBO> allDeviceBOList = new ArrayList<>(deviceBOList);

        long pages = pageDTO.getPage().getPages();
        while (current < pages) {
            current++;
            GrpcRPageDevicePointsDTO tPageDeviceDTO = getGrpcRPageDevicePointsDTO(current, tenantId);
            GrpcPageDevicePointsDTO tPageDTO = tPageDeviceDTO.getData();
            List<GrpcDevicePointsDTO> tDataList = tPageDTO.getDataList();
            List<DevicePointLatestBO> tDeviceBOList = tDataList.stream().map(devicePointsBuilder::buildDTOByGrpcDTO).toList();
            allDeviceBOList.addAll(tDeviceBOList);
            pages = tPageDTO.getPage().getPages();
        }
        return allDeviceBOList;
    }

    private GrpcRPageDevicePointsDTO getGrpcRPageDevicePointsDTO(long current, long tenantId) {
        GrpcPageDeviceQuery.Builder query = GrpcPageDeviceQuery.newBuilder();
        GrpcPage.Builder page = GrpcPage.newBuilder();
        page.setCurrent(current);
        query.setTenantId(tenantId)
                .setPage(page);
        GrpcRPageDevicePointsDTO rPageDeviceDTO = deviceApiBlockingStub.selectByLatestPage(query.build());
        if (!rPageDeviceDTO.getResult().getOk()) {
            throw new ServiceException("获取设备列表失败");
        }
        return rPageDeviceDTO;
    }

    @Override
    public List<PointValueBO> latest(Long deviceId, List<Long> pointIds) {
        Map<Long, PointValueBO> pointValueBOMap = redisRepositoryService.selectLatestPointValue(deviceId, pointIds);
        RepositoryService repositoryService = getFirstRepositoryService();
        return pointIds.stream().map(id -> {
            PointValueBO value = pointValueBOMap.get(id);
            return Objects.isNull(value) ? repositoryService.selectLatestPointValue(deviceId, id) : value;
        }).filter(Objects::nonNull).toList();
    }

    /**
     * 获取数据存储服务
     *
     * @return RepositoryService
     */
    private RepositoryService getFirstRepositoryService() {
        List<RepositoryService> repositoryServices = RepositoryStrategyFactory.get();
        if (!repositoryServices.isEmpty() && repositoryServices.size() > 1) {
            throw new RepositoryException("Save point values to repository error: There are multiple repository, only one is supported.");
        }

        Optional<RepositoryService> first = repositoryServices.stream().findFirst();
        if (!first.isPresent()) {
            throw new RepositoryException("Save point values to repository error: Please configure at least one repository.");
        }

        return first.get();
    }


}
