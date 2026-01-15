package com.aiforest.tmiot.common.data.service;

import com.aiforest.tmiot.common.data.entity.bo.DeviceSnapshotBO;
import com.aiforest.tmiot.common.data.entity.dto.HistoryPageDTO;
import com.aiforest.tmiot.common.data.entity.model.DevicePointsJsonbDO;
import com.aiforest.tmiot.common.data.entity.model.ZTJsonbDO;
import com.aiforest.tmiot.common.data.entity.vo.DeviceSnapshotVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public interface DevicePointsJsonbService extends IService<DevicePointsJsonbDO> {

    void save(DeviceSnapshotBO bo);
//    IPage<DeviceSnapshotVO> page(HistoryPageDTO dto, long current, long size);
//    IPage<DeviceSnapshotVO> pageByTime(LocalDateTime start, LocalDateTime end, long current, long size);
}
