/*
 * Copyright 2016-present the TM IoT original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aiforest.tmiot.common.data.biz.impl;

import cn.hutool.core.map.MapUtil;
import com.aiforest.tmiot.api.center.manager.*;
import com.aiforest.tmiot.api.common.GrpcDeviceDTO;
import com.aiforest.tmiot.api.common.GrpcDriverDTO;
import com.aiforest.tmiot.api.common.GrpcPage;
import com.aiforest.tmiot.common.constant.common.DefaultConstant;
import com.aiforest.tmiot.common.constant.common.PrefixConstant;
import com.aiforest.tmiot.common.constant.service.ManagerConstant;
import com.aiforest.tmiot.common.data.biz.DriverStatusService;
import com.aiforest.tmiot.common.data.entity.bo.DriverRunBO;
import com.aiforest.tmiot.common.data.entity.model.DriverRunDO;
import com.aiforest.tmiot.common.data.entity.query.DriverQuery;
import com.aiforest.tmiot.common.data.service.DriverRunService;
import com.aiforest.tmiot.common.enums.DeviceStatusEnum;
import com.aiforest.tmiot.common.enums.DriverStatusEnum;
import com.aiforest.tmiot.common.optional.LongOptional;
import com.aiforest.tmiot.common.optional.StringOptional;
import com.aiforest.tmiot.common.redis.service.RedisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DriverService Impl
 *
 * @author way
 * @version 2025.7.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverStatusServiceImpl implements DriverStatusService {

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private RedisService redisService;
    @Resource
    private DriverRunService driverRunService;

    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Override
    public Map<Long, String> selectByPage(DriverQuery pageQuery) {
        GrpcPage.Builder page = GrpcPage.newBuilder().setSize(pageQuery.getPage().getSize()).setCurrent(pageQuery.getPage().getCurrent());
        GrpcPageDriverQuery.Builder query = GrpcPageDriverQuery.newBuilder().setPage(page);
        StringOptional.ofNullable(pageQuery.getDriverName()).ifPresent(query::setDriverName);
        StringOptional.ofNullable(pageQuery.getDriverCode()).ifPresent(query::setDriverCode);
        StringOptional.ofNullable(pageQuery.getServiceName()).ifPresent(query::setServiceName);
        StringOptional.ofNullable(pageQuery.getServiceHost()).ifPresent(query::setServiceHost);
        LongOptional.ofNullable(pageQuery.getTenantId()).ifPresent(query::setTenantId);
        Optional.ofNullable(pageQuery.getDriverTypeFlag()).ifPresentOrElse(value -> query.setDriverTypeFlag(value.getIndex()), () -> query.setDriverTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(pageQuery.getEnableFlag()).ifPresentOrElse(value -> query.setEnableFlag(value.getIndex()), () -> query.setEnableFlag(DefaultConstant.DEFAULT_INT));
        GrpcRPageDriverDTO rPageDriverDTO = driverApiBlockingStub.selectByPage(query.build());

        if (!rPageDriverDTO.getResult().getOk()) {
            return MapUtil.empty();
        }

        List<GrpcDriverDTO> drivers = rPageDriverDTO.getData().getDataList();
        return getStatusMap(drivers);
    }

    @Override
    public DriverRunBO selectOnlineByDriverId(Long driverId) {
        List<DriverRunDO> driverRunDOList = driverRunService.get7daysDuration(driverId, DriverStatusEnum.ONLINE.getCode());
        Long totalDuration = driverRunService.selectSumDuration(driverId, DriverStatusEnum.ONLINE.getCode());
        GrpcDriverQuery.Builder builder = GrpcDriverQuery.newBuilder();
        builder.setDriverId(driverId);
        GrpcRDriverDTO rDriverDTO = driverApiBlockingStub.selectByDriverId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            throw new RuntimeException("Driver does not exist");
        }
        DriverRunBO driverRunBO = new DriverRunBO();
        List<Long> zeroList = Collections.nCopies(7, 0L);
        ArrayList<Long> list = new ArrayList<>(zeroList);
        driverRunBO.setDriverName(rDriverDTO.getData().getDriverName());
        driverRunBO.setStatus(DriverStatusEnum.ONLINE.getCode());
        driverRunBO.setTotalDuration(totalDuration == null ? 0L : totalDuration);
        if (Objects.isNull(driverRunDOList)) {
            driverRunBO.setDuration(list);
            return driverRunBO;
        }
        for (int i = 0; i < driverRunDOList.size(); i++) {
            list.set(i, driverRunDOList.get(i).getDuration());
        }
        driverRunBO.setDuration(list);
        return driverRunBO;
    }

    @Override
    public DriverRunBO selectOfflineByDriverId(Long driverId) {
        List<DriverRunDO> driverRunDOList = driverRunService.get7daysDuration(driverId, DriverStatusEnum.OFFLINE.getCode());
        Long totalDuration = driverRunService.selectSumDuration(driverId, DriverStatusEnum.OFFLINE.getCode());
        GrpcDriverQuery.Builder builder = GrpcDriverQuery.newBuilder();
        builder.setDriverId(driverId);
        GrpcRDriverDTO rDriverDTO = driverApiBlockingStub.selectByDriverId(builder.build());
        if (!rDriverDTO.getResult().getOk()) {
            throw new RuntimeException("Driver id does not exist");
        }
        DriverRunBO driverRunBO = new DriverRunBO();
        List<Long> zeroList = Collections.nCopies(7, 0L);
        ArrayList<Long> list = new ArrayList<>(zeroList);
        driverRunBO.setTotalDuration(totalDuration == null ? 0L : totalDuration);
        driverRunBO.setStatus(DriverStatusEnum.OFFLINE.getCode());
        driverRunBO.setDriverName(rDriverDTO.getData().getDriverName());
        if (Objects.isNull(driverRunDOList)) {
            driverRunBO.setDuration(list);
            return driverRunBO;
        }
        for (int i = 0; i < driverRunDOList.size(); i++) {
            list.set(i, driverRunDOList.get(i).getDuration());
        }
        driverRunBO.setDuration(list);
        return driverRunBO;
    }

    @Override
    public String getDeviceOnlineByDriverId(Long driverId) {
        List<String> list = getList(driverId);
        if (list == null) return String.valueOf(0L);
        long count = list.stream().filter(e -> e.equals(DeviceStatusEnum.ONLINE.getCode())).count();
        return String.valueOf(count);
    }

    @Override
    public String getDeviceOfflineByDriverId(Long driverId) {
        List<String> list = getList(driverId);
        if (list == null) return String.valueOf(0L);
        long count = list.stream().filter(e -> e.equals(DeviceStatusEnum.OFFLINE.getCode())).count();
        return String.valueOf(count);
    }

    /**
     * get deviceList  Online/Offline BY driverId
     *
     * @param driverId
     * @return
     */
    private List<String> getList(Long driverId) {
        GrpcDriverQuery query = GrpcDriverQuery.newBuilder()
                .setDriverId(driverId)
                .build();
        GrpcRDeviceListDTO onlineByDriverId = deviceApiBlockingStub.selectByDriverId(query);
        if (!onlineByDriverId.getResult().getOk()) {
            return null;
        }
        List<GrpcDeviceDTO> devices = onlineByDriverId.getDataList();
        Set<Long> deviceIds = devices.stream().map(d -> d.getBase().getId()).collect(Collectors.toSet());
        List<String> list = new ArrayList<>();
        deviceIds.forEach(id -> {
            String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + id;
            String status = redisService.getKey(key);
            status = Objects.nonNull(status) ? status : DeviceStatusEnum.OFFLINE.getCode();
            list.add(status);
        });
        return list;
    }

    /**
     * Get status map
     *
     * @param drivers GrpcDriverDTO Array
     * @return Status Map
     */
    private Map<Long, String> getStatusMap(List<GrpcDriverDTO> drivers) {
        Map<Long, String> statusMap = new HashMap<>(16);
        Set<Long> driverIds = drivers.stream().map(d -> d.getBase().getId()).collect(Collectors.toSet());
        driverIds.forEach(id -> {
            String key = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + id;
            String status = redisService.getKey(key);
            status = Objects.nonNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
            statusMap.put(id, status);
        });
        return statusMap;
    }

}
