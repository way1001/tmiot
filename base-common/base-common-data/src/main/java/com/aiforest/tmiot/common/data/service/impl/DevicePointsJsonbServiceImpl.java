package com.aiforest.tmiot.common.data.service.impl;

import com.aiforest.tmiot.common.data.entity.bo.DeviceSnapshotBO;
import com.aiforest.tmiot.common.data.entity.builder.DeviceDurationBuilder;
import com.aiforest.tmiot.common.data.entity.builder.DevicePointsJsonbBuilder;
import com.aiforest.tmiot.common.data.entity.dto.HistoryPageDTO;
import com.aiforest.tmiot.common.data.entity.vo.DeviceSnapshotVO;
import com.aiforest.tmiot.common.data.mapper.DevicePointsJsonbMapper;
import com.aiforest.tmiot.common.data.mapper.DriverRunHistoryMapper;
import com.aiforest.tmiot.common.data.service.DevicePointsJsonbService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.aiforest.tmiot.common.data.entity.model.DevicePointsJsonbDO;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class DevicePointsJsonbServiceImpl
        extends ServiceImpl<DevicePointsJsonbMapper, DevicePointsJsonbDO>
        implements DevicePointsJsonbService {

    @Resource
    private DevicePointsJsonbMapper mapper;
    /** 保存：直接传 BO，MyBatis-Plus 自动序列化 JSONB */
    @Override
    public void save(DeviceSnapshotBO bo) {
        DevicePointsJsonbDO entity = new DevicePointsJsonbDO();
        entity.setDeviceId(bo.getId());
        entity.setSnapshot(new ObjectMapper().valueToTree(bo)); // ← 自动序列化
        mapper.insert(entity);
    }

    /** 查询：MyBatis-Plus 自动转 VO */
//    @Override
//    public IPage<DeviceSnapshotVO> page(HistoryPageDTO dto, long current, long size) {
//        return mapper.selectVoPage(new Page<>(current, size), dto);
//    }

//    @Override
//    public IPage<DeviceSnapshotVO> pageByTime(LocalDateTime start, LocalDateTime end,
//                                              long current, long size) {
//        Page<DevicePointsJsonbDO> page = Page.of(current, size);
//        IPage<DevicePointsJsonbDO> doPage = baseMapper.selectByTime(page, start, end);
//        return doPage.convert(DevicePointsJsonbBuilder.INSTANCE::doToVo);
//    }
}
