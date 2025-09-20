package com.aiforest.tmiot.common.data.biz;

import com.aiforest.tmiot.common.data.entity.bo.DevicePointLatestBO;
import com.aiforest.tmiot.common.entity.bo.PointValueBO;

import java.util.List;

public interface NavigationDataService {
    List<DevicePointLatestBO> listDevicePoints(Long tenantId);
    List<PointValueBO> latest(Long deviceId, List<Long> pointIds);
}
