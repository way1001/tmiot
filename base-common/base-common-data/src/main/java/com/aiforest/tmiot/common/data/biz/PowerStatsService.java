package com.aiforest.tmiot.common.data.biz;

import com.aiforest.tmiot.common.data.entity.bo.DevicePointJsonbBattVBO;
import com.aiforest.tmiot.common.data.entity.query.DeviceTimeRangeQuery;
import com.aiforest.tmiot.common.data.entity.vo.StatsRespVO;
import com.aiforest.tmiot.common.data.mapper.DevicePointsJsonbMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PowerStatsService {

    private final DevicePointsJsonbMapper mapper;

    public Map<String, StatsRespVO> getStatsBatch(List<String> deviceIds,
                                                  LocalDateTime start,
                                                  LocalDateTime end) {
        DeviceTimeRangeQuery dto = new DeviceTimeRangeQuery(deviceIds, start, end);

        // 1. 任务数
        Map<String, Long> taskMap = mapper.countTasksBatch(dto).stream()
                .collect(Collectors.toMap(r -> (String) r.get("device_id"),
                        r -> ((Number) r.get("cnt")).longValue()));

        // 2. 导航段数
        Map<String, Long> navMap = mapper.countNavSegmentsBatch(dto).stream()
                .collect(Collectors.toMap(r -> (String) r.get("device_id"),
                        r -> ((Number) r.get("cnt")).longValue()));

        // 3. 电压曲线 → 按设备分组
        Map<String, List<DevicePointJsonbBattVBO>> curveMap =
                mapper.listVoltageCurveBatch(dto).stream()
                        .collect(Collectors.groupingBy(DevicePointJsonbBattVBO::getDeviceId,
                                LinkedHashMap::new, Collectors.toList()));

        // 4. 组装结果
        Map<String, StatsRespVO> result = new LinkedHashMap<>();
//        for (String dev : deviceIds) {
//            List<DevicePointJsonbBattVBO> curve = curveMap.getOrDefault(dev, List.of());
//
//            double cumulativeQ = 0.0; // 有效累积电量 (mV·h)
//            int chargeCnt = 0;
//
//            for (int i = 0; i < curve.size() - 1; i++) {
//                int v1   = curve.get(i).getVoltage();
//                int v2   = curve.get(i + 1).getVoltage();
//                long minutes = Duration.between(curve.get(i).getCreateTime(),
//                        curve.get(i + 1).getCreateTime()).toMinutes();
//                if (minutes <= 0) continue;
//
//                // 充电判断：上升 > 50 mV
//                if (v2 - v1 > 50) {
//                    chargeCnt++;
//                    continue; // 丢弃该段电量
//                }
//
//                // 否则视为有效放电段，梯形积分
//                double segmentQ = (v1 + v2) / 2.0 * minutes;
//                cumulativeQ += segmentQ;
//            }
//            cumulativeQ /= 60.0; // mV·h
//
//            result.put(dev, StatsRespVO.builder()
//                    .taskCount(taskMap.getOrDefault(dev, 0L))
//                    .navSegmentCount(navMap.getOrDefault(dev, 0L))
//                    .totalConsumption(cumulativeQ)
//                    .chargeCount(chargeCnt)
//                    .build());
//        }
        for (String dev : deviceIds) {
            List<DevicePointJsonbBattVBO> curve = curveMap.getOrDefault(dev, List.of());

            int totalDrop = 0;   // 累积压降（mV）
            int chargeCnt = 0;

            if (curve.isEmpty()) {
                result.put(dev, StatsRespVO.builder()
                        .taskCount(taskMap.getOrDefault(dev, 0L))
                        .navSegmentCount(navMap.getOrDefault(dev, 0L))
                        .totalConsumption(0)
                        .chargeCount(0)
                        .build());
                continue;
            }

            int segMax = curve.getFirst().getVoltage();
            int segMin = segMax;

            for (int i = 1; i < curve.size(); i++) {
                int v = curve.get(i).getVoltage();

                // 如果还在“爬坡”，刷新本段最大值
                if (v > segMax) {
                    segMax = v;
                }
                // 一旦出现 ≥50 mV 的回落，就截断
                else if (segMax - v >= 50) {
                    totalDrop += segMax - segMin; // 本段压降
                    chargeCnt++;                  // 记一次充电
                    // 开启新段
                    segMax = v;
                    segMin = v;
                }

                // 无论是否截断，都要刷新本段最小值
                segMin = Math.min(segMin, v);
            }

            // 最后一段别忘了累加
            totalDrop += segMax - segMin;

            result.put(dev, StatsRespVO.builder()
                    .taskCount(taskMap.getOrDefault(dev, 0L))
                    .navSegmentCount(navMap.getOrDefault(dev, 0L))
                    .totalConsumption(totalDrop)
                    .chargeCount(chargeCnt)
                    .build());
        }
        return result;
    }
}
